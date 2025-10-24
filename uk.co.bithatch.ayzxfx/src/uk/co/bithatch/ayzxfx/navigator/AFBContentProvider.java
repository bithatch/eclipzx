package uk.co.bithatch.ayzxfx.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITreeContentProvider;

import uk.co.bithatch.ayzxfx.editor.AFBEditor;

public class AFBContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IFile file && file.getFileExtension().equalsIgnoreCase("afb")) {
			AFBEditor editor = AFBEditor.findOpenAFBEditorFor(file);
			if (editor != null) {
				return editor.afb().effects().stream().map(afx -> new AFXNode(file, editor.afb(), afx)).toArray();
			}
		}
		return new Object[0];
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
