package uk.co.bithatch.eclipzpp.preprocessor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.google.inject.Injector;

import uk.co.bithatch.eclipzpp.PPStandaloneSetup;
import uk.co.bithatch.eclipzpp.pp.PPProgram;
import uk.co.bithatch.eclipzpp.preprocessor.PP.Results;


/**
 * Standalone command-line preprocessor. Parses a source file using the
 * Xtext grammar and outputs a processed stream ready for the next stage (assembly, compilation,
 * editing, whatever).
 *
 * <p>Usage: {@code java PPCLI [options] <input.something> [output.something]}
 *
 * <p>Options:
 * <ul>
 *   <li>{@code --map} — write a {@code .pmap} file alongside the binary output</li>
 *   <li>{@code --map=<path>} — write the {@code .pmap} file to the given path</li>
 * </ul>
 *
 * <p>If no output path is given the output is written to stdout.
 */
public class PPCLI {

	public static void main(String[] args) throws IOException {
		boolean mapEnabled = false;
		Path mapPath = null;
		String inputArg = null;
		String outputArg = null;
		Map<String, String> defs = new LinkedHashMap<>();
		List<Path> includePaths = new ArrayList<>();

		for (String arg : args) {
			if ("--map".equals(arg)) {
				mapEnabled = true;
			} else if (arg.startsWith("--map=")) {
				mapEnabled = true;
				mapPath = Paths.get(arg.substring("--map=".length())).toAbsolutePath();
			} else if (arg.startsWith("-O")) {
				includePaths.add(Paths.get(arg.substring(2).trim()));
			} else if(arg.startsWith("-D")) {
				String name = arg.substring(2);
				int idx = arg.indexOf('=');
				String value = idx == -1 ? null : arg.substring(idx + 1);
				defs.put(name, value);
			} else if (inputArg == null) {
				inputArg = arg;
			} else if (outputArg == null) {
				outputArg = arg;
			}
		}

		if (inputArg == null) {
			System.err.println("Usage: PPCLI [--map[=<path>]] [-O<path> -O<path2> ..] [-D<name>[=<value]] <input> [output]");
			System.exit(1);
		}

		Path inputPath = Paths.get(inputArg).toAbsolutePath();
		Path outputPath = null;
		if (outputArg != null) {
			outputPath = Paths.get(outputArg).toAbsolutePath();
		}
		String outputName = outputPath == null ? "<stdout>" : outputPath.toString();

		System.err.println("Input:  " + inputPath);
		System.err.println("Output: " + outputName);

		// ── Xtext standalone setup ──
		Injector injector = new PPStandaloneSetup().createInjectorAndDoEMFRegistration();
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

		PPProgram program = (PPProgram) resource.getContents().get(0);

		// ── Build assembler ──
		PP.Builder builder = PP.builder();
		if (mapEnabled) {
			if (mapPath != null) {
				builder.withMap(mapPath);
			} else {
				builder.withMap();
			}
		}
		builder.withIncludePaths(includePaths);
		defs.forEach(builder::withDefine);
		
		PP pp = builder.build();

		// ── Process ──

		for (String warning : pp.getWarnings()) {
			System.err.println("WARNING: " + warning);
		}

		// ── Write output ──
		Results results;
		if(outputPath == null) {
			results = pp.process(inputPath.getFileName().toString(), program, System.out);

			System.err.println("Preproceessed to " + outputName);
		}
		else {
			try (OutputStream fos = Files.newOutputStream(outputPath)) {
				results = pp.process(inputPath.getFileName().toString(), program, fos);
			} catch (IOException e) {
				results = null;
				System.err.println("ERROR writing output: " + e.getMessage());
				System.exit(4);
			}

			long binaryLength = Files.size(outputPath);
			System.err.println("Preproceessed " + binaryLength + " bytes to " + outputName);

		}
		if (mapEnabled) {
			System.err.println("Map:      " + results.mapFile());
		}
	}
}