package uk.co.bithatch.fatexplorer.preferences;

import static uk.co.bithatch.bitzx.URIS.stripLeadingSlash;
import static uk.co.bithatch.bitzx.URIS.stripTrailingSlash;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import uk.co.bithatch.fatexplorer.Activator;

public class FATPreferencesAccess {

	public static void addImagePath(String uriStr) {
		var list = new LinkedHashSet<>(getURIs());
		list.add(uriStr);
		getPreferences().put(PreferenceConstants.DISK_IMAGES, String.join(File.pathSeparator, list));
	}

	public static URI encodeToURI(String path) {
		try {
        	return new URI(PreferenceConstants.SCHEME, null, (path.startsWith("/") ? "///" : "/") +  path, null);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}
	
	public static List<URI> getConfiguredImageURIs() {
		var uriStrings = getURIs();
		return uriStrings.stream().
				map(FATPreferencesAccess::encodeToURIOrNull).
				filter(s -> s != null).
				toList();
	}
	
	public static URI getImageForPath(String path) {
		return getConfiguredImageURIs().stream().
				filter(u -> matchesPath(path, u)).
				findFirst().
				orElseThrow(() -> new IllegalArgumentException("No image with this path " + path));
		
	}

	public static URI getImageForURI(URI uri) {
		return getConfiguredImageURIs().stream().
				filter(u -> matchesURIs(uri, u)).
				findFirst().
				map(u -> {
					/* Add the fragment from the original URI to the matched URI, it will be needed if the image is locked to unlock it. */
					try {
						return new URI(u.getScheme(), u.getUserInfo(), u.getHost(), u.getPort(),
								encodeWorkspaceOrExternalPath(
										u.getPath()/* .substring(1) */), u.getQuery(), uri.getFragment());
					} catch (URISyntaxException e) {
						return u;
					}
				}).
				orElseThrow(() -> new IllegalArgumentException("No image with this URI " + uri));
		
	}

	public static String getPathForURI(URI uri) {
		return getConfiguredImageURIs().stream().
				filter(u -> matchesURIs(uri, u)).
				findFirst().
				map(u -> {
					try {
						return URLDecoder.decode(uri.getPath().substring(u.getPath().length()), "UTF-8");
					} catch (UnsupportedEncodingException e) {
						return uri.getPath().substring(u.getPath().length());
					}
				}).
				orElseThrow(() -> new IllegalArgumentException("No image with this URI " + uri));
		
	}
	
	public static IEclipsePreferences getPreferences() {
		return InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
	}

	public static IPreferenceStore getPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID);
	}

	public static List<String> getURIs() {
		var lstr = getPreferences().get(PreferenceConstants.DISK_IMAGES, "");
		return lstr.equals("") ? Collections.emptyList() : Arrays.asList(lstr.
			split(Pattern.quote(File.pathSeparator)));
	}

	public static void removeImagePath(String uriStr) {
		var list = new LinkedHashSet<>(getURIs());
		list.remove(uriStr);
		getPreferences().put(PreferenceConstants.DISK_IMAGES, String.join(File.pathSeparator, list));
	}

	public static URI resolve(URI uri, String path) {
		return resolve(uri, path, uri.getFragment());
	}
	
	static URI addKeyToURI(URI uri, String key) {
		try {
			return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), encodeWorkspaceOrExternalPath(uri.getPath()), uri.getQuery(), key);
		} catch (URISyntaxException e) {
			return uri;
		}
	}
	
	static URI resolve(URI uri, String path, String key) {
		if(!path.equals("") && !path.equals("\\") && !path.equals("/")) {
			uri = FATPreferencesAccess.addTrailingSlashToPath(uri);
			uri = uri.resolve(path.substring(1));
			uri = FATPreferencesAccess.addKeyToURI(uri, key);
		}
		return uri;
	}

	
	private static URI addTrailingSlashToPath(URI uri) {
		try {
			var path = uri.getPath();
			if(path == null) {
				path = "/";
			}
			else if(!path.endsWith("/")) {
				path = path + "/";
			}
			return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), encodeWorkspaceOrExternalPath(path), uri.getQuery(), uri.getFragment());
		} catch (URISyntaxException e) {
			return uri;
		}
	}
	
	private static URI encodeToURIOrNull(String path) {
		try {
			return encodeToURI(path);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
    
	private static String encodeWorkspaceOrExternalPath(String path) {
		return ( path.startsWith("//") ? "////" : "/" ) + stripLeadingSlash(path);
	}

	private static boolean matchesPath(String path, URI u) {
		if(path == null || path.equals("")) {
			return false;
		}
		if(path.startsWith("/")) {
			return  stripLeadingSlash(u.getPath()).startsWith(stripLeadingSlash(path));
		}
		else {
			return  stripLeadingSlash(u.getPath()).startsWith(path);
		}
	}

	private static boolean matchesURIs(URI uri, URI u) {
		if(uri.getPath().equals(u.getPath()))
			return true;
		else {
			var p = stripLeadingSlash(stripTrailingSlash(u.getPath()));
			return stripLeadingSlash(uri.getPath()).startsWith(p + "/");
		}
	}
}
