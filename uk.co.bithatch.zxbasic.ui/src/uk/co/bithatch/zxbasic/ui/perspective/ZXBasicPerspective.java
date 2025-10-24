package uk.co.bithatch.zxbasic.ui.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

import uk.co.bithatch.jspeccy.views.EmulatorView;
import uk.co.bithatch.jspeccy.views.KeyboardView;
import uk.co.bithatch.zxbasic.ui.views.MemoryMapView;

public class ZXBasicPerspective implements IPerspectiveFactory {
    public static final String ID = "uk.co.bithatch.zxbasic.perspective";

	@Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        
        // Show editor
        layout.setEditorAreaVisible(true);
        layout.setFixed(false);

        // Vertical split inside the left folder (simulated via stacking)
        IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.20f, editorArea);
        left.addView(IPageLayout.ID_PROJECT_EXPLORER);
        IFolderLayout leftSplit = layout.createFolder("leftSplit", IPageLayout.BOTTOM, 0.65f, "left");
        leftSplit.addView(IPageLayout.ID_OUTLINE);
        
        // Bottom folder: tabbed layout for multiple views
        IFolderLayout bottomFolder = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.6f, editorArea);
        bottomFolder.addView(EmulatorView.ID);
        bottomFolder.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottomFolder.addView(IConsoleConstants.ID_CONSOLE_VIEW);

        // Horizontal split inside the bottom folder (simulated via stacking)
        IFolderLayout bottomSplit = layout.createFolder("bottomRightSplit", IPageLayout.RIGHT, 0.75f, "bottom");
        bottomSplit.addView(KeyboardView.ID);
        

        // Bottom folder: tabbed layout for multiple views
        IFolderLayout rightFolder = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea);
        rightFolder.addView("org.eclipse.debug.ui.DebugView");
        rightFolder.addView(MemoryMapView.ID);
    }
}
