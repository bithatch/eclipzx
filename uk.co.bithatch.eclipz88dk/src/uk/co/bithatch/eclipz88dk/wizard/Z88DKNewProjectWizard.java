package uk.co.bithatch.eclipz88dk.wizard;

import java.io.ByteArrayInputStream;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.WellKnownArchitecture;
import uk.co.bithatch.bitzx.WellKnownOutputFormat;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKSDK;
import uk.co.bithatch.eclipz88dk.wizard.CdtProjectCreator.CdtType;

public class Z88DKNewProjectWizard extends AbstractZ88DKProjectWizard<Z88DKNewProjectWizardPage> {

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle("New Z88DK Project");
	}

	@Override
	protected Z88DKNewProjectWizardPage createMainPage() {
		return new Z88DKNewProjectWizardPage();
	}

	@Override
	protected CreateTask doProjectCreation(String projectName) {
		// Capture SWT widget values on the UI thread before the background operation
		boolean overridePreferences = page.isOverridePreferences();
		IArchitecture selectedArch = overridePreferences ? page.getArchitecture() : null;
		Z88DKSDK selectedSDK = overridePreferences ? page.getSDK() : null;
		String selectedCLibrary = overridePreferences ? page.getCLibrary() : null;
		CdtType cdtType = page.getProjectCdtType();
		boolean isLibrary = cdtType == CdtType.LIBRARY;
		boolean createExample = page.isCreateExampleProgram();

		return (mon, locationURI) -> {
			var project = CdtProjectCreator.createManagedCProject(cdtType, projectName, locationURI, mon);
			var pax = Z88DKPreferencesAccess.get();

			if (overridePreferences) {
				pax.setArchitecture(project, selectedArch);
				pax.setSDK(project, selectedSDK);
				pax.setCLibrary(project, selectedCLibrary);
			}
			
			var finalArch = pax.getArchitecture(project);
			var isZxNext = finalArch != null && WellKnownArchitecture.ZXNEXT.equals(finalArch.wellKnown().orElse(null));
			if (isZxNext) {
				finalArch.outputFormat(WellKnownOutputFormat.NEX).ifPresent(fmt -> pax.setOutputFormat(project, fmt));
			}
			else {
				finalArch.outputFormat(WellKnownOutputFormat.TAP).ifPresent(fmt -> pax.setOutputFormat(project, fmt));
			}

			// Now that preferences are set, re-apply the language settings provider
			// so it can resolve include paths correctly against the configured SDK/architecture.
			CdtProjectCreator.enableZ88DKFeatures(project);

			if (!isLibrary && createExample) {
				var file = project.getFile("main.c");
				if (!file.exists()) {
					if (isZxNext) {
						file.create(new ByteArrayInputStream("""
								#include <arch/zxn.h>   // ZX Spectrum Next architecture specfic functions
								#include <stdio.h>

								// Define some macros to make use of tty_z88dk control codes
								// Program must be compiled with a CRT that supports tty_z88dk e.g. -startup=1
								#define printInk(k)          printf("\\x10%c", '0'+(k))
								#define printPaper(k)        printf("\\x11%c", '0'+(k))
								#define printAt(row, col)    printf("\\x16%c%c", (col)+1, (row)+1)

								int main()
								{
								    printAt(10,10);                 // move cursor
								    puts("Hello World!");

								    while(1) {                      // loop for ever
								            zx_border(INK_RED);     // set border red
								            zx_border(INK_YELLOW);  // set border yellow
								    };

								    return 0;
								}
								  	""".getBytes()), true, null);
					} else {
						file.create(new ByteArrayInputStream("""
								#include <stdio.h>

								int main()
								{
								    printf("Hello World!\\n");
								    return 0;
								}
								""".getBytes()), true, null);
					}
				}
			} // end !isLibrary

			// Reindex now that preferences (SDK, architecture, cLibrary) are set,
			// so the language settings provider returns the correct include paths.
			var cproj = CoreModel.getDefault().create(project);
			if (cproj != null) {
				CCorePlugin.getIndexManager().reindex(cproj);
			}
		};
	}

}
