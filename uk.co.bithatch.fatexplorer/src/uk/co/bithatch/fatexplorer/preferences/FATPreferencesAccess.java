package uk.co.bithatch.fatexplorer.preferences;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import uk.co.bithatch.fatexplorer.Activator;

public class FATPreferencesAccess {

	public static String getPathForUUID(String uuid) {
		return getPathForURI(URI.create(PreferenceConstants.SCHEME + "://" + uuid));
	}
	
	public static String stripTrailingSlash(String path) {
		while(path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		return path;
	}
	
	public static String rootUri(URI uri) {
		return uri.getScheme() + "://" + uri.getAuthority();
	}
	
	public static String getPathForURI(URI uri) {
		var rooturi = rootUri(uri);
		return getURIs().stream().
//				peek(u -> System.out.println("peek1 " + u + " vs " + uri)).
				filter(u -> rooturi.equals(encode(u))).
//				peek(u -> System.out.println("peek2 " + u + " vs " + uri)).
				findFirst().
				orElseThrow(() -> new IllegalArgumentException("No image with this URI " + uri));
		
	}
	
	public static List<URI> getConfiguredImageURIs() {
		var uriStrings = getURIs();
		return uriStrings.stream().
				map(FATPreferencesAccess::encode).
				map(URI::create).
				toList();
	}
	
	public static URI encodeToURI(String path) {
		return URI.create(encode(path));
	}
	
	public static String encode(String uri) {
		try {
			return PreferenceConstants.SCHEME + "://" + UUID.nameUUIDFromBytes(uri.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static List<String> getURIs() {
		var lstr = getPreferences().get(PreferenceConstants.DISK_IMAGES, "");
		return lstr.equals("") ? Collections.emptyList() : Arrays.asList(lstr.
			split(Pattern.quote(File.pathSeparator)));
	}
    
	public static IPreferenceStore getPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID);
	}

	public static IEclipsePreferences getPreferences() {
		return InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
	}

	public static void removeImagePath(String uriStr) {
		var list = new LinkedHashSet<>(getURIs());
		list.remove(uriStr);
		getPreferences().put(PreferenceConstants.DISK_IMAGES, String.join(File.pathSeparator, list));
	}

	public static void addImagePath(String uriStr) {
		var list = new LinkedHashSet<>(getURIs());
		list.add(uriStr);
		getPreferences().put(PreferenceConstants.DISK_IMAGES, String.join(File.pathSeparator, list));
	}
}
