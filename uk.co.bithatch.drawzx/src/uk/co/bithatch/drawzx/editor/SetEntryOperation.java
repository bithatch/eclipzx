package uk.co.bithatch.drawzx.editor;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.zyxy.graphics.Palette.Entry;


public class SetEntryOperation extends AbstractOperation {

    private final PaletteEditor editor;
	private final Entry entry;
	private final int index;
	private Entry was;

    public SetEntryOperation(PaletteEditor editor, int index, Entry entry) {
        super("Set Palette Entry");
        this.editor = editor;
        this.entry = entry;
        this.index = index;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
    	editor.select(index); 
    	was = editor.setEntry(entry);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
    	editor.select(index);
    	editor.setEntry(was);
    	was = null;
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}
