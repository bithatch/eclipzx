package uk.co.bithatch.squashzx.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.squashzx.Activator;
import uk.co.bithatch.squashzx.ZX0;

public abstract class AbstractZX0Wizard<PG extends AbstractZX0WizardPage> extends Wizard implements IExportWizard {

	private PG page;
	private IFile file;
	private final String jobTitle;
	private final String taskName;
	private final String failureMessage;
	private final boolean decompress;

	protected AbstractZX0Wizard(String title, String jobTitle, String taskName, String failureMessage, boolean decompress) {
		setWindowTitle(title);
		this.jobTitle = jobTitle;
		this.taskName = taskName;
		this.failureMessage = failureMessage;
		this.decompress = decompress;
	}

	@Override
	public final void init(IWorkbench workbench, IStructuredSelection selection) {
		// Optional: pre-populate selection
		if (selection instanceof IStructuredSelection sel) {
			var element = sel.getFirstElement();
			if (element instanceof IFile file) {
				this.file = file;
			}
			else if (element instanceof IAdaptable adp) {
				this.file = adp.getAdapter(IFile.class);
			}
		}
	}

	@Override
	public final void addPages() {
		page = createPage();
		page.setSource(file);
		addPage(page);
	}

	protected abstract PG createPage();

	@Override
	public final boolean performFinish() {
		var destination = page.getTargetPath();
		var source = page.getSource();
		var overwrite = page.isOverwrite();
		var backwards = page.isBackwards();
		var quick = page.isQuick();
		var classic = page.isClassic();
		var removeSource = page.isRemoveSource();
		var skip = page.getSkip();
		var threads = page.getThreads();

		var job = new Job(jobTitle) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(taskName, decompress ? IProgressMonitor.UNKNOWN : 100);
				try {

					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					ZX0.zx0(monitor, 
							source.getLocation().toPath(), 
							skip, 
							overwrite, 
							backwards,
							destination.getLocation().toPath(), 
							decompress, 
							classic, 
							quick, 
							threads);

					destination.refreshLocal(IResource.DEPTH_ZERO, monitor);
					
					if(removeSource) {
						source.delete(false, monitor);
					}
					
					return Status.OK_STATUS;

				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, failureMessage, e);
				} finally {
					monitor.done();
				}
			}
		};

		job.setUser(true);
		job.schedule();
		return true;
	}

	public final void setSource(IFile file) {
		this.file = file;
	}

}
