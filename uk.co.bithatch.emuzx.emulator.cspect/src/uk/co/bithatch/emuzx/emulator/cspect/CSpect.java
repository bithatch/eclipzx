package uk.co.bithatch.emuzx.emulator.cspect;

import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.DEBUGGER_EMULATOR_ARGS;
import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.PORT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_CONTENT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_FILE;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CUSTOM_WORKING_DIRECTORY;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_ARGS;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_TARGET;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.WORKING_DIRECTORY_LOCATION;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import uk.co.bithatch.bitzx.Strings;
import uk.co.bithatch.bitzx.WellKnownArchitecture;
import uk.co.bithatch.bitzx.WellKnownOutputFormat;
import uk.co.bithatch.emuzx.ExternallyLaunchableRegistry;
import uk.co.bithatch.emuzx.api.EmulatorDescriptor;
import uk.co.bithatch.emuzx.api.IEmulator;

public class CSpect implements IEmulator {

	@Override
	public void configure(EmulatorDescriptor descriptor, ILaunchConfigurationWorkingCopy configuration, IFile programFile, File home, String mode) throws CoreException {
		var proj = programFile.getProject();
		var arch = ExternallyLaunchableRegistry.externallyLaunchableFor(programFile).getArchitecture(proj);
		
		if(WellKnownArchitecture.ZXNEXT.equals(arch.wellKnown().orElse(null))) {

			/* TODO check this actually exists as its in a separate plugin */
			configuration.setAttribute(PREPARATION_TARGET, "uk.co.bithatch.nextzxos.nextzxosFATPreparationTarget");
			
			configuration.setAttribute(OUTPUT_FORMAT, arch.outputFormat(WellKnownOutputFormat.NEX).map(wk -> wk.name()).orElseThrow(() -> new IllegalStateException("Cannot map output format.")));
			configuration.setAttribute(EMULATOR_ARGS, Strings.separatedList("""
					-zxnext
					-nextrom
					-mmc=${fat_image}
					-mouse
					""", System.lineSeparator()));
		}
		else {
			configuration.setAttribute(OUTPUT_FORMAT, arch.outputFormat(WellKnownOutputFormat.BIN).map(wk -> wk.name()).orElseThrow(() -> new IllegalStateException("Cannot map output format.")));
			configuration.setAttribute(EMULATOR_ARGS, Strings.separatedList("""
					""", System.lineSeparator()));
		}
        configuration.setAttribute(CUSTOM_WORKING_DIRECTORY, true);
		configuration.setAttribute(WORKING_DIRECTORY_LOCATION, home.toString());
        configuration.setAttribute(CONFIGURATION_FILE, "");
        configuration.setAttribute(CONFIGURATION_CONTENT, "");

        configuration.setAttribute(PORT, 11000);
        configuration.setAttribute(DEBUGGER_EMULATOR_ARGS, Arrays.asList("-remote"));
		
		
	}
}
