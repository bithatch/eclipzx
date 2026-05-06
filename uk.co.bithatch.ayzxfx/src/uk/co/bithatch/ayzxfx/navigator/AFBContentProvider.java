package uk.co.bithatch.ayzxfx.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITreeContentProvider;

import uk.co.bithatch.ayzxfx.ay.AFB;
import uk.co.bithatch.ayzxfx.ay.AFX;
import uk.co.bithatch.ayzxfx.editor.AFBEditor;

public class AFBContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IFile file && file.getFileExtension().equalsIgnoreCase("afb")) {
			AFBEditor editor = AFBEditor.findOpenAFBEditorFor(file);
			if (editor != null) {
				AFB afb = editor.afb();
				return getAFBNodes(file, afb);
			}
			else {
				AFB afb = AFB.load(file.getLocation().toPath());
				return getAFBNodes(file, afb);
			}
		}
		return new Object[0];
	}

	private Object[] getAFBNodes(IFile file, AFB afb) {
		var effects = afb.effects();
		var nodes = new Object[effects.size()];
		for (int i = 0; i < effects.size(); i++) {
			AFX effect = effects.get(i);
			nodes[i] = new AFXNode(file, afb, effect, i);
		}
		return nodes;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof IFile file && file.getFileExtension().equalsIgnoreCase("afb");
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof AFXNode node) {
			return node.getFile();
		} else {
			return null;
		}
	}
}
