package uk.co.bithatch.zxbasic.ui.util;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

import uk.co.bithatch.zxbasic.ui.navigator.ILibraryContentsNode;

public class FileStorageEditorInput implements IStorageEditorInput {
	private final ILibraryContentsNode storage;

	public FileStorageEditorInput(ILibraryContentsNode storage) {
		this.storage = storage;
	}

	@Override
	public boolean exists() {
		return storage.getFullPath().toFile().exists();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return storage.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return storage.getFullPath().toOSString();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public IStorage getStorage() {
		return storage;
	}
}
