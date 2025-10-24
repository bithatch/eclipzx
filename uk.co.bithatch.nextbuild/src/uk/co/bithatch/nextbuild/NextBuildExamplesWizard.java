package uk.co.bithatch.nextbuild;

import static uk.co.bithatch.nextbuild.AddRemoveNextBuildNatureHandler.toggleNature;
import static uk.co.bithatch.zxbasic.ui.library.ContributedLibraryRegistry.getLibrary;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import uk.co.bithatch.widgetzx.util.FileCopyUtil;
import uk.co.bithatch.zxbasic.ui.ZXBasicUiActivator;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicArchitecture;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.wizard.AbstractBasicProjectWizard;

public class NextBuildExamplesWizard extends AbstractBasicProjectWizard<NextBuildExamplesWizardPage> {

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle("New ZX BASIC NextBuild Examples Project");
	}

	@Override
	protected NextBuildExamplesWizardPage createMainPage() {
		return new NextBuildExamplesWizardPage();
	}

	@Override
	protected boolean onProjectCreated(IProject project) throws CoreException {

		try {
			var pax = ZXBasicPreferencesAccess.get();
			pax.setArchitecture(project, BorielZXBasicArchitecture.ZXNEXT);
			pax.setOutputFormat(project, BorielZXBasicOutputFormat.NEX);
			pax.addDependency(project, 
					getLibrary(project, "NextLib").
					orElseThrow(() -> new CoreException(Status.error("Could not find NextLib library."))));
			
			toggleNature(project);
			
	        var bundle = Platform.getBundle(Activator.PLUGIN_ID);
	        var entry = FileLocator.find(bundle, new Path("META-INF/next-build-sources"), null);
	        var file = new File(FileLocator.toFileURL(entry).toURI()).getCanonicalFile();
			
			getContainer().run(true, true, new WorkspaceModifyOperation() {
				@Override
				protected void execute(IProgressMonitor monitor) throws CoreException {
					try {
						FileCopyUtil.copyDirectoryToProject(file, project, monitor);
					} catch (IOException e) {
						throw new CoreException(new Status(IStatus.ERROR, ZXBasicUiActivator.PLUGIN_ID, "Failed to copy templates", e));
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException | IOException | URISyntaxException e) {
			// Handle exception or cancel
			return false;
		}

		return true;
	}

}
