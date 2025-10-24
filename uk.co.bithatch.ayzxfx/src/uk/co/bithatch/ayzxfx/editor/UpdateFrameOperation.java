package uk.co.bithatch.ayzxfx.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.SwingUtilities;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.ayzxfx.ay.AFXFrame;

public class UpdateFrameOperation extends AbstractOperation {

	private final AFXTableModel model;
	private final int[] rows;
	private final Function<AFXFrame, AFXFrame> update;
	private List<AFXFrame> was = new ArrayList<>();

    public UpdateFrameOperation(AFXTableModel model, int[] rows, Function<AFXFrame, AFXFrame> update) {
        super("Update Frame");
        this.model = model;
        this.rows = rows;
        this.update = update;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
    	for(var row : rows) {
    		was.add(model.afx().set(row, update.apply(model.afx().get(row))));
    	}
    	fireRowChange();
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
    	var wasIt = was.iterator();
    	for(var row : rows) {
        	model.afx().set(row, wasIt.next());
    	}
    	was.clear();
    	fireRowChange();
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }

	private void fireRowChange() {
		/* TODO try to collapse the interval into fewer ranges */
		if(SwingUtilities.isEventDispatchThread()) {
			for(var row : rows)
				model.fireTableRowsUpdated(row, row);
    	}
    	else {
	    	SwingUtilities.invokeLater(() -> {
				for(var row : rows)
					model.fireTableRowsUpdated(row, row);
	    	});
    	}
	}
}
