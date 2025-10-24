package uk.co.bithatch.zxbasic.ui.builder;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import uk.co.bithatch.bitzx.FileNames;

public class NaturePromptUtil {

	private static boolean prompting = false;

    public static void maybePromptToEnableNature(IProject project) {
        try {
            if (!project.hasNature(ZXBasicNature.NATURE_ID)) {
                for (IResource member : project.members()) {
                    if (member instanceof IFile && FileNames.hasExtensions(member.getName(), ZXBasicBuilder.EXTENSIONS)) {
                        promptToEnable(project);
                        break;
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    private static void promptToEnable(IProject project) {
        Display.getDefault().asyncExec(() -> {
        	if(prompting)
        		return;
        	prompting = true;
        	try {
	            boolean confirmed = MessageDialog.openQuestion(
	                Display.getDefault().getActiveShell(),
	                "Enable ZX Basic Builder",
	                "This project contains .bas files but does not have the ZX Basic nature.\nWould you like to enable it?"
	            );
	            if (confirmed) {
	                try {
	                    IProjectDescription desc = project.getDescription();
	                    String[] existingNatures = desc.getNatureIds();
	                    String[] updatedNatures = Arrays.copyOf(existingNatures, existingNatures.length + 1);
	                    updatedNatures[existingNatures.length] = ZXBasicNature.NATURE_ID;
	                    desc.setNatureIds(updatedNatures);
	                    project.setDescription(desc, new NullProgressMonitor());
	                } catch (CoreException e) {
	                    e.printStackTrace(); 
	                    IStatus status = e.getStatus();
	                    System.err.println("Status: " + status.getMessage());
	                }
	            }
        	}
        	finally {
        		prompting = false;
        	}
        });
    }
}
