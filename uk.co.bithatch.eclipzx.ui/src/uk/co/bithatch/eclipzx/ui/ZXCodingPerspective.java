package uk.co.bithatch.eclipzx.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import uk.co.bithatch.eclipzoxo.views.EmulatorView;
import uk.co.bithatch.eclipzoxo.views.KeyboardView;

public class ZXCodingPerspective implements IPerspectiveFactory {

    private static final String MINIMAP_INSTANCE = IPageLayout.ID_MINIMAP_VIEW + ":initial";

	@Override
    public void createInitialLayout(IPageLayout layout) {
        var editorArea = layout.getEditorArea();
        
        // Show editor
        layout.setEditorAreaVisible(true);
        layout.setFixed(false);

        // Vertical split inside the left folder (simulated via stacking)
        var left = layout.createFolder("left", IPageLayout.LEFT, 0.20f, editorArea);
        left.addView(IPageLayout.ID_PROJECT_EXPLORER);
        var leftSplit = layout.createFolder("leftSplit", IPageLayout.BOTTOM, 0.65f, "left");
        leftSplit.addView(IPageLayout.ID_OUTLINE);
        
        // Keep all right-side views in one column to avoid conflicting right anchors.
        var right = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea);
        right.addPlaceholder(IPageLayout.ID_MINIMAP_VIEW);
        right.addView(MINIMAP_INSTANCE);
        right.addPlaceholder(IPageLayout.ID_MINIMAP_VIEW + ":*");

        // Bottom folder: tabbed layout for multiple views
        var bottomFolder = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.65f, editorArea);
        bottomFolder.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottomFolder.addView(IPageLayout.ID_TASK_LIST);
        bottomFolder.addView(EmulatorView.ID);

        // Split the right column vertically: minimap above, keyboard below.
        var rightBottom = layout.createFolder("rightBottom", IPageLayout.BOTTOM, 0.70f, "right");
        rightBottom.addView(KeyboardView.ID);

        layout.addShowViewShortcut(IPageLayout.ID_MINIMAP_VIEW);
        layout.getViewLayout(MINIMAP_INSTANCE).setMoveable(true);
    }
}
