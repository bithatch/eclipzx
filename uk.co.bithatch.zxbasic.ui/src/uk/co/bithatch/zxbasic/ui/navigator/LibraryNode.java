package uk.co.bithatch.zxbasic.ui.navigator;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import uk.co.bithatch.zxbasic.ui.library.ZXLibrary;

public class LibraryNode implements ILibraryContentsNode {

	private LibrariesNode libs;
	private ZXLibrary library;

	public LibraryNode(LibrariesNode libs, ZXLibrary library) {
		this.libs = libs;
		this.library = library;
	}

	public LibrariesNode getLibraries() {
		return libs;
	}

	public ZXLibrary getLibrary() {
		return library;
	}

	@Override
	public File getFile() {
		return library.location();
	}

	@Override
	public InputStream getContents() throws CoreException {
		return null;
	}

	@Override
	public IPath getFullPath() {
		return IPath.fromFile(library.location());
	}

	@Override
	public String getName() {
		return library.name();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}
