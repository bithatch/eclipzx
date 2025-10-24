package uk.co.bithatch.jspeccy.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.jspeccy.views.EmulatorView;
import uk.co.bithatch.jspeccy.wizards.ExportScreenshotWizard;

public class ScreenshotHandler extends AbstractEmulatorHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, EmulatorView emulator) {
		var bench = PlatformUI.getWorkbench();
		var wizDesc = bench.getExportWizardRegistry().findWizard(ExportScreenshotWizard.ID);
		if (wizDesc != null) {
			try {
				var wiz = wizDesc.createWizard();
				var wizDiag = new WizardDialog(bench.getDisplay().getActiveShell(), wiz);
				wizDiag.setTitle(wiz.getWindowTitle());
				wiz.init(bench, null);
				wizDiag.open();
			} catch (CoreException ce) {
				throw new IllegalStateException(ce);
			}
		}
		return null;
	}

}
