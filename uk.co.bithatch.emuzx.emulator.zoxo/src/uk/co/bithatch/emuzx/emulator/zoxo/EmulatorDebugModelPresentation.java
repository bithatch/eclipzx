package uk.co.bithatch.emuzx.emulator.zoxo;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import uk.co.bithatch.bitzx.LanguageSystem;

/**
 * Debug model presentation for the Zoxo internal emulator debugger.
 * Registered via {@code org.eclipse.debug.ui.debugModelPresentations}
 * with model ID {@link Activator#PLUGIN_ID}.
 */
public class EmulatorDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {

	@Override
	public void setAttribute(String attribute, Object value) {
		/* No configurable attributes */
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		return null; /* Use default labels */
	}

	@Override
	public void computeDetail(IValue value, IValueDetailListener listener) {
		try {
			listener.detailComputed(value, value.getValueString());
		} catch (Exception e) {
			listener.detailComputed(value, "?");
		}
	}

	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof IFile file) {
			return new FileEditorInput(file);
		}
		return null;
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		if (element instanceof IFile file) {
			var ed = LanguageSystem.languageSystem(file.getProject()).getEditorId(file);
			if(ed != null) {
				return ed;
			}
		}
		return "org.eclipse.ui.DefaultTextEditor";
	}
}
