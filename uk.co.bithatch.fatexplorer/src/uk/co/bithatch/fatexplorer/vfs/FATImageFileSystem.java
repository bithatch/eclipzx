package uk.co.bithatch.fatexplorer.vfs;

import static uk.co.bithatch.bitzx.URIS.stripTrailingSlash;

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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.fatexplorer.preferences.FATLock;
import uk.co.bithatch.fatexplorer.preferences.FATLock.LockListener;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.zyxy.mmc.SDCard;

public class FATImageFileSystem extends FileSystem implements IPreferenceChangeListener, LockListener {
	private final static ILog LOG = ILog.of(FATImageFileSystem.class);

	private final Map<String, SDCard> deviceCache = new HashMap<>();
	private final Map<String, FATImageFileStore> storeCache = new HashMap<>();
	
	public FATImageFileSystem() {
		FATPreferencesAccess.getPreferences().addPreferenceChangeListener(this);
		FATLock.addListener(this);
	}

	@Override
	public int attributes() {
		return EFS.NONE;
	}

	@Override
	public IFileStore getStore(URI uri) {
		synchronized(storeCache) {
		
			var diskImg = FATPreferencesAccess.getImageForURI(uri);

			if(FATLock.isImageLocked(diskImg)) {
				throw new IllegalStateException("Disk image is locked.");
			}
			
			var diskPath = FATPreferencesAccess.getPathForURI(uri);
			var diskFile = toDiskFile(diskImg);
			var remainingPath = stripTrailingSlash(diskPath);

			var device = deviceCache.computeIfAbsent(diskImg.toString(), p -> {
				LOG.info("Opening disk image " + p);
				return new SDCard.Builder().withFile(diskFile).withMBR().withReadWrite().build();
			});
			
			var rootStore = storeCache.computeIfAbsent(diskImg.toString(), p -> {
				return new FATImageFileStore(uri, this, "/", device.fileSystem());
			});

			if (!remainingPath.equals("")) {
				rootStore = (FATImageFileStore) rootStore.getFileStore(IPath.forPosix(remainingPath.substring(1)));
			}

			return rootStore;
		}
	}

	public static File toDiskFile(URI uri) {
		try {
			String uriPath = uri.getPath();
			var diskFile = new File(URLDecoder.decode(uriPath.substring(1), "UTF-8"));
			if (!diskFile.isAbsolute()) {
				var wsRoot = PlatformUI.getWorkbench().getAdapter(IWorkspace.class).
						getRoot();
				var wsFile = wsRoot.findMember(diskFile.toString());
				if(wsFile == null)
					throw new IllegalArgumentException("Disk image file " + diskFile + " does not exist in the workspace.");
				diskFile = wsFile.getLocation().toFile();
			}
			return diskFile;
		}
		catch (UnsupportedEncodingException uee) {
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

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		resetStoreCache();
	}
	
	public void closeStore(FATImageFileStore store) throws IOException {
		synchronized(storeCache) {
			var p = FATPreferencesAccess.getImageForURI(store.toURI()).toString();
			if(storeCache.containsKey(p)) {
				try {
					LOG.info("Closing disk image " + p + " (close store)");
					deviceCache.get(p).close();
				} finally {
					storeCache.remove(p);
					deviceCache.remove(p);
				}
				return;
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
		resetStoreCache();
	}

}
