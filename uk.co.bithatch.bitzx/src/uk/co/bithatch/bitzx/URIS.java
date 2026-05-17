package uk.co.bithatch.bitzx;

public class URIS {


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
