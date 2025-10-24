package uk.co.bithatch.zxbasic.ui.api;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import uk.co.bithatch.zxbasic.ui.util.FileSet;

public interface IPreparationSource {
	void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException;
}
