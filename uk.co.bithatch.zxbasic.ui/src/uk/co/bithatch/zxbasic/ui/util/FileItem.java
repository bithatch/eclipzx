package uk.co.bithatch.zxbasic.ui.util;

import java.io.File;

public record FileItem(File file, String targetName) {
	public FileItem(File file) {
		this(file, file.getName());
	}
}
