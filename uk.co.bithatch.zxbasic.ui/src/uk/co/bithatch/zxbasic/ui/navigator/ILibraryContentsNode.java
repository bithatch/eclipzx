package uk.co.bithatch.zxbasic.ui.navigator;

import java.io.File;

import org.eclipse.core.resources.IStorage;

public interface ILibraryContentsNode extends IStorage {
	File getFile();
}
