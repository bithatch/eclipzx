package uk.co.bithatch.fatexplorer.preferences;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FATLock {
	
	public interface LockListener {
		void lockStateChanged(URI uri, boolean locked);
	}

	private final static Map<String, String> lockedImages = Collections.synchronizedMap(new HashMap<>());
	private final static List<LockListener> listeners = new ArrayList<>();
	
	public static void addListener(LockListener listener) {
		synchronized(listeners) {
			listeners.add(listener);
		}
	}
	
	public static void removeListener(LockListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}

	public static void unlockImage(URI uri) {
		if(lockedImages.remove(uri.getPath()) == null)
			throw new IllegalStateException("Image " + uri + " is not locked.");
		listeners.forEach(l -> l.lockStateChanged(uri, false));
	}
	
	public static boolean isImageLocked(URI uri) {
		var key = uri.getFragment();
		var val = lockedImages.get(uri.getPath());
		return val != null && !val.equals(key);
	}
	
	public static URI lockImage(URI uri) {
		if(lockedImages.containsKey(uri.getPath()))
			throw new IllegalStateException("Image " + uri + " is already locked.");
		String key = UUID.randomUUID().toString();
		lockedImages.put(uri.getPath(), key);
		listeners.forEach(l -> l.lockStateChanged(uri, true));
		return FATPreferencesAccess.addKeyToURI(uri, key);
	}
}
