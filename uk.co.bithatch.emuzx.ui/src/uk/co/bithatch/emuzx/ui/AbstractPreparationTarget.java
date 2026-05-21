package uk.co.bithatch.emuzx.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.co.bithatch.bitzx.FileItem;
import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.bitzx.FileSet.Purpose;
import uk.co.bithatch.emuzx.api.IPreparationTarget;

public abstract class AbstractPreparationTarget implements IPreparationTarget {

	protected List<FileItem> flatten(FileSet fileSet) {
		var fls = Arrays.asList(fileSet.files());
		if(fileSet.flatten()) {
			var l = new ArrayList<FileItem>();
			fls.forEach(f -> flatten(l, f));
			return l;
		}
		else {
			return fls;
		}
	}

	protected String resolveDestination(String destFolder, FileSet fileSet) {
		String subfolder;
		if(destFolder.equals("") || fileSet.purpose() == Purpose.BOOT)
			subfolder = fileSet.destination();
		else
			subfolder = destFolder + "/" + fileSet.destination();
		return subfolder;
	}

	protected void flatten(List<FileItem> l, FileItem fls) {
		if(fls.file().isFile())
			l.add(fls);
		else if(fls.file().isDirectory()) {
			for(var f : fls.file().listFiles()) {
				flatten(l, new FileItem(f));
			}
		}
	}
}
