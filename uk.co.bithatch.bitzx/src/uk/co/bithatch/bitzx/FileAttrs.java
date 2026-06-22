package uk.co.bithatch.bitzx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FileAttrs {

	public static FileTime safeLastModified(Path path) {
		return safeLastModified(path, null);
	}
	
	public static FileTime safeLastModified(Path path, FileTime defaultTime) {
		try {
			return Files.getLastModifiedTime(path);
		}
		catch(IOException ioe) {
			return defaultTime;
		}
	}

	public static long safeSize(Path path) {
		return safeSize(path, 0);
	}
	
	public static long safeSize(Path path, long defaultSize) {
		try {
			return Files.size(path);
		}
		catch(IOException ioe) {
			return defaultSize;
		}
	}
}
