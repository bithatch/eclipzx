package uk.co.bithatch.zxbasic.ui.wizard;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicArchitecture;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class BasicProjectWizard extends AbstractBasicProjectWizard<BasicProjectWizardPage> {

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("New ZX BASIC Project");
    }

	@Override
	protected BasicProjectWizardPage createMainPage() {
		return new BasicProjectWizardPage();
	}

	@Override
	protected boolean onProjectCreated(IProject project) throws CoreException {

		var pax = ZXBasicPreferencesAccess.get();
		page.getLibraries().forEach(l -> {
			pax.addDependency(project, l);
		});

		if(page.isOverridePreferences()) {
			var arch = page.getArchitecture();
			pax.setArchitecture(project, arch);
			pax.setSDK(project, page.getSDK());
			page.getLibraries().forEach(lib -> pax.addDependency(project, lib));
			
			/* Bit hacky, should probably have an option for this, or the library could find 
			 * at hit maybe
			 */
			if(arch.equals(BorielZXBasicArchitecture.ZXNEXT)) {
				pax.setOutputFormat(project, BorielZXBasicOutputFormat.NEX);
			}
		}

		var file = project.getFile("main.bas");
		if (!file.exists()) {
		    file.create(new ByteArrayInputStream("""
		    		10 REM ZX Basic knows the meaning of life
		    		20 PRINT 42
		    		30 STOP
		    		""".getBytes()), true, null);
		}
		return true;
	}

}
