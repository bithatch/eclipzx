package uk.co.bithatch.emuzx.emulator.jspeccy;

import static uk.co.bithatch.emuzx.IEmulatorLaunchConfigurationAttributes.PROGRAM;
import static uk.co.bithatch.emuzx.IEmulatorLaunchConfigurationAttributes.PROJECT;

import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.bitzx.LaunchContext;
import uk.co.bithatch.emuzx.api.IInternallyLaunchable;
import uk.co.bithatch.emuzx.api.IPreparationTarget;
import uk.co.bithatch.emuzx.api.IWritablePreparationContext;
import uk.co.bithatch.emuzx.ui.AbstractPreparedLaunchConfigurationDelegate;
import uk.co.bithatch.jspeccy.views.EmulatorInstance;
import uk.co.bithatch.jspeccy.views.EmulatorView;

public class EmulatorLaunchConfiguration extends AbstractPreparedLaunchConfigurationDelegate<IInternallyLaunchable> {

	public EmulatorLaunchConfiguration() {
		super(PROJECT, PROGRAM, IInternallyLaunchable.class);
	}


	@Override
	protected void preparedLaunch(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor,
			Optional<IPreparationTarget> preparationTarget, String mode, IFile file,
			IWritablePreparationContext prepCtx, IInternallyLaunchable launchable, LaunchContext launchCtx)
			throws CoreException {
		var ofmt = launchable.getLaunchFormat(configuration, file, f -> Activator.JSPECCY_RUNNABLE_FORMATS.contains(f.extension()));
		var lang = LanguageSystem.languageSystem(file);
		var binaryFile = lang.prepareForInternalLaunch(ofmt, file, configuration, mode, launch, monitor);
		
		PlatformUI.getWorkbench().getDisplay().execute(() -> {
			// TODO make this better wrt threads

			try {
				var eview = openEmulatorView(configuration, PlatformUI.getWorkbench());
				launch.addDebugTarget(new EmulatorDebugTarget(launch, eview));
				eview.load(binaryFile.toFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
	}


	@Override
	protected Predicate<IOutputFormat> getSupportedFormatsFilter() {
		var sf = super.getSupportedFormatsFilter();
		return f -> sf.test(f) && Activator.JSPECCY_RUNNABLE_FORMATS.contains(f.extension());
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
					return ((EmulatorView) view).resetAndShowEmulator(configuration.getName());
				} catch (PartInitException e) {
					throw new IllegalStateException("Failed to open emulator view.");
				}
			}
		}
	}

}