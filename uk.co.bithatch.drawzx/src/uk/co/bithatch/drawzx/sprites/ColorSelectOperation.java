package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.drawzx.widgets.SpriteEditorGrid;

public class ColorSelectOperation extends AbstractOperation {

    private final SpriteEditorGrid editorGrid;
    private final boolean primary;
    private final int oldColor;
    private final int newColor;
    private final Runnable afterChange;

    public ColorSelectOperation(SpriteEditorGrid editorGrid, boolean primary, int oldColor, int newColor, Runnable afterChange) {
        super(primary ? "Select Primary Colour" : "Select Secondary Colour");
        this.editorGrid = editorGrid;
        this.primary = primary;
        this.oldColor = oldColor;
        this.newColor = newColor;
        this.afterChange = afterChange;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        applyColor(newColor);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
        applyColor(oldColor);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }

    private void applyColor(int color) {
        if (primary) {
            editorGrid.color(color);
        } else {
            editorGrid.secondaryColor(color);
        }
        afterChange.run();
    }
}
