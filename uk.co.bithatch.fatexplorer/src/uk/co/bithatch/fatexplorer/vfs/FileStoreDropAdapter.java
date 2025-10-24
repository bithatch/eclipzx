package uk.co.bithatch.fatexplorer.vfs;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

public class FileStoreDropAdapter extends CommonDropAdapterAssistant {

	/**
	 * Override to perform any one-time initialization.
	 */
    @Override
	protected void doInit() {
		System.out.println("doInit");
	}
	
    @Override
    public IStatus validateDrop(Object target, int operation, TransferData transferType) {
        return FileStoreTransfer.getInstance().isSupportedType(transferType)
        		? Status.OK_STATUS : Status.CANCEL_STATUS;
    }

    @Override
	public IStatus handleDrop(CommonDropAdapter aDropAdapter, DropTargetEvent event, Object target) {
        if (!FileStoreTransfer.getInstance().isSupportedType(event.currentDataType)) {
            return Status.CANCEL_STATUS;
        }

        Object data = FileStoreTransfer.getInstance().nativeToJava(event.currentDataType);
        if (!(data instanceof IFileStore[])) {
            return Status.CANCEL_STATUS;
        }

        IFileStore[] files = (IFileStore[]) data;
        IContainer container = getTargetContainer(target);
        if (container == null) return Status.CANCEL_STATUS;

        for (IFileStore vf : files) {
            try {
                IFile file = container.getFile(new Path(vf.getName()));
                if (file.exists()) file.delete(true, null);
                file.create(vf.openInputStream(EFS.NONE, new NullProgressMonitor()), true, null);
            } catch (CoreException e) {
                return new Status(IStatus.ERROR, "your.plugin.id", "Failed to import", e);
            }
        }

        return Status.OK_STATUS;
    }

    private IContainer getTargetContainer(Object target) {
        if (target instanceof IContainer) {
            return (IContainer) target;
        } else if (target instanceof IAdaptable) {
            return ((IAdaptable) target).getAdapter(IContainer.class);
        }
        return ResourcesPlugin.getWorkspace().getRoot(); // fallback
    }
}
