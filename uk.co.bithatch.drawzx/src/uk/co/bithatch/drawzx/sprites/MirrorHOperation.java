package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.drawzx.widgets.SpriteEditorGrid;

public class MirrorHOperation extends AbstractOperation {

    private final SpriteEditorGrid editorGrid;
    private final Runnable dirty;


    public MirrorHOperation(SpriteEditorGrid editorGrid, Runnable dirty) {
        super("Mirro Sprite Horizontally");
        this.editorGrid = editorGrid;
        this.dirty = dirty;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
    	editorGrid.mirrorH();
    	dirty.run();
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}