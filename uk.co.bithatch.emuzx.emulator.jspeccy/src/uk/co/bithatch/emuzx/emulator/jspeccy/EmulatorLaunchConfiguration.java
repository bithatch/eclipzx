package uk.co.bithatch.emuzx.emulator.jspeccy;

import static uk.co.bithatch.emuzx.emulator.jspeccy.EmulatorLaunchConfigurationAttributes.PROGRAM;
import static uk.co.bithatch.emuzx.emulator.jspeccy.EmulatorLaunchConfigurationAttributes.PROJECT;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.bitzx.LanguageSystemPreferenceConstants;
import uk.co.bithatch.bitzx.WellKnownOutputFormat;
import uk.co.bithatch.emuzx.AbstractConfigurationDelegate;
import uk.co.bithatch.jspeccy.views.EmulatorInstance;
import uk.co.bithatch.jspeccy.views.EmulatorView;

public class EmulatorLaunchConfiguration extends AbstractConfigurationDelegate {

	public EmulatorLaunchConfiguration() {
		super(PROJECT, PROGRAM);
	}

	@Override
	public void launch(IFile file, ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		
		var ofmt = LanguageSystem.outputFormatOrDefault(file.getProject(), configuration.getAttribute(LanguageSystemPreferenceConstants.OUTPUT_FORMAT, WellKnownOutputFormat.SNA.name()));
		var binaryFile = LanguageSystem.languageSystem(file).prepareForLaunch(ofmt, file, configuration, mode, launch, monitor);
		
//		var ctx = new DefaultPreparationContext(configuration, file,
//				OutputFormat.parse(configuration.getAttribute(OUTPUT_FORMAT,
//				OutputFormat.SNA.name()), OutputFormat.SNA));
//
//		compileForLaunch(ctx, mode, ZXBasicBuilder.DEFAULT_REPORTER);

		PlatformUI.getWorkbench().getDisplay().execute(() -> {
			// TODO make this better wrt threads

			try {
				var eview = openEmulatorView(configuration, PlatformUI.getWorkbench());
				launch.addDebugTarget(new EmulatorDebugTarget(launch, eview));
				eview.load(binaryFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private EmulatorInstance openEmulatorView(ILaunchConfiguration configuration, IWorkbench bench) {

		var window = bench.getActiveWorkbenchWindow();
		if (window == null) {
			throw new IllegalStateException("No active workbench.");
		} else {
			var page = window.getActivePage();
			if (page == null) {
				throw new IllegalStateException("No active page.");
			} else {
				try {
					var view = page.showView(EmulatorView.ID);
					page.bringToTop(view);
					return ((EmulatorView) view).showEmulator(configuration.getName());
				} catch (PartInitException e) {
					throw new IllegalStateException("Failed to open emulator view.");
				}
			}
		}
	}

}