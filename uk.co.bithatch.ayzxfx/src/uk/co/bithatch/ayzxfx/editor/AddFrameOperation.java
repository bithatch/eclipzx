package uk.co.bithatch.ayzxfx.editor;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.ayzxfx.ay.AFXFrame;

public class AddFrameOperation extends AbstractOperation {

    private final AFXFrame frame;
	private final AFXTableModel model;
	private final int index;

    public AddFrameOperation(AFXTableModel model, AFXFrame frame) {
        this(model, -1, frame);
    }

    public AddFrameOperation(AFXTableModel model, int index, AFXFrame frame) {
        super("Add Frame");
        this.frame = frame;
        this.model = model;
        this.index = index;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        if (index >= 0 && index < model.getRowCount()) {
            model.add(index, frame);
        } else {
            model.add(frame);
        }
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
    	model.remove(frame);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}