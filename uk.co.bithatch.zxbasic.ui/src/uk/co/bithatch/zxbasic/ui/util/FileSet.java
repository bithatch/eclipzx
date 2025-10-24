package uk.co.bithatch.zxbasic.ui.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public record FileSet(Purpose purpose, String destination, boolean flatten, FileItem... files) {
	
	public enum Purpose {
		PROGRAM, BOOT, ANCILLARY
	}
	
	public FileSet(Purpose purpose, File... files) {
		this(purpose, Arrays.asList(files).stream().map(FileItem::new).toList().toArray(new FileItem[0]));
	}
	
	public FileSet(Purpose purpose, FileItem... files) {
		this(purpose, "", false, files);
	}

	public FileSet(Purpose purpose, List<FileItem> files) {
		this(purpose, "", true, files);
	}
	
	public FileSet(Purpose purpose, String destination, boolean flatten, List<FileItem> files) {
		this(purpose, destination, flatten, files.toArray(new FileItem[0]));
	}
}
