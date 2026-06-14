package uk.co.bithatch.bitzx;

import java.nio.file.Path;
import java.nio.file.Paths;

public class URIS {

	public static Path toPath(String uriStr) {
		try {
			if (uriStr.startsWith("file:")) {
				return Paths.get(java.net.URI.create(uriStr));
			}
			return Paths.get(uriStr);
		} catch (Exception e) {
			return null;
		}
	}

	public static String stripLeadingSlash(String path) {
		while(path.startsWith("/"))
			path = path.substring(1);
		return path;
	}
	
	public static String stripTrailingSlash(String path) {
		while(path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		return path;
	}
}
