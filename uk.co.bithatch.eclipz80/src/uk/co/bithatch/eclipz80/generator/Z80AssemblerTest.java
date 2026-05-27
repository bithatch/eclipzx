package uk.co.bithatch.eclipz80.generator;

import java.io.BufferedReader;
import java.io.FileOutputStream;
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
 * Simple test that assembles test_basic.asm with both our Z80Assembler and
 * z88dk's z80asm, then compares the binary output byte-by-byte.
 * <p>
 * Run from Eclipse — no special setup needed.
 */
public class Z80AssemblerTest {

	private static final String Z80ASM = "/home/SOUTHPARK/tanktarta/Applications/z88dk-master/bin/z80asm";
	private static final String SAMPLE = "samples/test_basic.asm";

	public static void main(String[] args) throws Exception {
		// Resolve paths relative to the project root
		Path projectRoot = Paths.get("").toAbsolutePath();
		// If running from Eclipse, CWD may be the project dir already.
		// Try to find the sample relative to known locations.
		Path samplePath = projectRoot.resolve(SAMPLE);
		if (!Files.exists(samplePath)) {
			// Try from the eclipz80 project specifically
			samplePath = Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.eclipz80")
					.resolve(SAMPLE);
		}
		if (!Files.exists(samplePath)) {
			System.err.println("ERROR: Cannot find " + SAMPLE);
			System.err.println("Looked in: " + projectRoot.resolve(SAMPLE));
			System.exit(1);
		}

		System.out.println("=== Z80 Assembler Comparison Test ===");
		System.out.println("Source: " + samplePath);
		System.out.println();

		// ── Step 1: Assemble with our assembler ──
		System.out.println("--- Our Z80Assembler ---");
		byte[] ourBinary = assembleWithOurs(samplePath);
		System.out.println("Output: " + ourBinary.length + " bytes");
		System.out.println("Hex:    " + hexDump(ourBinary));
		System.out.println();

		// ── Step 2: Assemble with z88dk z80asm ──
		System.out.println("--- z88dk z80asm ---");
		byte[] z88dkBinary = assembleWithZ88dk(samplePath);
		if (z88dkBinary == null) {
			System.err.println("z88dk assembly failed — cannot compare");
			System.exit(2);
		}
		System.out.println("Output: " + z88dkBinary.length + " bytes");
		System.out.println("Hex:    " + hexDump(z88dkBinary));
		System.out.println();

		// ── Step 3: Compare ──
		System.out.println("--- Comparison ---");
		if (Arrays.equals(ourBinary, z88dkBinary)) {
			System.out.println("PASS: Binary output is identical (" + ourBinary.length + " bytes)");
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
			System.exit(3);
		}
	}

	private static byte[] assembleWithOurs(Path asmFile) {
		Injector injector = new AsmStandaloneSetup().createInjectorAndDoEMFRegistration();
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
		Z80Assembler assembler = Z80Assembler.builder()
				.withMap()
				.withSourceFileName(asmFile.getFileName().toString())
				.build();

		byte[] binary = assembler.assemble(program);

		// Write .bin so we can derive the .zmap alongside it
		try (FileOutputStream fos = new FileOutputStream(binPath.toFile())) {
			fos.write(binary);
		} catch (Exception e) {
			System.err.println("WARNING: Could not write .bin for map derivation: " + e.getMessage());
		}

		// Now assemble again with the output path to trigger .zmap writing
		assembler = Z80Assembler.builder()
				.withMap()
				.withSourceFileName(asmFile.getFileName().toString())
				.build();
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		assembler.assemble(program, baos, binPath);
		binary = baos.toByteArray();

		Path zmapPath = binPath.resolveSibling(baseName + ".zmap");
		if (Files.exists(zmapPath)) {
			System.out.println("Map file written: " + zmapPath);
		}

		for (String warning : assembler.getWarnings()) {
			System.err.println("  WARNING: " + warning);
		}
		return binary;
	}

	private static byte[] assembleWithZ88dk(Path asmFile) throws Exception {
		// z80asm -b produces <basename>.bin in the same directory as the source.
		// Work in a temp directory to avoid polluting the source tree.
		Path tempDir = Files.createTempDirectory("z80asm_test_");
		Path tempAsm = tempDir.resolve("test_basic.asm");
		Files.copy(asmFile, tempAsm);

		ProcessBuilder pb = new ProcessBuilder(Z80ASM, "-b", tempAsm.toString());
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
			// Clean up
			deleteDir(tempDir);
			return null;
		}

		// z80asm -b produces test_basic.bin alongside the .asm
		Path binFile = tempDir.resolve("test_basic.bin");
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
