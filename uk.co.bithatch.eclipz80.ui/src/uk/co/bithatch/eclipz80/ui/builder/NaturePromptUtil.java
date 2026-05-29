package uk.co.bithatch.eclipz80.ui.builder;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

public class NaturePromptUtil {

	private static boolean prompting = false;

	public static void maybePromptToEnableNature(IProject project) {
		try {
			if (!project.hasNature(AsmNature.NATURE_ID)) {
				for (IResource member : project.members()) {
					if (member instanceof IFile && hasAsmExtension(member.getName())) {
						promptToEnable(project);
						break;
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static boolean hasAsmExtension(String name) {
		if (name == null) return false;
		String lower = name.toLowerCase();
		for (String ext : AsmBuilder.EXTENSIONS) {
			if (lower.endsWith("." + ext)) {
				return true;
			}
		}
		return false;
	}

	private static void promptToEnable(IProject project) {
		Display.getDefault().asyncExec(() -> {
			if (prompting)
				return;
			prompting = true;
			try {
				boolean confirmed = MessageDialog.openQuestion(
					Display.getDefault().getActiveShell(),
					"Enable Assembly Builder",
					"This project contains .asm files but does not have the Assembly nature.\nWould you like to enable it?"
				);
				if (confirmed) {
					try {
						IProjectDescription desc = project.getDescription();
						String[] existingNatures = desc.getNatureIds();
						String[] updatedNatures = Arrays.copyOf(existingNatures, existingNatures.length + 1);
						updatedNatures[existingNatures.length] = AsmNature.NATURE_ID;
						desc.setNatureIds(updatedNatures);
						project.setDescription(desc, new NullProgressMonitor());
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			} finally {
				prompting = false;
			}
		});
	}
}
