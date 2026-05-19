package uk.co.bithatch.tnfs.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import uk.co.bithatch.tnfs.client.TNFSClient;
import uk.co.bithatch.tnfs.client.TNFSMount;
import uk.co.bithatch.tnfs.lib.DirEntryFlag;
import uk.co.bithatch.tnfs.lib.ModeFlag;
import uk.co.bithatch.tnfs.lib.OpenFlag;
import uk.co.bithatch.tnfs.lib.Protocol;
import uk.co.bithatch.tnfs.lib.TNFS;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

/**
 * EFS IFileStore implementation that delegates to the TNFS client API directly.
 * Each IFileStore wraps a tnfs:// URI and resolves it to a TNFSMount for all operations.
 */
public class TNFSEclipseFileStore extends FileStore {

	private final URI uri;

	private record MountEntry(TNFSClient client, TNFSMount mount) implements AutoCloseable {
		@Override
		public void close() throws Exception {
			try {
				mount.close();
			} finally {
				client.close();
			}
		}
	}

	// Cache of open TNFSMount instances keyed by authority (host:port)
	private static final Map<String, MountEntry> MOUNT_CACHE = Collections.synchronizedMap(new HashMap<>());

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
			var mount = resolveMount();
			var tnfsPath = resolvePath();
			try {
				var names = new ArrayList<String>();
				try (var dir = mount.directory(tnfsPath)) {
					dir.stream().forEach(entry -> {
						if (!DirEntryFlag.isSpecial(entry.flags())) {
							names.add(entry.name());
						}
					});
				}
				return names.toArray(new String[0]);
			} catch (java.nio.file.NotDirectoryException e) {
				return new String[0];
			}
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to list TNFS directory: " + uri, e));
		}
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		var info = new FileInfo(getName());
		try {
			var mount = resolveMount();
			var tnfsPath = resolvePath();
			try {
				var stat = mount.stat(tnfsPath);
				info.setExists(true);
				var modes = Arrays.asList(stat.mode());
				info.setDirectory(modes.contains(ModeFlag.IFDIR));
				info.setLength(stat.size());
				if (stat.mtime() != null) {
					info.setLastModified(stat.mtime().toMillis());
				}
			} catch (java.nio.file.NoSuchFileException e) {
				info.setExists(false);
			}
		} catch (IllegalArgumentException | IOException e) {
			info.setExists(false);
		}
		return info;
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			var mount = resolveMount();
			var tnfsPath = resolvePath();
			var channel = mount.open(tnfsPath, OpenFlag.READ);
			return channelToInputStream(channel);
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to open TNFS file for reading: " + uri, e));
		}
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
			var mount = resolveMount();
			var tnfsPath = resolvePath();
			var append = (options & EFS.APPEND) != 0;
			var flags = append
					? new OpenFlag[] { OpenFlag.WRITE, OpenFlag.CREATE, OpenFlag.APPEND }
					: new OpenFlag[] { OpenFlag.WRITE, OpenFlag.CREATE, OpenFlag.TRUNCATE };
			var channel = mount.open(tnfsPath, ModeFlag.DEFAULT_WRITABLE_FLAGS, flags);
			return channelToOutputStream(channel);
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to open TNFS file for writing: " + uri, e));
		}
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		try {
			var mount = resolveMount();
			var tnfsPath = resolvePath();
			if ((options & EFS.SHALLOW) != 0) {
				mount.mkdir(tnfsPath);
			} else {
				// Create directories recursively
				mkdirs(mount, tnfsPath);
			}
			return this;
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to create TNFS directory: " + uri, e));
		}
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		try {
			var mount = resolveMount();
			var tnfsPath = resolvePath();
			try {
				var stat = mount.stat(tnfsPath);
				if (Arrays.asList(stat.mode()).contains(ModeFlag.IFDIR)) {
					mount.rmdir(tnfsPath);
				} else {
					mount.unlink(tnfsPath);
				}
			} catch (java.nio.file.NoSuchFileException e) {
				// Already gone, nothing to do
			}
		} catch (IOException e) {
			throw new CoreException(Status.error("Failed to delete TNFS file: " + uri, e));
		}
	}

	/**
	 * Resolve this store's URI to a TNFSMount via the TNFS client API.
	 */
	private TNFSMount resolveMount() throws IOException {
		var protocol = Protocol.TCP;
		var fragment = uri.getFragment();
		if (fragment != null && !fragment.isEmpty()) {
			try {
				protocol = Protocol.valueOf(fragment.toUpperCase());
			} catch (IllegalArgumentException e) {
				// default to TCP
			}
		}
		var authority = uri.getHost() + ":" + (uri.getPort() > 0 ? uri.getPort() : TNFS.DEFAULT_PORT) + ":" + protocol.name();
		var fProtocol = protocol;
		var entry = MOUNT_CACHE.computeIfAbsent(authority, k -> {
			try {
				var bldr = new TNFSClient.Builder()
						.withHostname(uri.getHost())
						.withProtocol(fProtocol);
				if (uri.getPort() > 0) {
					bldr.withPort(uri.getPort());
				}
				var client = bldr.build();

				var mountBldr = client.mount("/");
				if (uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
					mountBldr.withUsername(uri.getUserInfo());
				}
				var mount = mountBldr.build();
				return new MountEntry(client, mount);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create TNFS mount for " + authority, e);
			}
		});
		return entry.mount();
	}

	/**
	 * Get the TNFS path from the URI.
	 */
	private String resolvePath() {
		var path = uri.getPath();
		if (path == null || path.isEmpty()) return "/";
		return path;
	}

	/**
	 * Recursively create directories.
	 */
	private void mkdirs(TNFSMount mount, String path) throws IOException {
		if (path.equals("/") || path.isEmpty()) return;
		try {
			mount.stat(path);
			return; // already exists
		} catch (java.nio.file.NoSuchFileException e) {
			// doesn't exist, continue to create
		}
		var parent = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
		if (!parent.isEmpty() && !parent.equals("/")) {
			mkdirs(mount, parent);
		}
		mount.mkdir(path);
	}

	private URI withPath(String newPath) {
		try {
			return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), newPath, null, null);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Close all cached mounts. Should be called on plugin stop.
	 */
	public static void closeAll() {
		synchronized (MOUNT_CACHE) {
			for (var entry : MOUNT_CACHE.values()) {
				try {
					entry.close();
				} catch (Exception e) {
					// ignore
				}
			}
			MOUNT_CACHE.clear();
		}
	}

	/**
	 * Remove a specific cached mount (e.g. when config changes).
	 */
	public static void invalidateCache(String host, int port) {
		var key = host + ":" + (port > 0 ? port : TNFS.DEFAULT_PORT);
		synchronized (MOUNT_CACHE) {
			var entry = MOUNT_CACHE.remove(key);
			if (entry != null) {
				try {
					entry.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	private static InputStream channelToInputStream(SeekableByteChannel channel) {
		return new InputStream() {
			private final ByteBuffer single = ByteBuffer.allocate(1);

			@Override
			public int read() throws IOException {
				single.clear();
				int n = channel.read(single);
				if (n <= 0) return -1;
				single.flip();
				return Byte.toUnsignedInt(single.get());
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				var buf = ByteBuffer.wrap(b, off, len);
				int n = channel.read(buf);
				return n == 0 ? -1 : n;
			}

			@Override
			public void close() throws IOException {
				channel.close();
			}
		};
	}

	private static OutputStream channelToOutputStream(SeekableByteChannel channel) {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				channel.write(ByteBuffer.wrap(new byte[] { (byte) b }));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				channel.write(ByteBuffer.wrap(b, off, len));
			}

			@Override
			public void close() throws IOException {
				channel.close();
			}
		};
	}
}
