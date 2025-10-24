package uk.co.bithatch.zxbasic.ui.navigator;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;

import uk.co.bithatch.zxbasic.ui.util.FileStorageEditorInput;

public class LibraryFileStorageAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] ADAPTER_LIST = new Class[] { IStorage.class, IEditorInput.class };

    @SuppressWarnings("unchecked")
	@Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == IStorage.class && adaptableObject instanceof LibraryFileNode libNode) {
            return adapterType.cast(libNode);
        }
        else if (adapterType == IEditorInput.class && adaptableObject instanceof LibraryFileNode libNode) {
            return (T)new FileStorageEditorInput(libNode);
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return ADAPTER_LIST;
    }
}
