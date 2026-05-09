package uk.co.bithatch.fatexplorer.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FATLock {
	
	public interface LockListener {
		void lockStateChanged(String uri, boolean locked);
	}

	private final static Set<String> lockedImages = Collections.synchronizedSet(new HashSet<>());
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

	public static void unlockImage(String uri) {
		if(!lockedImages.remove(uri))
			throw new IllegalStateException("Image " + uri + " is not locked.");
		listeners.forEach(l -> l.lockStateChanged(uri, false));
	}
	
	public static boolean isImageLocked(String uri) {
		return lockedImages.contains(uri);
	}
	
	public static void lockImage(String uri) {
		if(!lockedImages.add(uri))
			throw new IllegalStateException("Image " + uri + " is already locked.");
		listeners.forEach(l -> l.lockStateChanged(uri, true));
	}
}
