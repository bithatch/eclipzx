package uk.co.bithatch.ayzxfx.editor;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.ayzxfx.ay.AFX;

public class UpdateEffectNameOperation extends AbstractOperation {

    private final AFX effect;
    private final String newName;
	private final AFBEditor editor;
	private String wasName;

    public UpdateEffectNameOperation(AFBEditor editor, AFX effect, String newName) {
        super("Update Effect Name");
        this.effect = effect;
        this.editor = editor;
        this.newName = newName;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
    	wasName = editor.setName(effect, newName);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
    	editor.setName(effect, wasName);
    	wasName= null;
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}
