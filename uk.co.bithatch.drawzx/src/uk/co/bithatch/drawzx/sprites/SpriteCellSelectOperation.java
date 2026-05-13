package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class SpriteCellSelectOperation extends AbstractOperation {

    private final int oldIndex;
    private final int newIndex;
    private final java.util.function.IntConsumer selectFunction;

    public SpriteCellSelectOperation(int oldIndex, int newIndex, java.util.function.IntConsumer selectFunction) {
        super("Select Sprite Cell");
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
        this.selectFunction = selectFunction;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
        selectFunction.accept(newIndex);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
        selectFunction.accept(oldIndex);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}
