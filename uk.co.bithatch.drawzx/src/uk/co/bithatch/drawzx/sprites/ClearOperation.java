package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.drawzx.widgets.SpriteEditorGrid;

public class ClearOperation extends AbstractOperation {

    private final SpriteEditorGrid editorGrid;
    private final Runnable dirty;
    private int[][] savedData;

    public ClearOperation(SpriteEditorGrid editorGrid, Runnable dirty) {
        super("Clear Sprite");
        this.editorGrid = editorGrid;
        this.dirty = dirty;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        savedData = snapshotData();
        editorGrid.clear();
        dirty.run();
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
        restoreData(savedData);
        dirty.run();
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }

    private int[][] snapshotData() {
        var cell = editorGrid.spriteCell();
        var size = cell.size();
        var snapshot = new int[size][size];
        for (var y = 0; y < size; y++) {
            for (var x = 0; x < size; x++) {
                snapshot[y][x] = cell.index(x, y);
            }
        }
        return snapshot;
    }

    private void restoreData(int[][] data) {
        var cell = editorGrid.spriteCell();
        var size = cell.size();
        for (var y = 0; y < size; y++) {
            for (var x = 0; x < size; x++) {
                cell.index(x, y, data[y][x]);
            }
        }
        editorGrid.notifyDataChanged();
    }
}
