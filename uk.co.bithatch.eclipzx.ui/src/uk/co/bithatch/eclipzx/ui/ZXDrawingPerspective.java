package uk.co.bithatch.eclipzx.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import uk.co.bithatch.drawzx.views.ColourPickerView;
import uk.co.bithatch.drawzx.views.SpriteView;

public class ZXDrawingPerspective implements IPerspectiveFactory {

	@Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        
        // Show editor
        layout.setEditorAreaVisible(true);
        layout.setFixed(false);

        // Vertical split inside the left folder (simulated via stacking)
        IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.20f, editorArea);
        left.addView(IPageLayout.ID_PROJECT_EXPLORER);
        left.setProperty(editorArea, editorArea);
        
        // Bottom folder: tabbed layout for multiple views
        IFolderLayout rightFolder = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea);
        rightFolder.addView(ColourPickerView.ID);
        rightFolder.addView(SpriteView.ID);
        
    }
}