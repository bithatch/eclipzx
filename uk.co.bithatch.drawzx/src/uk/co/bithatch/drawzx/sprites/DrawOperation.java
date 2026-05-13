package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.drawzx.widgets.SpriteEditorGrid;

public class DrawOperation extends AbstractOperation {

    private final SpriteEditorGrid editorGrid;
    private final int[][] beforeData;
    private int[][] afterData;

    public DrawOperation(SpriteEditorGrid editorGrid, int[][] beforeData) {
        super("Draw");
        this.editorGrid = editorGrid;
        this.beforeData = beforeData;
    }

    public void captureAfter() {
        this.afterData = snapshotData();
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        // Already executed by the user's drawing action
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
        restoreData(beforeData);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        restoreData(afterData);
        return Status.OK_STATUS;
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
