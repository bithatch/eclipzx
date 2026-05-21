package uk.co.bithatch.zximgconv;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Adds the {@link ZXImageConversionNature} to the selected project.
 */
public class EnableNatureHandler extends AbstractHandler {

	private static final ILog LOG = Platform.getLog(EnableNatureHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (!(selection instanceof IStructuredSelection ssel)) {
			return null;
		}
		for (Iterator<?> it = ssel.iterator(); it.hasNext();) {
			Object element = it.next();
			IProject project = null;
			if (element instanceof IProject p) {
				project = p;
			} else if (element instanceof IAdaptable a) {
				project = a.getAdapter(IProject.class);
			}
			if (project != null) {
				try {
					NatureUtil.addNature(project);
				} catch (CoreException e) {
					LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
							"Failed to enable ZX Image Conversion nature", e));
				}
			}
		}
		return null;
	}
}
