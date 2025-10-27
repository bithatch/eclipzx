package uk.co.bithatch.emuzx.emulator.zesarux;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_CONTENT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_FILE;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.CUSTOM_WORKING_DIRECTORY;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.EMULATOR_ARGS;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_TARGET;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.WORKING_DIRECTORY_LOCATION;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import uk.co.bithatch.bitzx.Strings;
import uk.co.bithatch.bitzx.WellKnownArchitecture;
import uk.co.bithatch.bitzx.WellKnownOutputFormat;
import uk.co.bithatch.emuzx.api.EmulatorDescriptor;
import uk.co.bithatch.emuzx.api.IEmulator;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class Zesarux implements IEmulator {
	

//Q: How can I run Chloe from commandline and no configuration file?
//A: Run a sentence like this:
//
//./zesarux --noconfigfile --machine chloe280 --mmc-file /zesarux-extras/extras/media/spectrum/chloe/chloehd.mmc --enable-mmc --enable-divmmc --nowelcomemessage
//
//The /zesarux-extras/extras/media/spectrum/chloe/chloehd.mmc is the location to where you have downloaded the chloehd.mmc from the zesarux-extras package.
//The --nowelcomemessage is only needed to disable the ZEsarUX splash logo which hides the Chloe "boot logo" (the wolf)
//
//You can also use ESXDOS handler (without mmc image), having all the required files on a folder, with a sentence like this:
//./zesarux --noconfigfile --machine chloe280 --esxdos-root-dir /path_to_chloe_files/ --enable-esxdos-handler --enable-divmmc --nowelcomemessage 
//
//
//Q: How can I run programs from the MMC c
//	Q: How can I use ZXMMC emulation?
//			A: Run alternate ROM alternaterom_plus3e_mmcen3eE.rom with machine type Spectrum +2A. Of course you have first to select MMC file, enable MMC emulation,  and enable ZXMMC on the menu
//
//
//			Q: I'm trying to run BBC Basic included in ZXMMC+ but it doesn't work
//			A: You need to select machine Spectrum 128k, and have Timex video disabled. After that, just do a reset and on the ZXMMC+ boot menu, press key V
//
//
//			Q: How can I use DIVMMC emulation?
//			A: ZEsarUX uses by default esxdos 0.8.5 firmware (file esxmmc085.rom). It also needs a filesystem for the common esxdos files.
//			So, first select MMC file, ZEsarUX has two mmc images for esxdos included in the extras package:
//			media/disk_images/divmmcesx085.mmc : for esxdos 0.8.5
//			media/disk_images/zxuno.mmc : for esxdos 0.8.6 beta (use with ZX-Uno)
//			Then enable MMC emulation,  and enable DIVMMC on the menu. Then reset the machine (and press space while booting if necessary)



	@Override
	public void configure(EmulatorDescriptor descriptor, ILaunchConfigurationWorkingCopy configuration, IFile programFile, File home, String mode) {
		var proj = programFile.getProject();
		var arch = ZXBasicPreferencesAccess.get().getArchitecture(proj);

		if(WellKnownArchitecture.ZXNEXT.equals(arch.wellKnown().orElse(null))) {
			/* TODO check this actually exists as its in a separate plugin */
			configuration.setAttribute(PREPARATION_TARGET, "uk.co.bithatch.eclipzx.ui.glue.automaticFATPreparationTarget");
			
			configuration.setAttribute(OUTPUT_FORMAT, arch.outputFormat(WellKnownOutputFormat.NEX).map(wk -> wk.name()).orElseThrow(() -> new IllegalStateException("Cannot map output format.")));
			configuration.setAttribute(EMULATOR_ARGS, Strings.separatedList("""
					--machine
					 TBBlue
					--configfile
					${emulator_config_file}
					--snap
					${zxbasic_launch_loc}
					""", System.lineSeparator()));
	        configuration.setAttribute(CONFIGURATION_CONTENT, """
					--enable-mmc
					--mmc-file=${fat_image}
	        		""");
		}
		else {
			configuration.setAttribute(OUTPUT_FORMAT, arch.outputFormat(WellKnownOutputFormat.SNA).map(wk -> wk.name()).orElseThrow(() -> new IllegalStateException("Cannot map output format.")));
			configuration.setAttribute(EMULATOR_ARGS, Strings.separatedList("""
					--machine
					P2A41
					--configfile
					${emulator_config_file}
					--snap
					${zxbasic_launch_loc}
					""", System.lineSeparator()));
	        configuration.setAttribute(CONFIGURATION_CONTENT, """
	        		""");
		}
        configuration.setAttribute(CONFIGURATION_FILE, "");
        configuration.setAttribute(CUSTOM_WORKING_DIRECTORY, true);
		configuration.setAttribute(WORKING_DIRECTORY_LOCATION, home.toString());
	}

}
