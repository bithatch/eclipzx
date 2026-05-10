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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.fatexplorer.preferences.FATLock;
import uk.co.bithatch.fatexplorer.preferences.FATLock.LockListener;
import uk.co.bithatch.fatexplorer.preferences.FATPreferencesAccess;
import uk.co.bithatch.zyxy.mmc.SDCard;

public class FATImageFileSystem extends FileSystem implements IPreferenceChangeListener, LockListener {

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
		
		var imgUUID = uri.getAuthority();
		if (imgUUID == null || imgUUID.isEmpty()) {
			return null;
		}

		var diskImg = FATPreferencesAccess.getPathForURI(uri);
		var diskFile = toDiskFile(diskImg);
		try {
			var remainingPath = FATPreferencesAccess.stripTrailingSlash(URLDecoder.decode(uri.getPath(), "UTF-8"));

			var device = deviceCache.computeIfAbsent(diskImg, p -> {
				return new SDCard.Builder().withFile(diskFile).withMBR().withReadWrite().build();
			});
			
			var rootStore = storeCache.computeIfAbsent(diskImg, p -> {
				return new FATImageFileStore(imgUUID, this, "/", device.fileSystem());
			});

			if (!remainingPath.equals("")) {
				rootStore = (FATImageFileStore) rootStore.getFileStore(IPath.forPosix(remainingPath.substring(1)));
			}

			return rootStore;
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException(uee);
		}
	}

	public static File toDiskFile(String diskImg) {
		var diskFile = new File(diskImg);
		if (!diskFile.isAbsolute()) {
			diskFile = new File(
					PlatformUI.getWorkbench().getAdapter(IWorkspace.class).getRoot().getRawLocation().toFile(),
					diskFile.toString());
		}
		return diskFile;
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

	protected void resetStoreCache() {
		synchronized(storeCache) {
			deviceCache.values().forEach(v -> {
				try {
					v.close();
				} catch (IOException e) {
				}
			});
			storeCache.clear();
			deviceCache.clear();
		}
	}

	@Override
	public void lockStateChanged(String uri, boolean locked) {
		resetStoreCache();
	}

}
