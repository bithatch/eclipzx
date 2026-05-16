package uk.co.bithatch.tnfs.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import uk.co.bithatch.tnfs.nio.TNFSFileSystemProvider;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

/**
 * EFS IFileStore implementation that delegates to the TNFS NIO FileSystem provider.
 * Each IFileStore wraps a tnfs:// URI and resolves it to a NIO Path for all operations.
 */
public class TNFSEclipseFileStore extends FileStore {

	private final URI uri;

	// Single shared TNFSFileSystemProvider instance (bypasses ServiceLoader which doesn't work in OSGi)
	private static final TNFSFileSystemProvider PROVIDER = new TNFSFileSystemProvider();

	// Cache of open NIO file systems keyed by authority (host:port)
	private static final Map<String, FileSystem> FS_CACHE = Collections.synchronizedMap(new HashMap<>());

	public TNFSEclipseFileStore(URI uri) {
		this.uri = uri;
	}

	@Override
	public URI toURI() {
		return uri;
	}

	@Override
	public String getName() {
		var path = uri.getPath();
		if (path == null || path.isEmpty() || "/".equals(path)) {
			return uri.getHost();
		}
		var idx = path.lastIndexOf('/');
		return idx >= 0 && idx < path.length() - 1 ? path.substring(idx + 1) : path;
	}

	@Override
	public IFileStore getParent() {
		var path = uri.getPath();
		if (path == null || path.isEmpty() || "/".equals(path)) {
			return null;
		}
		var idx = path.lastIndexOf('/');
		if (idx <= 0) {
			return new TNFSEclipseFileStore(withPath("/"));
		}
		return new TNFSEclipseFileStore(withPath(path.substring(0, idx)));
	}

	@Override
	public IFileStore getChild(String name) {
		var path = uri.getPath();
		if (path == null || path.isEmpty()) path = "/";
		if (!path.endsWith("/")) path += "/";
		return new TNFSEclipseFileStore(withPath(path + name));
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		try {
			var nioPath = resolveNioPath();
			if (!Files.isDirectory(nioPath)) {
				return new String[0];
			}
			var names = new ArrayList<String>();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(nioPath)) {
				try {
					for (Path entry : stream) {
						names.add(entry.getFileName().toString());
					}	
				}
				catch(UncheckedIOException nde) {
					if(nde.getCause() instanceof NotDirectoryException) {
						// This can happen if the directory is unreadable for some reason (e.g. permissions) - treat it as empty rather than failing
						return new String[0];
					}
				}
			}
			return names.toArray(new String[0]);
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to list TNFS directory: " + uri, e));
		}
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		var info = new FileInfo(getName());
		try {
			var nioPath = resolveNioPath();
			if (Files.exists(nioPath)) {
				info.setExists(true);
				var attrs = Files.readAttributes(nioPath, BasicFileAttributes.class);
				info.setDirectory(attrs.isDirectory());
				info.setLength(attrs.size());
				info.setLastModified(attrs.lastModifiedTime().toMillis());
			} else {
				info.setExists(false);
			}
		} catch (IOException e) {
			info.setExists(false);
		}
		return info;
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			return Files.newInputStream(resolveNioPath());
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to open TNFS file for reading: " + uri, e));
		}
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			var nioPath = resolveNioPath();
			if ((options & EFS.APPEND) != 0) {
				return Files.newOutputStream(nioPath, java.nio.file.StandardOpenOption.APPEND,
						java.nio.file.StandardOpenOption.CREATE);
			}
			return Files.newOutputStream(nioPath);
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to open TNFS file for writing: " + uri, e));
		}
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		try {
			var nioPath = resolveNioPath();
			if ((options & EFS.SHALLOW) != 0) {
				Files.createDirectory(nioPath);
			} else {
				Files.createDirectories(nioPath);
			}
			return this;
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to create TNFS directory: " + uri, e));
		}
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		try {
			Files.deleteIfExists(resolveNioPath());
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to delete TNFS file: " + uri, e));
		}
	}

	/**
	 * Resolve this store's URI to a NIO Path via the TNFS NIO FileSystemProvider.
	 */
	private Path resolveNioPath() throws IOException {
		var authority = uri.getHost() + ":" + (uri.getPort() > 0 ? uri.getPort() : uk.co.bithatch.tnfs.lib.TNFS.DEFAULT_PORT);
		var fs = FS_CACHE.computeIfAbsent(authority, k -> {
			try {
				// Build a URI with just the authority for getting the file system
				var fsUri = new URI("tnfs", uri.getUserInfo(), uri.getHost(),
						uri.getPort(), "/", null, null);
				try {
					return PROVIDER.getFileSystem(fsUri);
				} catch (Exception e) {
					var env = new HashMap<String, Object>();
					if (uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
						env.put("username", uri.getUserInfo());
					}
					return PROVIDER.newFileSystem(fsUri, env);
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to create TNFS file system for " + authority, e);
			}
		});

		var path = uri.getPath();
		if (path == null || path.isEmpty()) path = "/";
		return fs.getPath(path);
	}

	private URI withPath(String newPath) {
		try {
			return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), newPath, null, null);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
