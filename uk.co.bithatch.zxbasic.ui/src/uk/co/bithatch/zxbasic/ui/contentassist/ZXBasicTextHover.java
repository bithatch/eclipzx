package uk.co.bithatch.zxbasic.ui.contentassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

public class ZXBasicTextHover implements ITextHover {

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        try {
            IDocument document = textViewer.getDocument();
            String text = document.get(hoverRegion.getOffset(), hoverRegion.getLength());
            // Lookup macro by name (e.g., EnableStuff)
            String macroExpansion = lookupMacro(text.trim());
            return macroExpansion != null ? "Macro: " + macroExpansion : null;
        } catch (BadLocationException e) {
            return null;
        }
    }

    private String lookupMacro(String trim) {
		return "EXPANDED!";
	}

	@Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        // Return the region around the hovered word
        return new Region(offset, 1); // Better: implement logic to find full word
    }
}
