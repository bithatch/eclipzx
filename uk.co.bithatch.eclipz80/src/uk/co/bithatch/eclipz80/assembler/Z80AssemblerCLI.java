package uk.co.bithatch.eclipz80.assembler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.google.inject.Injector;

import uk.co.bithatch.eclipz80.AsmStandaloneSetup;
import uk.co.bithatch.eclipz80.asm.AsmProgram;
import uk.co.bithatch.eclipz80.assembler.Z80Assembler.Results;

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
 *   <li>{@code --z80n} — enable Z80N (ZX Spectrum Next) extended instructions</li>
 * </ul>
 *
 * <p>If no output path is given the {@code .bin} is written alongside the source.
 */
public class Z80AssemblerCLI {

	public static void main(String[] args) throws IOException {
		boolean mapEnabled = false;
		Path mapPath = null;
		boolean farAddresses = false;
		boolean z80n = false;
		String inputArg = null;
		String outputArg = null;
		int forcedORG = -1;
		int defaultFill = -1;
		List<Path> includePaths = new ArrayList<>();
		List<Path> libPaths = new ArrayList<>();

		for (String arg : args) {
			if ("--map".equals(arg)) {
				mapEnabled = true;
			} else if (arg.startsWith("--map=")) {
				mapEnabled = true;
				mapPath = Paths.get(arg.substring("--map=".length())).toAbsolutePath();
			} else if ("--far".equals(arg)) {
				farAddresses = true;
			} else if ("--z80n".equals(arg)) {
				z80n = true;
			} else if (arg.startsWith("-O")) {
				includePaths.add(Paths.get(arg.substring(2).trim()));
			} else if (arg.startsWith("-L")) {
				libPaths.add(Paths.get(arg.substring(2).trim()));
			} else if (arg.startsWith("-r")) {
				forcedORG = parseNumber(arg.substring(2).trim());
			}  else if (arg.startsWith("-f")) {
				defaultFill = parseNumber(arg.substring(2).trim());
			} else if (inputArg == null) {
				inputArg = arg;
			} else if (outputArg == null) {
				outputArg = arg;
			}
		}

		if (inputArg == null) {
			System.err.println("Usage: Z80AssemblerCLI [--map[=<path>]] [-r<org>] [-f<byte]>] [--far] [--z80n] <input.asm> [output.bin]");
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
		Z80Assembler.Builder builder = Z80Assembler.builder();
		if (mapEnabled) {
			if (mapPath != null) {
				builder.withMap(mapPath);
			} else {
				builder.withMap();
			}
		}
		builder.withLibPaths(libPaths);
		if (farAddresses) {
			builder.withFarAddresses();
		}
		if (z80n) {
			builder.withZ80N();
		}
		if(forcedORG > 0) {
			builder.withORG(forcedORG);
		}
		if(defaultFill > 0) {
			builder.withDefaultFill(defaultFill);
		}
		
		Z80Assembler assembler = builder.build();

		// ── Assemble ──

		for (String warning : assembler.getWarnings()) {
			System.err.println("WARNING: " + warning);
		}

		// ── Write output ──
		Results results;
		try (OutputStream fos = Files.newOutputStream(outputPath)) {
			results = assembler.assemble(inputPath.getFileName().toString(), program, fos);
		} catch (IOException e) {
			results = null;
			System.err.println("ERROR writing output: " + e.getMessage());
			System.exit(4);
		}

		long binaryLength = Files.size(outputPath);
		System.out.println("Assembled " + binaryLength + " bytes to " + outputPath);
		if (mapEnabled) {
			System.out.println("Map:      " + results.mapFile());
		}
	}

	private static int parseNumber(String str) {
		if(str.startsWith("0x")) {
			return Integer.parseInt(str.substring(2), 16);
		}
		else if(str.startsWith("$")) {
			return Integer.parseInt(str.substring(0, str.length() - 1), 16);
		}
		else {
			return Integer.parseInt(str);
		}
	}
}