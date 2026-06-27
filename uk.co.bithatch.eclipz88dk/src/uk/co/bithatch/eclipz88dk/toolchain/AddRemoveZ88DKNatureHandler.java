package uk.co.bithatch.eclipz88dk.toolchain;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.eclipz88dk.wizard.CdtProjectCreator;

/**
 * Command handler that toggles the Z88DK nature on the selected project(s).
 * Used by the "Configure → Enable/Disable Z88DK Nature" context menu.
 */
public class AddRemoveZ88DKNatureHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection structured) {
			for (Iterator<?> it = structured.iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject p) {
					project = p;
				} else if (element instanceof IAdaptable adaptable) {
					project = adaptable.getAdapter(IProject.class);
				}
				if (project != null) {
					try {
						toggleNature(project);
					} catch (CoreException e) {
						throw new ExecutionException("Failed to toggle Z88DK nature", e);
					}
				}
			}
		}
		return null;
	}

	private void toggleNature(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();

		for (int i = 0; i < natures.length; ++i) {
			if (Z88DKNature.NATURE_ID.equals(natures[i])) {
				// Remove the nature
				String[] newNatures = new String[natures.length - 1];
				System.arraycopy(natures, 0, newNatures, 0, i);
				System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				return;
			}
		}

		// Enable full Z88DK support, including required CDT natures.
		CdtProjectCreator.enableZ88DKFeatures(project);
	}
}
