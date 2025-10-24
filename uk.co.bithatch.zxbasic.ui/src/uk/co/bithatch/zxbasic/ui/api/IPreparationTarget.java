package uk.co.bithatch.zxbasic.ui.api;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import uk.co.bithatch.zxbasic.ui.util.FileSet;

public interface IPreparationTarget {
	String init(IPreparationContext prepCtx) throws CoreException;
	
	IStatus prepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException;
	
	void cleanUp();
}
