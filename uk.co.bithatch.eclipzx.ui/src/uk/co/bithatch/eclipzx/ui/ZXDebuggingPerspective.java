package uk.co.bithatch.eclipzx.ui;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

import uk.co.bithatch.eclipzoxo.views.EmulatorView;
import uk.co.bithatch.eclipzoxo.views.KeyboardView;

public class ZXDebuggingPerspective implements IPerspectiveFactory {

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
        
        // Bottom folder: tabbed layout for multiple views
        var bottomFolder = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.65f, editorArea);
        bottomFolder.addView(EmulatorView.ID);
        bottomFolder.addView(IConsoleConstants.ID_CONSOLE_VIEW);
        bottomFolder.addView(IDebugUIConstants.ID_MEMORY_VIEW);

        // Horizontal split inside the bottom folder (simulated via stacking)
        var bottomSplit = layout.createFolder("bottomRightSplit", IPageLayout.RIGHT, 0.75f, "bottom");
        bottomSplit.addView(KeyboardView.ID);
        

        // Bottom folder: tabbed layout for multiple views
        var rightFolder = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea);
        rightFolder.addView(IDebugUIConstants.ID_DEBUG_VIEW);
        rightFolder.addView(IDebugUIConstants.ID_BREAKPOINT_VIEW);
        
        var rightSplit = layout.createFolder("rightSplit", IPageLayout.BOTTOM, 0.5f, "right");
        rightSplit.addView(IDebugUIConstants.ID_REGISTER_VIEW);
    }
}
