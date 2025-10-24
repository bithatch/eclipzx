package uk.co.bithatch.ayzxfx.navigator;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import uk.co.bithatch.ayzxfx.Activator;
import uk.co.bithatch.ayzxfx.ay.NamedAFX;
import uk.co.bithatch.ayzxfx.editor.AFBEditor;

public class AFXLabelProvider extends LabelProvider implements IStyledLabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof AFXNode afx) {
			return getNameText(afx);
		}
		return super.getText(element);
	}

	private String getNameText(AFXNode afx) {
		var fx = afx.getAfx();
		if(fx instanceof NamedAFX nafx && nafx.name() != null && !nafx.name().equals("")) {
			return nafx.name() + " (" + afx.getAfx().frames().size() + " frames)";
		}
		else {
			var idx = afx.getAfb().indexOf(fx);
			return "Effect " + idx;
		}
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof AFXNode) {
			return Activator.getDefault().getImageRegistry().get(Activator.EFFECT);
		}
		return super.getImage(element);
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof AFXNode afx) {
			var editor = AFBEditor.findOpenAFBEditorFor(afx.getFile());
	        var str = new StyledString(getNameText(afx));
			if(!editor.afx().equals(afx.getAfx())) {
				str.setStyle(0, str.length(), StyledString.DECORATIONS_STYLER);
			}
			return str;
		}
	    return new StyledString(element.toString());
	}
}
