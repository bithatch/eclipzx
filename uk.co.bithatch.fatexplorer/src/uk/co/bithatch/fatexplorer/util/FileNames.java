package uk.co.bithatch.fatexplorer.util;

import java.net.URI;

public class FileNames {
	public static String getURIFileName(String uri) {
		var uriObj = URI.create(uri);
		return getURIFileName(uriObj);
	}

	public static String getURIFileName(URI uriObj) {
		var path = uriObj.getPath();
		return getPathFileName(path);
	}

	public static String getPathFileName(String path) {
		var lastIdx = path.length() > 1 ? path.lastIndexOf('/', path.length() - 1) : -1;
		return lastIdx == -1 ? path : path.substring(lastIdx + 1);
	}
}
