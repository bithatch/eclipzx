package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.drawzx.widgets.SpriteEditorGrid;

public class RotateOperation extends AbstractOperation {

    private final SpriteEditorGrid editorGrid;
    private final Runnable dirty;
    private final int degrees;

    public RotateOperation(SpriteEditorGrid editorGrid, int degrees, Runnable dirty) {
        super("Rotate Sprite " + (degrees > 0 ? "Clockwise" : "Anticlockwise"));
        this.editorGrid = editorGrid;
        this.degrees = degrees;
        this.dirty = dirty;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        editorGrid.rotate(degrees);
        dirty.run();
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
        editorGrid.rotate(-degrees);
        dirty.run();
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}
