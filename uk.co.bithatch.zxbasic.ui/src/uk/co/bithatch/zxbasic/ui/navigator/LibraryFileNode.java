package uk.co.bithatch.zxbasic.ui.navigator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;

public class LibraryFileNode implements ILibraryContentsNode, IStorage {

	private ILibraryContentsNode parent;
	private IPath file;

	public LibraryFileNode(ILibraryContentsNode parent, File file) {
		this.file = IPath.fromFile(file);
		this.parent = parent;
	}

	public ILibraryContentsNode getParent() {
		return parent;
	}

	@Override
	public File getFile() {
		return file.toFile();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public InputStream getContents() throws CoreException {
		try {
			return new FileInputStream(file.toFile());
		} catch (FileNotFoundException e) {
			throw new CoreException(Status.error("Failed to open library contents.", e));
		}
	}

	@Override
	public IPath getFullPath() {
		return file;
	}

	@Override
	public String getName() {
		return file.lastSegment();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}
}
