package uk.co.bithatch.squashzx;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.squashzx.wizards.DecompressFileWizard;


public class ZX0DecompressHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
    	var selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection sel) {
			var workbench = PlatformUI.getWorkbench();
			var shell = workbench.getActiveWorkbenchWindow().getShell();
			var wizard = new DecompressFileWizard();
			wizard.init(workbench, sel);
			var dialog = new WizardDialog(shell, wizard);
			dialog.open();
		}
		return null;
    }
}
