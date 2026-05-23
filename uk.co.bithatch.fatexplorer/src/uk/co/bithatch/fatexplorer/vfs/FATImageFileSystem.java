package uk.co.bithatch.fatexplorer.vfs;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;

import uk.co.bithatch.fatexplorer.FATDiskImageManager;
import uk.co.bithatch.fatexplorer.preferences.FATLock;
import uk.co.bithatch.fatexplorer.preferences.FATLock.LockListener;
import uk.co.bithatch.zyxy.mmc.SDCard;

public class FATImageFileSystem extends FileSystem implements LockListener {
	private final static ILog LOG = ILog.of(FATImageFileSystem.class);

	private final Map<String, SDCard> deviceCache = new HashMap<>();
	private final Map<String, FATImageFileStore> storeCache = new HashMap<>();
	
	public FATImageFileSystem() {
		FATLock.addListener(this);
	}

	@Override
	public int attributes() {
		return EFS.NONE;
	}

	@Override
	public IFileStore getStore(URI uri) {
		synchronized(storeCache) {
			/* The URI path may encode just the disk image, or the disk image
			 * plus a path inside the FAT filesystem (appended by resolve()).
			 * E.g. fatimg:////home/user/disk.img         (just the image)
			 *      fatimg:////home/user/disk.img/subfolder (image + in-image path)
			 *      fatimg:/project/Debug/disk.img          (workspace-relative)
			 *      fatimg:/project/Debug/disk.img/subfolder (workspace-relative + in-image)
			 */
			var parsed = findDiskFile(uri);
			var diskFile = parsed.diskFile;
			var remainingPath = parsed.remainingPath;
			var diskKey = diskFile.getAbsolutePath();

			if(FATLock.isImageLocked(uri)) {
				// Image is locked for exclusive access. Return a non-existent
				// placeholder store so that Eclipse's internal refresh/tree-walk
				// code (which cannot supply a lock key) doesn't crash.
				// Callers with the lock key will pass through isImageLocked()
				// and reach the real store below.
				LOG.info("Disk image is locked, returning empty placeholder store for " + uri);
				return EFS.getNullFileSystem().getStore(uri);
			}

			if (!diskFile.exists()) {
				LOG.error("Disk image file does not exist: " + diskFile);
				return null;
			}

			var device = deviceCache.computeIfAbsent(diskKey, p -> {
				LOG.info("Opening disk image " + p);
				return new SDCard.Builder().withFile(diskFile).withMBR().withReadWrite().build();
			});
			
			var rootStore = storeCache.computeIfAbsent(diskKey, p -> {
				return new FATImageFileStore(uri, this, "/", device.fileSystem());
			});

			// Navigate into the FAT filesystem if there's a remaining path
			if (remainingPath != null && !remainingPath.isEmpty() && !remainingPath.equals("/")) {
				var pathToNavigate = remainingPath;
				if (pathToNavigate.startsWith("/")) pathToNavigate = pathToNavigate.substring(1);
				return rootStore.getFileStore(IPath.forPosix(pathToNavigate));
			}

			return rootStore;
		}
	}

	/**
	 * Result of parsing a URI into a disk file and remaining in-image path.
	 */
	private record ParsedImagePath(File diskFile, String remainingPath) {}

	/**
	 * Find the actual disk image file from a URI path that may contain
	 * an in-image path suffix. Walks the path segments to find the first
	 * existing file (the .img file), and returns the remainder as the
	 * in-image path.
	 * <p>
	 * For absolute paths (4-slash prefix), strips leading slashes first.
	 * For workspace-relative paths, resolves via workspace root.
	 */
	private ParsedImagePath findDiskFile(URI uri) {
		try {
			String uriPath = uri.getPath();
			var decoded = URLDecoder.decode(uriPath.substring(1), "UTF-8");
			
			// Check if this is an absolute path (starts with // after stripping first /)
			if (decoded.startsWith("//")) {
				// Absolute path: strip leading slashes to get native path
				var nativePath = decoded;
				while (nativePath.startsWith("/")) {
					nativePath = nativePath.substring(1);
				}
				nativePath = "/" + nativePath; // re-add single leading slash for absolute
				
				// Walk path to find the image file
				return findImageInPath(nativePath);
			} else {
				// Workspace-relative path (e.g. "project/Debug/disk.img/subfolder")
				var wsRoot = ResourcesPlugin.getWorkspace().getRoot();
				
				// Walk segments to find the image file
				var segments = decoded.split("/");
				var pathBuilder = new StringBuilder();
				for (int i = 0; i < segments.length; i++) {
					if (i > 0) pathBuilder.append("/");
					pathBuilder.append(segments[i]);
					
					var wsFile = wsRoot.findMember(pathBuilder.toString());
					if (wsFile != null && wsFile.getType() == org.eclipse.core.resources.IResource.FILE) {
						var diskFile = wsFile.getLocation().toFile();
						if (diskFile.exists()) {
							var remaining = decoded.substring(pathBuilder.length());
							return new ParsedImagePath(diskFile, remaining);
						}
					}
				}
				
				// Fallback: try resolving the full path from workspace root location
				// (file may exist on disk but not yet known to Eclipse)
				var fullPath = wsRoot.getLocation().append(decoded).toFile();
				return findImageInPath(fullPath.getAbsolutePath());
			}
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalArgumentException(uee);
		}
	}

	/**
	 * Walk an absolute native path to find the first segment that is an existing file
	 * (the disk image), and return it along with the remaining path (in-image path).
	 */
	private ParsedImagePath findImageInPath(String nativePath) {
		var file = new File(nativePath);
		if (file.exists() && file.isFile()) {
			return new ParsedImagePath(file, "");
		}
		
		// Walk up from the full path to find the image file
		var current = nativePath;
		while (true) {
			var f = new File(current);
			if (f.exists() && f.isFile()) {
				var remaining = nativePath.substring(current.length());
				return new ParsedImagePath(f, remaining);
			}
			var lastSlash = current.lastIndexOf('/');
			if (lastSlash <= 0) break;
			current = current.substring(0, lastSlash);
		}
		
		// Nothing found — return the full path as the file (will fail with "does not exist")
		return new ParsedImagePath(new File(nativePath), "");
	}

	/**
	 * Resolve a URI to its disk file. Used by {@link FATDiskImageManager}
	 * to check if the image exists before mounting.
	 * Only handles the image file itself, not in-image paths.
	 */
	public static File toDiskFile(URI uri) {
		try {
			String uriPath = uri.getPath();
			var decoded = URLDecoder.decode(uriPath.substring(1), "UTF-8");
			
			if (decoded.startsWith("//")) {
				// Absolute path
				var nativePath = decoded;
				while (nativePath.startsWith("/")) {
					nativePath = nativePath.substring(1);
				}
				return new File("/" + nativePath);
			} else {
				// Workspace-relative path
				var wsRoot = ResourcesPlugin.getWorkspace().getRoot();
				var wsFile = wsRoot.findMember(decoded);
				if (wsFile != null) {
					return wsFile.getLocation().toFile();
				}
				// Fallback: resolve from workspace root location
				return wsRoot.getLocation().append(decoded).toFile();
			}
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalArgumentException(uee);
		}
	}

	@Override
	public boolean canDelete() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public boolean isCaseSensitive() {
		return false;
	}

	public void closeStore(FATImageFileStore store) throws IOException {
		synchronized(storeCache) {
			var diskFile = toDiskFile(store.toURI());
			var diskKey = diskFile.getAbsolutePath();
			if(storeCache.containsKey(diskKey)) {
				try {
					LOG.info("Closing disk image " + diskKey + " (close store)");
					deviceCache.get(diskKey).close();
				} finally {
					storeCache.remove(diskKey);
					deviceCache.remove(diskKey);
				}
			}
		}
	}

	protected void resetStoreCache() {
		synchronized(storeCache) {
			deviceCache.values().forEach(v -> {
				try {
					LOG.info("Closing disk image " + v + " (reset store cache)");
					v.close();
				} catch (IOException e) {
				}
			});
			storeCache.clear();
			deviceCache.clear();
		}
	}

	@Override
	public void lockStateChanged(URI uri, boolean locked) {
		// Only close the store for the specific image whose lock state changed,
		// not all stores. Closing all stores causes stale FATImageFileStore references
		// held by Eclipse's tree viewers for OTHER images to become invalid, leading to
		// cross-contaminated or missing content.
		synchronized(storeCache) {
			var diskFile = toDiskFile(uri);
			var diskKey = diskFile.getAbsolutePath();
			var device = deviceCache.remove(diskKey);
			if (device != null) {
				try {
					LOG.info("Closing disk image " + diskKey + " (lock state changed: " + (locked ? "locked" : "unlocked") + ")");
					device.close();
				} catch (IOException e) {
					LOG.error("Failed to close disk image " + diskKey, e);
				}
			}
			storeCache.remove(diskKey);
		}
		
		// Refresh linked folders and image file icons on any lock state change.
		// On unlock: re-reads the now-accessible content.
		// On lock: updates the .img file icon to show locked state.
		FATDiskImageManager.refreshLinkedFoldersForImage(uri);
	}

	/**
	 * Close all open stores whose disk image files are under the given project's
	 * location. Called when a project is closed or deleted to release file handles.
	 */
	public void closeStoresForProject(File projectLocation) {
		synchronized(storeCache) {
			var projectPath = projectLocation.getAbsolutePath();
			var keysToRemove = new java.util.ArrayList<String>();
			for (var key : storeCache.keySet()) {
				if (key.startsWith(projectPath)) {
					keysToRemove.add(key);
				}
			}
			for (var key : keysToRemove) {
				var device = deviceCache.remove(key);
				if (device != null) {
					try {
						LOG.info("Closing disk image " + key + " (project closed)");
						device.close();
					} catch (IOException e) {
						LOG.error("Failed to close disk image " + key, e);
					}
				}
				storeCache.remove(key);
			}
		}
	}

}
