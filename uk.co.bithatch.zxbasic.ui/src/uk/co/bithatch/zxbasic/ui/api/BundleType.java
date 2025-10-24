package uk.co.bithatch.zxbasic.ui.api;

import uk.co.bithatch.bitzx.FileNames;

public enum BundleType {
	BY_FILE_TYPE, MMU, FILE, EXTRA;
	
	public final static String[] SUPPORTED_FILE_TYPES = { "scr", "slr", "shr", "shc", "bmp" };
	
	public boolean supportsBankAddress() {
		switch(this) {
		case MMU:
		case FILE:
			return true;
		default:
			return false;
		}
	}
	
	public String description() {
		switch(this) {
		case BY_FILE_TYPE:
			return "By File Type";
		case MMU:
			return "MMU";
		case FILE:
			return "As raw file";
		case EXTRA:
			return "As extra file";
		default:
			return "Unknown";
		}
	}

	public static  boolean isSupportedFileType(String fileExtension) {
		return FileNames.hasExtensions("." +fileExtension, SUPPORTED_FILE_TYPES);
	}

	public static BundleType defaultForFilename(String name) {
		if(FileNames.hasExtensions(name, SUPPORTED_FILE_TYPES)) {
			return BundleType.BY_FILE_TYPE;
		}
		else {
			return BundleType.MMU;
		}
	}
}