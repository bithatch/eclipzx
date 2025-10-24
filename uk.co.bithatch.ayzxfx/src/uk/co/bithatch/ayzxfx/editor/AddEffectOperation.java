package uk.co.bithatch.ayzxfx.editor;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.ayzxfx.ay.AFX;

public class AddEffectOperation extends AbstractOperation {

    private final AFX effect;
	private final AFBEditor editor;

    public AddEffectOperation(AFBEditor editor, AFX effect) {
        super("Add Effect");
        this.effect = effect;
        this.editor = editor;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        editor.doAddEffect(effect);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
        editor.doRemoveEffects(effect);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}
