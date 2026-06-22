package uk.co.bithatch.eclipz80.assembler;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.google.inject.Injector;

import uk.co.bithatch.eclipz80.AsmStandaloneSetup;
import uk.co.bithatch.eclipz80.asm.AsmProgram;

/**
 * Simple test that assembles sample .asm files with both our Z80Assembler and
 * z88dk's z80asm, then compares the binary output byte-by-byte.
 * <p>
 * Currently tests:
 * <ul>
 *   <li>{@code test_basic.asm} — core instructions, data directives (no labels)</li>
 *   <li>{@code test_labels.asm} — forward/backward label references, JR relative offsets</li>
 * </ul>
 * Run from Eclipse — no special setup needed.
 */
public class Z80AssemblerTest {

	private static final String Z80ASM = "/home/SOUTHPARK/tanktarta/Applications/z88dk-master/bin/z80asm";
	private static final Path PROJECT_ROOT = Paths.get(
			"/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.eclipz80");

	private static final String[] SAMPLES = {
		"samples/test_basic.asm",
		"samples/test_labels.asm",
		"samples/test_sections.asm",
		"samples/test_z80n.asm",
	};

	private static int passed = 0;
	private static int failed = 0;

	public static void main(String[] args) throws Exception {
		Injector injector = new AsmStandaloneSetup().createInjectorAndDoEMFRegistration();

		for (String sample : SAMPLES) {
			runTest(injector, sample);
			System.out.println();
		}

		System.out.println("════════════════════════════════════════");
		System.out.printf("Results: %d passed, %d failed (of %d)%n", passed, failed, SAMPLES.length);
		if (failed > 0) {
			System.exit(1);
		}
	}

	private static void runTest(Injector injector, String sampleRelPath) throws Exception {
		Path samplePath = resolveSample(sampleRelPath);
		if (samplePath == null) {
			System.err.println("ERROR: Cannot find " + sampleRelPath);
			failed++;
			return;
		}

		boolean isZ80N = sampleRelPath.contains("z80n");

		String sampleName = samplePath.getFileName().toString();
		System.out.println("=== Test: " + sampleName + (isZ80N ? " [Z80N]" : "") + " ===");
		System.out.println("Source: " + samplePath);
		System.out.println();

		// ── Step 1: Assemble with our assembler ──
		System.out.println("--- Our Z80Assembler ---");
		byte[] ourBinary = assembleWithOurs(injector, samplePath, isZ80N);
		System.out.println("Output: " + ourBinary.length + " bytes");
		System.out.println("Hex:    " + hexDump(ourBinary));
		System.out.println();

		// ── Step 2: Assemble with z88dk z80asm ──
		System.out.println("--- z88dk z80asm ---");
		byte[] z88dkBinary = assembleWithZ88dk(samplePath, isZ80N);
		if (z88dkBinary == null) {
			System.err.println("z88dk assembly failed — cannot compare");
			failed++;
			return;
		}
		System.out.println("Output: " + z88dkBinary.length + " bytes");
		System.out.println("Hex:    " + hexDump(z88dkBinary));
		System.out.println();

		// ── Step 3: Compare ──
		System.out.println("--- Comparison ---");
		if (Arrays.equals(ourBinary, z88dkBinary)) {
			System.out.println("PASS: Binary output is identical (" + ourBinary.length + " bytes)");
			passed++;
		} else {
			System.out.println("FAIL: Binary output differs!");
			System.out.println();
			int maxLen = Math.max(ourBinary.length, z88dkBinary.length);
			System.out.printf("%-6s  %-6s  %-6s%n", "Offset", "Ours", "z88dk");
			System.out.printf("%-6s  %-6s  %-6s%n", "------", "----", "-----");
			for (int i = 0; i < maxLen; i++) {
				String ours = i < ourBinary.length ? String.format("%02X", ourBinary[i] & 0xFF) : "--";
				String theirs = i < z88dkBinary.length ? String.format("%02X", z88dkBinary[i] & 0xFF) : "--";
				String marker = ours.equals(theirs) ? "" : " <-- DIFF";
				System.out.printf("  %04X  %-6s  %-6s%s%n", i, ours, theirs, marker);
			}
			failed++;
		}
	}

	private static Path resolveSample(String sampleRelPath) {
		// Try CWD first
		Path cwd = Paths.get("").toAbsolutePath();
		Path p = cwd.resolve(sampleRelPath);
		if (Files.exists(p)) return p;

		// Try the known project root
		p = PROJECT_ROOT.resolve(sampleRelPath);
		if (Files.exists(p)) return p;

		return null;
	}

	private static byte[] assembleWithOurs(Injector injector, Path asmFile, boolean z80n) throws IOException {
		ResourceSet resourceSet = injector.getInstance(ResourceSet.class);
		Resource resource = resourceSet.getResource(URI.createFileURI(asmFile.toString()), true);

		if (resource.getErrors() != null && !resource.getErrors().isEmpty()) {
			System.err.println("Parse errors:");
			for (Resource.Diagnostic diag : resource.getErrors()) {
				System.err.println("  Line " + diag.getLine() + ": " + diag.getMessage());
			}
		}

		AsmProgram program = (AsmProgram) resource.getContents().get(0);

		// Derive .bin output path (same directory as source)
		String baseName = asmFile.getFileName().toString();
		int dot = baseName.lastIndexOf('.');
		if (dot >= 0) baseName = baseName.substring(0, dot);
		Path binPath = asmFile.resolveSibling(baseName + ".bin");

		// Use builder pattern — .withMap() will auto-derive .zmap path from .bin
		Z80Assembler.Builder builder = Z80Assembler.builder()
				.withMap();
		if (z80n) {
			builder.withZ80N();
		}
		Z80Assembler assembler = builder.build();

		// Write .bin so we can derive the .zmap alongside it
		Results res;
		try (FileOutputStream fos = new FileOutputStream(binPath.toFile())) {
			res = assembler.assemble(asmFile.getFileName().toString(), program, fos);
		} 

		if (Files.exists(res.mapFile())) {
			System.out.println("Map file written: " + res.mapFile());
		}

		for (String warning : assembler.getWarnings()) {
			System.err.println("  WARNING: " + warning);
		}
		
		return Files.readAllBytes(binPath);
	}

	private static byte[] assembleWithZ88dk(Path asmFile, boolean z80n) throws Exception {
		// z80asm -b produces <basename>.bin in the same directory as the source.
		// Work in a temp directory to avoid polluting the source tree.
		Path tempDir = Files.createTempDirectory("z80asm_test_");
		String fileName = asmFile.getFileName().toString();
		Path tempAsm = tempDir.resolve(fileName);
		Files.copy(asmFile, tempAsm);

		// Copy sibling files (e.g. testbin.dat, inc.asm) so INCBIN/INCLUDE work
		Path sourceDir = asmFile.getParent();
		if (sourceDir != null) {
			try (var stream = Files.list(sourceDir)) {
				stream.filter(p -> !p.equals(asmFile) && Files.isRegularFile(p))
					.forEach(p -> {
						try {
							Files.copy(p, tempDir.resolve(p.getFileName().toString()));
						} catch (Exception e) { /* ignore */ }
					});
			}
		}

		ProcessBuilder pb;
		if (z80n) {
			pb = new ProcessBuilder(Z80ASM, "-mz80n", "-b", tempAsm.toString());
		} else {
			pb = new ProcessBuilder(Z80ASM, "-b", tempAsm.toString());
		}
		pb.redirectErrorStream(true);
		Process proc = pb.start();

		// Capture output
		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
		}

		int exitCode = proc.waitFor();
		if (output.length() > 0) {
			System.out.println("z80asm output: " + output.toString().trim());
		}

		if (exitCode != 0) {
			System.err.println("z80asm exited with code " + exitCode);
			deleteDir(tempDir);
			return null;
		}

		// z80asm -b produces <basename>.bin alongside the .asm
		String baseName = fileName;
		int dot = baseName.lastIndexOf('.');
		if (dot >= 0) baseName = baseName.substring(0, dot);
		Path binFile = tempDir.resolve(baseName + ".bin");
		if (!Files.exists(binFile)) {
			System.err.println("z80asm did not produce " + binFile);
			deleteDir(tempDir);
			return null;
		}

		byte[] binary = Files.readAllBytes(binFile);

		// Clean up temp files
		deleteDir(tempDir);

		return binary;
	}

	private static void deleteDir(Path dir) {
		try {
			Files.walk(dir)
				.sorted(java.util.Comparator.reverseOrder())
				.forEach(p -> {
					try { Files.deleteIfExists(p); } catch (Exception e) { /* ignore */ }
				});
		} catch (Exception e) {
			/* ignore cleanup errors */
		}
	}

	private static String hexDump(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			if (i > 0) sb.append(' ');
			sb.append(String.format("%02X", data[i] & 0xFF));
		}
		return sb.toString();
	}
}
