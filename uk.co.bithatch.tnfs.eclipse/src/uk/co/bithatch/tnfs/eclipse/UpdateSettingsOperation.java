package uk.co.bithatch.tnfs.eclipse;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class UpdateSettingsOperation extends WorkspaceModifyOperation {

	public UpdateSettingsOperation() {
		System.out.println("bler");
	}

    @Override
    protected void execute(IProgressMonitor monitor)
            throws CoreException, InvocationTargetException, InterruptedException {

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        SubMonitor sub = SubMonitor.convert(
                monitor,
                "Updating TNFS server state",
                IProgressMonitor.UNKNOWN);

        try {
			 TNFSActivator.getDefault().updateState(sub);
        }
        catch(Exception e) {
        	throw new CoreException(Status.error("Failed to update TNFS server state.", e));

        } finally {
            sub.done();
        }
    }

}
