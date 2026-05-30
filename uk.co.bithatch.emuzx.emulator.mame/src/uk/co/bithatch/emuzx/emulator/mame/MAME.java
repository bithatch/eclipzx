package uk.co.bithatch.emuzx.emulator.mame;

import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.DEBUGGER;
import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.DEBUGGER_EMULATOR_ARGS;
import static uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes.PORT;
import static uk.co.bithatch.emuzx.IEmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_CONTENT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_FILE;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CUSTOM_WORKING_DIRECTORY;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_ARGS;
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
import uk.co.bithatch.emuzx.LaunchableRegistry;
import uk.co.bithatch.emuzx.api.EmulatorDescriptor;
import uk.co.bithatch.emuzx.api.IEmulator;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;

/**
 * https://wiki.specnext.dev/MAME:Installing
 */
public class MAME implements IEmulator {

	@Override
	public void configure(EmulatorDescriptor descriptor, ILaunchConfigurationWorkingCopy configuration, IFile programFile, File home, String mode) throws CoreException {
		var proj = programFile.getProject();
		var extlnchr = LaunchableRegistry.launchableFor(IExternallyLaunchable.class, programFile);
		var arch = extlnchr.getArchitecture(proj);
		var wellKnown = arch.wellKnown().orElse(null);
		
		if(WellKnownArchitecture.ZXNEXT.equals(wellKnown)) {

			/* TODO check this actually exists as its in a separate plugin */
			configuration.setAttribute(PREPARATION_TARGET, "uk.co.bithatch.nextzxos.nextzxosFATPreparationTarget");
			configuration.setAttribute(OUTPUT_FORMAT, arch.outputFormat(WellKnownOutputFormat.NEX).map(wk -> wk.name()).orElseThrow(() -> new IllegalStateException("Cannot map output format.")));
			configuration.setAttribute(EMULATOR_ARGS, Strings.separatedList("""
					-inipath
					${emulator_config_file}
					-ui_active
					-nounevenstretch
					-aspect
					2:1
					-video
					bgfx
					-bgfx_screen_chains
					unfiltered
					-window
					-skip_gameinfo
					-mouse_device
					none
					-confirm_quit
					tbblue
					-hard1
					${fat_image}
					""", System.lineSeparator()));
		}
		else {
			configuration.setAttribute(PREPARATION_TARGET, "");
			configuration.setAttribute(OUTPUT_FORMAT, arch.outputFormat(WellKnownOutputFormat.TAP).map(wk -> wk.name()).orElseThrow(() -> new IllegalStateException("Cannot map output format.")));
			configuration.setAttribute(EMULATOR_ARGS, Strings.separatedList("""
					-inipath
					${emulator_config_file}
					-ui_active
					-nounevenstretch
					-aspect
					2:1
					-video
					bgfx
					-bgfx_screen_chains
					unfiltered
					-window
					-skip_gameinfo
					-mouse_device
					none
					-confirm_quit
					spec128
					-cass
					${ee_launch_loc}
					""", System.lineSeparator()));
		}
        configuration.setAttribute(CUSTOM_WORKING_DIRECTORY, true);
		configuration.setAttribute(WORKING_DIRECTORY_LOCATION, home.toString());
        configuration.setAttribute(CONFIGURATION_FILE, "");
        configuration.setAttribute(CONFIGURATION_CONTENT, "");

        configuration.setAttribute(PORT, 23946);
        configuration.setAttribute(DEBUGGER_EMULATOR_ARGS, Arrays.asList("-debug", "-debugger", "gdbstub", "-debugger_port", "${ee_debug_port}"));
        configuration.setAttribute(DEBUGGER, "uk.co.bithatch.emuzx.debug.gdb");
		
		
	}
}
