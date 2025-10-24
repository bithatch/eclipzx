package uk.co.bithatch.zxbasic.ui.preparation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.co.bithatch.zxbasic.ui.api.IPreparationTarget;
import uk.co.bithatch.zxbasic.ui.util.FileItem;
import uk.co.bithatch.zxbasic.ui.util.FileSet;

public abstract class AbstractPreparationTarget implements IPreparationTarget {

	protected List<FileItem> flatten(FileSet fileSet) {
		var fls = Arrays.asList(fileSet.files());
		if(fileSet.flatten()) {
			return fls;
		}
		else {
			var l = new ArrayList<FileItem>();
			fls.forEach(f -> flatten(l, f));
			return l;
		}
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
