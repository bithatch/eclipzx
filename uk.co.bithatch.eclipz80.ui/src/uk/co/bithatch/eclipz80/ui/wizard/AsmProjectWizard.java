package uk.co.bithatch.eclipz80.ui.wizard;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import uk.co.bithatch.eclipz80.ui.AsmUiActivator;
import uk.co.bithatch.eclipz80.ui.builder.AsmNature;
import uk.co.bithatch.widgetzx.ZXPerspectivesUI;

public class AsmProjectWizard extends Wizard implements INewWizard {

	private AsmProjectWizardPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("New Z80 Assembly Project");
	}

	@Override
	public void addPages() {
		page = new AsmProjectWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		String projectName = page.getProjectName();
		if (projectName.isEmpty())
			return false;

		try {
			var project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (!project.exists()) {
				var locationURI = page.getLocationURI();
				if (locationURI != null) {
					IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
					desc.setLocationURI(locationURI);
					project.create(desc, null);
				} else {
					project.create(null);
				}
			}
			project.open(null);

			IProjectDescription desc = project.getDescription();
			desc.setNatureIds(new String[] {
				"org.eclipse.xtext.ui.shared.xtextNature",
				AsmNature.NATURE_ID
			});
			project.setDescription(desc, null);

			if (page.isCreateExampleProgram()) {
				var file = project.getFile("main.asm");
				if (!file.exists()) {
					file.create(new ByteArrayInputStream(
							("; Z80 Assembly - Hello World\n"
							+ "; Prints \"Hello\" to the screen using RST 10h (ZX Spectrum ROM)\n"
							+ "\n"
							+ "    org $8000\n"
							+ "\n"
							+ "start:\n"
							+ "    ld hl, message\n"
							+ ".loop:\n"
							+ "    ld a, (hl)\n"
							+ "    or a\n"
							+ "    ret z\n"
							+ "    rst $10\n"
							+ "    inc hl\n"
							+ "    jr loop\n"
							+ "\n"
							+ "message:\n"
							+ "    db \"Hello\", 0\n").getBytes()), true, null);
				}
			}
			

        	ZXPerspectivesUI.zxCodingPerspective(AsmUiActivator.PLUGIN_ID);

			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
