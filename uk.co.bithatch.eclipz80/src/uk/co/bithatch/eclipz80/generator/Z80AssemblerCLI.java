package uk.co.bithatch.eclipz80.generator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.google.inject.Injector;

import uk.co.bithatch.eclipz80.AsmStandaloneSetup;
import uk.co.bithatch.eclipz80.asm.AsmProgram;

/**
 * Standalone command-line Z80 assembler. Parses a {@code .asm} file using the
 * Xtext grammar and assembles it to a {@code .bin} file using
 * {@link Z80Assembler}.
 *
 * <p>Usage: {@code java Z80AssemblerCLI [options] <input.asm> [output.bin]}
 *
 * <p>Options:
 * <ul>
 *   <li>{@code --map} — write a {@code .zmap} file alongside the binary output</li>
 *   <li>{@code --map=<path>} — write the {@code .zmap} file to the given path</li>
 *   <li>{@code --far} — use 32-bit far addresses in map output (Z88DK)</li>
 * </ul>
 *
 * <p>If no output path is given the {@code .bin} is written alongside the source.
 */
public class Z80AssemblerCLI {

	public static void main(String[] args) {
		boolean mapEnabled = false;
		Path mapPath = null;
		boolean farAddresses = false;
		String inputArg = null;
		String outputArg = null;

		for (String arg : args) {
			if ("--map".equals(arg)) {
				mapEnabled = true;
			} else if (arg.startsWith("--map=")) {
				mapEnabled = true;
				mapPath = Paths.get(arg.substring("--map=".length())).toAbsolutePath();
			} else if ("--far".equals(arg)) {
				farAddresses = true;
			} else if (inputArg == null) {
				inputArg = arg;
			} else if (outputArg == null) {
				outputArg = arg;
			}
		}

		if (inputArg == null) {
			System.err.println("Usage: Z80AssemblerCLI [--map[=<path>]] [--far] <input.asm> [output.bin]");
			System.exit(1);
		}

		Path inputPath = Paths.get(inputArg).toAbsolutePath();
		Path outputPath;
		if (outputArg != null) {
			outputPath = Paths.get(outputArg).toAbsolutePath();
		} else {
			String name = inputPath.getFileName().toString();
			int dot = name.lastIndexOf('.');
			String baseName = dot >= 0 ? name.substring(0, dot) : name;
			outputPath = inputPath.resolveSibling(baseName + ".bin");
		}

		System.out.println("Input:  " + inputPath);
		System.out.println("Output: " + outputPath);

		// ── Xtext standalone setup ──
		Injector injector = new AsmStandaloneSetup().createInjectorAndDoEMFRegistration();
		ResourceSet resourceSet = injector.getInstance(ResourceSet.class);

		// ── Load and parse ──
		Resource resource = resourceSet.getResource(URI.createFileURI(inputPath.toString()), true);

		if (resource.getErrors() != null && !resource.getErrors().isEmpty()) {
			System.err.println("Parse errors:");
			for (Resource.Diagnostic diag : resource.getErrors()) {
				System.err.println("  Line " + diag.getLine() + ": " + diag.getMessage());
			}
			System.exit(2);
		}

		if (resource.getContents().isEmpty()) {
			System.err.println("ERROR: No content parsed from " + inputPath);
			System.exit(3);
		}

		AsmProgram program = (AsmProgram) resource.getContents().get(0);

		// ── Build assembler ──
		Z80Assembler.Builder builder = Z80Assembler.builder()
				.withSourceFileName(inputPath.getFileName().toString());
		if (mapEnabled) {
			if (mapPath != null) {
				builder.withMap(mapPath);
			} else {
				builder.withMap();
			}
		}
		if (farAddresses) {
			builder.withFarAddresses();
		}
		Z80Assembler assembler = builder.build();

		// ── Assemble ──
		byte[] binary;
		if (mapEnabled && mapPath == null) {
			// Use 3-arg form to auto-derive .zmap path from output path
			java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
			assembler.assemble(program, baos, outputPath);
			binary = baos.toByteArray();
		} else {
			binary = assembler.assemble(program);
		}

		for (String warning : assembler.getWarnings()) {
			System.err.println("WARNING: " + warning);
		}

		// ── Write output ──
		try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
			fos.write(binary);
		} catch (IOException e) {
			System.err.println("ERROR writing output: " + e.getMessage());
			System.exit(4);
		}

		System.out.println("Assembled " + binary.length + " bytes to " + outputPath);
		if (mapEnabled) {
			Path effectiveMapPath = mapPath;
			if (effectiveMapPath == null) {
				String binName = outputPath.getFileName().toString();
				int dot = binName.lastIndexOf('.');
				String baseName = dot >= 0 ? binName.substring(0, dot) : binName;
				effectiveMapPath = outputPath.resolveSibling(baseName + ".zmap");
			}
			System.out.println("Map:      " + effectiveMapPath);
		}

		// ── Hex dump ──
		System.out.println("\nHex dump:");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < binary.length; i++) {
			sb.append(String.format("%02X ", binary[i] & 0xFF));
			if ((i + 1) % 16 == 0) {
				System.out.println("  " + sb.toString().trim());
				sb.setLength(0);
			}
		}
		if (sb.length() > 0) {
			System.out.println("  " + sb.toString().trim());
		}
	}
}