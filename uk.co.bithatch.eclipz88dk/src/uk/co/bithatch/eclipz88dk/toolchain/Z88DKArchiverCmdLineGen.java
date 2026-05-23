package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.File;
import java.util.Arrays;

import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedCommandLineGenerator;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;

/**
 * Archiver command line generator for producing z88dk {@code .lib} static
 * libraries. Uses {@code z80asm} with the {@code -x} flag to create a library
 * archive from compiled {@code .o} files.
 * <p>
 * The generated command is: {@code z80asm -x<output.lib> file1.o file2.o ...}
 */
public class Z88DKArchiverCmdLineGen extends ManagedCommandLineGenerator {

	private static final ILog LOG = ILog.of(Z88DKArchiverCmdLineGen.class);

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
		var pax = Z88DKPreferencesAccess.get();
		var sdk = pax.getSDK(project).get();

		/* z80asm is in the bin directory */
		String cmd = new File(new File(sdk.location(), "bin"), "z80asm").getAbsolutePath();

		/* Determine z80asm CPU from architecture (e.g. zxn -> z80n, zx -> z80) */
		String archName = pax.getArchitecture(project).name().toLowerCase();
		String cpu = archToCpu(archName);

		LOG.info("Z88DK Archiver: tool.getId()=" + tool.getId()
				+ ", output=" + output
				+ ", cpu=" + cpu
				+ ", inputs=" + (inputResources != null ? Arrays.toString(inputResources) : "null"));

		/* Build command: z80asm --cpu=<cpu> -x<output.lib> file1.o file2.o ... */
		var sb = new StringBuilder();
		sb.append(cmd);
		sb.append(" -m=").append(cpu);
		sb.append(" -x").append(output);

		if (inputResources != null) {
			for (String inp : inputResources) {
				sb.append(' ').append(inp);
			}
		}

		var finalCmd = sb.toString();
		var inputStr = (inputResources != null) ? String.join(" ", inputResources) : "";
		var flagStr = "-m=" + cpu + " -x" + output;

		return new IManagedCommandLineInfo() {
			@Override public String getCommandLine() { return finalCmd; }
			@Override public String getCommandLinePattern() { return commandLinePattern; }
			@Override public String getCommandName() { return cmd; }
			@Override public String getFlags() { return flagStr; }
			@Override public String getOutputFlag() { return ""; }
			@Override public String getOutputPrefix() { return ""; }
			@Override public String getOutput() { return ""; }
			@Override public String getInputs() { return inputStr; }
		};
	}

	/**
	 * Map z88dk architecture name (used with {@code +arch} in zcc) to the
	 * z80asm {@code --cpu} value.
	 */
	private static String archToCpu(String arch) {
		return switch (arch) {
			case "zxn" -> "z80n";
			default -> "z80";
		};
	}
}
