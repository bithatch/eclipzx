package uk.co.bithatch.drawzx.editor;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.zyxy.graphics.Palette;


public class SetPaletteOperation extends AbstractOperation {

    private final PaletteEditor editor;
	private final Palette palette;
	private Palette was;

    public SetPaletteOperation(PaletteEditor editor, Palette palette) {
        super("Update Palette Structure");
        this.editor = editor;
        this.palette = palette;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
    	was = editor.setPalette(palette);
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
    	editor.setPalette(was);
    	was = null;
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}
