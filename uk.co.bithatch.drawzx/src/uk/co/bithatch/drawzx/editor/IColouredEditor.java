package uk.co.bithatch.drawzx.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;

import uk.co.bithatch.drawzx.editor.EditorFileProperties.PaletteSource;
import uk.co.bithatch.drawzx.views.IColourPicker;
import uk.co.bithatch.zyxy.graphics.Palette;

public interface IColouredEditor extends IWorkbenchPart {

	IFile getFile();

	default IFile getPaletteFile() {
		var source = EditorFileProperties.paletteSource(getFile());
		var val = source == PaletteSource.FILE
				? EditorFileProperties.getProperty(getFile(), EditorFileProperties.PALETTE_PROPERTY, "")
				: "";
		if (!val.equals("")) {
			var fileForLocation = getFile().getWorkspace().getRoot().findMember(IPath.fromPortableString(val));
			if (fileForLocation instanceof IFile ifile) {
				return ifile;
			}
		}
		return null;
	}

	void colorSelected(int data, boolean b);

	int maxPaletteHistorySize();
	
	int paletteCellSize();

	IEditorSite getEditorSite();
	
	boolean isPalettedChangeAllowed();
	
	boolean isPaletteResettable();

	Palette palette();
	

	void setDefaultPalette();

	void setDefaultTransPalette();
	
	boolean isPaletteOffsetUsed();
	
	boolean isPaletteHistoryUsed();

	void currentPaletteUpdate();

	int defaultPaletteIndex();
	
	void picker(IColourPicker picker);

	int defaultSecondaryPaletteIndex();

}
