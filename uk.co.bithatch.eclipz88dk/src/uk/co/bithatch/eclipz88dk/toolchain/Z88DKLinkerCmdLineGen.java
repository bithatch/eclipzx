package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedCommandLineGenerator;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;

/**
 * Linker command line generator. CDT invokes this once with all {@code .o}
 * files collected from the mirrored build directory structure as inputs.
 * <p>
 * When a {@link Z88DKBuildContext} is set (launch build), the linker emits
 * {@code -create-app -subtype=<fmt>} to produce the final distributable
 * binary (e.g. {@code .nex}, {@code .tap}). Otherwise it produces a plain
 * {@code .bin}.
 * <p>
 * Linker-specific options ({@code -pragma-include:}, {@code -startup=})
 * are read from the linker tool's options and emitted here.
 */
public class Z88DKLinkerCmdLineGen extends ManagedCommandLineGenerator {

	private static final ILog LOG = ILog.of(Z88DKLinkerCmdLineGen.class);

	private static final String OPT_PRAGMA_INCLUDE = "uk.co.bithatch.eclipz88dk.linker.pragmainclude";
	private static final String OPT_STARTUP = "uk.co.bithatch.eclipz88dk.linker.startup";

	@Override
	public IManagedCommandLineInfo generateCommandLineInfo(
			ITool tool,
			String command,
			String[] flags,
			String outputFlag,
			String outputPrefix,
			String output,
			String[] inputResources,
			String commandLinePattern) {

		var project = Z88DKCmdLineGen.projectFromTool(tool);

		var merged = new ArrayList<String>();
		if (flags != null)
			merged.addAll(Arrays.asList(flags));

		/* Strip linker options from flags — we re-add them ourselves */
		stripLinkerOptions(tool, merged);

		var pax = Z88DKPreferencesAccess.get();
		var sdk = pax.getSDK(project).get();

		command = new File(new File(sdk.location(), "bin"), command).getAbsolutePath();
		final String cmd = command;

		LOG.info("Z88DK Linker: tool.getId()=" + tool.getId()
				+ ", output=" + output
				+ ", inputs=" + (inputResources != null ? Arrays.toString(inputResources) : "null")
				+ ", buildContext=" + Z88DKBuildContext.get());

		/* Architecture and clib */
		merged.add("+" + pax.getArchitecture(project).name().toLowerCase());
		merged.add("-clib=" + pax.getCLibrary(project));

		/* Add library paths/libs from referenced projects */
		Z88DKCmdLineGen.addSettingsFromReferences(project, merged);

		/* Linker-specific options */
		addStringOption(tool, OPT_PRAGMA_INCLUDE, "-pragma-include:", merged);
		addStringOption(tool, OPT_STARTUP, "-startup=", merged);

		/* Build context: launch build vs normal build */
		var buildCtx = Z88DKBuildContext.get();
		String ext;
		if (buildCtx.isPresent()) {
			merged.add("-create-app");
			merged.add("-subtype=" + buildCtx.get().name().toLowerCase());
			ext = buildCtx.get().extension().toLowerCase();
		} else {
			ext = "bin";
		}

		/* Adjust output extension to match the target format */
		String linkOutput = output.replaceAll("\\.[^.]+$", "." + ext);

		/* Build the command line manually so we can control quoting/globbing.
		 * CDT passes .o inputs from the mirrored directory structure. */
		var sb = new StringBuilder();
		sb.append(cmd);
		for (String f : merged) {
			sb.append(' ').append(f);
		}
		sb.append(' ').append(outputFlag).append(' ').append(linkOutput);

		/* Add all .o inputs */
		if (inputResources != null) {
			for (String inp : inputResources) {
				sb.append(' ').append(inp);
			}
		}

		var finalCmd = sb.toString();
		var flagStr = String.join(" ", merged);
		var inputStr = (inputResources != null) ? String.join(" ", inputResources) : "";

		return new IManagedCommandLineInfo() {
			@Override public String getCommandLine() { return finalCmd; }
			@Override public String getCommandLinePattern() { return commandLinePattern; }
			@Override public String getCommandName() { return cmd; }
			@Override public String getFlags() { return flagStr; }
			@Override public String getOutputFlag() { return outputFlag; }
			@Override public String getOutputPrefix() { return outputPrefix; }
			@Override public String getOutput() { return linkOutput; }
			@Override public String getInputs() { return inputStr; }
		};
	}

	private static final String[][] LINKER_OPTION_PREFIXES = {
		{ OPT_PRAGMA_INCLUDE, "-pragma-include:" },
		{ OPT_STARTUP,        "-startup=" },
	};

	private static void stripLinkerOptions(ITool tool, java.util.List<String> flags) {
		for (String[] entry : LINKER_OPTION_PREFIXES) {
			String optId = entry[0];
			String prefix = entry[1];
			String val = Z88DKCmdLineGen.getStringOptionValue(tool, optId);
			if (val != null && !val.isBlank()) {
				flags.remove(prefix + val.trim());
				flags.remove(val.trim());
			}
		}
	}

	private static void addStringOption(ITool tool, String optionId, String prefix, java.util.List<String> flags) {
		String val = Z88DKCmdLineGen.getStringOptionValue(tool, optionId);
		if (val != null && !val.isBlank()) {
			flags.add(prefix + val.trim());
		}
	}
}
