package uk.co.bithatch.jspeccy.views;

import java.io.File;

public class Cartridge {
	private int drive;
	private File file;
	private boolean readOnly;
	private boolean modified;
	private boolean inserted;

	public Cartridge(int drive) {
		this.drive = drive;
	}

	public int getDrive() {
		return drive;
	}

	public File getFile() {
		return file;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isModified() {
		return modified;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public boolean isInserted() {
		return inserted;
	}

	public void setInserted(boolean inserted) {
		this.inserted = inserted;
	}
}