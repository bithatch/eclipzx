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

    public AddFrameOperation(AFXTableModel model, AFXFrame frame) {
        super("Add Frame");
        this.frame = frame;
        this.model = model;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        model.add(frame);
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
