package uk.co.bithatch.jspeccy.wizards;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.jspeccy.views.EmulatorInstance;
import uk.co.bithatch.jspeccy.views.EmulatorView;

public class ExportScreenshotWizard extends Wizard implements IExportWizard {
	public final static String ID = "uk.co.bithatch.jspeccy.wizards.screenshot";

	private IContainer container;
	private EmulatorInstance emulator;
	private boolean wasRunning;
	private ExportScreenshotWizardPage page;

	public ExportScreenshotWizard() {
		setWindowTitle("Export Screenshot");
	}

	@Override
	public final void addPages() {
		page = new ExportScreenshotWizardPage(emulator, container);
		addPage(page);
	}

	@Override
	public final void init(IWorkbench workbench, IStructuredSelection selection) {
		try {
			emulator = EmulatorView.show();
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
		wasRunning = emulator.isEmulating();
		if (wasRunning) {
			emulator.pause();
		}
		if (selection instanceof IStructuredSelection sel) {
			var element = sel.getFirstElement();
			if (element instanceof IFile ifile) {
				container = ifile.getParent();
			}
			else if (element instanceof IContainer cntr) {
				container = cntr;
			} else if (element instanceof IAdaptable adp) {
				container = adp.getAdapter(IContainer.class);
			}
		}

	}

	@Override
	public void dispose() {
		try {
			super.dispose();
		} finally {
			if (wasRunning) {
				emulator.unpause();
			}

		}
	}

	@Override
	public boolean performFinish() {
		var target = page.getTargetPath();
		emulator.screenshot(target.getLocation().toFile());
		try {
			target.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
		}
		return true;
	}

}
