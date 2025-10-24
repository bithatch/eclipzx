package uk.co.bithatch.ayzxfx.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import uk.co.bithatch.ayzxfx.ay.AFXFrame;

public class RemoveFrameOperation extends AbstractOperation {

    private final int[] frames;
	private final AFXTableModel model;
	private Map<Integer, AFXFrame> removed = new LinkedHashMap<Integer, AFXFrame>();

    public RemoveFrameOperation(AFXTableModel model, int... frames) {
        super("Remove Frames");
        this.frames = frames;
        this.model = model;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) {
    	SwingUtilities.invokeLater(() -> {
    		for(var i = frames.length - 1 ; i >= 0 ; i--) {
    			var frm = model.remove(frames[i]);
    			removed.put(i, frm);
    		}
    	});
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) {
    	SwingUtilities.invokeLater(() -> {
        	var lst = new ArrayList<>(removed.entrySet().stream().toList());
        	Collections.reverse(lst);
    		lst.forEach(e -> {
            	model.add(e.getKey(), e.getValue()); 
        	});
        	removed.clear();	
    	});
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) {
        return execute(monitor, info);
    }
}
