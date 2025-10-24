package uk.co.bithatch.fatexplorer.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.fatexplorer.Activator;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.zyxy.lib.MemoryUnit;
import uk.co.bithatch.zyxy.mmc.SDCard;


public class NewDiskImageWizard extends Wizard implements INewWizard {

	private IStructuredSelection selection;
	private NewDiskImageWizardPage page;
	private DiskImageConfigurationWizardPage config;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	@Override
	public void addPages() {
		page = new NewDiskImageWizardPage("New Disk Image File", selection);
		page.setFileExtension("img");
		config = new DiskImageConfigurationWizardPage();
		addPage(page);
		addPage(config);
	}

	@Override
	public boolean performFinish() {
		var file = page.createNewFileHandle();
		if (file == null)
			return false;

		var addToExplorer = config.mbr();
		var mbr = config.mbr();
		var size = config.size();
		var fatType = config.fatType();
		var oemName = config.oemName().trim();
		var oemLabel = config.volumeLabel();
		var nativeFile = file.getLocation().toFile();
		
		var job = new Job("Creating Disk Image") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("", IProgressMonitor.UNKNOWN);
				try {

					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					
					new SDCard.Builder().
							withCreate().
							withFile(nativeFile).
							withMBR(mbr).
							withSize(MemoryUnit.MEBIBYTE, size).
							withFormatter(new SDCard.Formatter.Builder().
								withType(fatType).
								withOEMName(oemName).
								withLabel(oemLabel).
								build()).
							build();

					file.getParent().refreshLocal(IResource.DEPTH_ZERO, monitor);
					
					if(addToExplorer) {
						FATPreferencesAccess.addImagePath(file.getFullPath().toString().substring(1));
					}
					
					return Status.OK_STATUS;

				} catch (Exception e) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to create disk image.", e);
				} finally {
					monitor.done();
				}
			}
		};

		job.setUser(true);
		job.schedule();
		return true;
	}
}
