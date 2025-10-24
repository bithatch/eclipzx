package uk.co.bithatch.ayzxfx.editor;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.ayzxfx.ay.AFX;

public class RemoveEffectsOperation extends AbstractOperation {

    private final AFX[] effects;
    private final int[] indexes;
	private final AFBEditor editor;

    public RemoveEffectsOperation(AFBEditor editor, AFX... effects) {
        super("Remove Effects");
        this.effects = effects;
        this.editor = editor;
        
        indexes = new int[effects.length];
        for(var i = 0 ; i < effects.length; i++) {
        	indexes[i] = editor.afb().indexOf(effects[i]);
        }
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        editor.doRemoveEffects(effects);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
    	for(var i = indexes.length - 1 ; i >= 0 ; i--) {
    		editor.doAddEffect(indexes[i], effects[i]);
    	}
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}
