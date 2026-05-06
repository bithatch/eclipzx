package uk.co.bithatch.ayzxfx.editor;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class MoveEffectOperation extends AbstractOperation {

	private final AFBEditor editor;
	private final int fromIndex;
	private final int toIndex;

	public MoveEffectOperation(AFBEditor editor, int fromIndex, int toIndex) {
		super("Move Effect");
		this.editor = editor;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
		editor.doMoveEffect(fromIndex, toIndex);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
		// Reverse the move
		int reverseFrom = toIndex > fromIndex ? toIndex - 1 : toIndex;
		int reverseTo = toIndex > fromIndex ? fromIndex : fromIndex + 1;
		editor.doMoveEffect(reverseFrom, reverseTo);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
		return execute(monitor, info);
	}
}
