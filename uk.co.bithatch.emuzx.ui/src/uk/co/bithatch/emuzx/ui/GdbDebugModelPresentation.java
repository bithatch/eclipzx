package uk.co.bithatch.emuzx.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Debug model presentation for the External Emulator / GDB RSP debugger.
 * Provides text labels and editor input for the Eclipse debug UI.
 */
public class GdbDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {

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
			/* Use the C editor for .c files, text editor otherwise */
			var name = file.getName().toLowerCase();
			if (name.endsWith(".c") || name.endsWith(".h")) {
				return "org.eclipse.cdt.ui.editor.CEditor";
			}
			if (name.endsWith(".asm") || name.endsWith(".s")) {
				return "org.eclipse.ui.DefaultTextEditor";
			}
		}
		return "org.eclipse.ui.DefaultTextEditor";
	}
}
