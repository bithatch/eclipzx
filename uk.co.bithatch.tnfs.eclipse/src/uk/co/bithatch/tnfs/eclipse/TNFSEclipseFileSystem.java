package uk.co.bithatch.tnfs.eclipse;

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;

/**
 * Eclipse File System (EFS) provider for tnfs:// URIs.
 * Registered via org.eclipse.core.filesystem.filesystems extension point.
 * Bridges TNFS NIO paths to Eclipse's IFileStore API.
 */
public class TNFSEclipseFileSystem extends FileSystem {

	public TNFSEclipseFileSystem() {
	}

	@Override
	public IFileStore getStore(URI uri) {
		return new TNFSEclipseFileStore(uri);
	}
}
