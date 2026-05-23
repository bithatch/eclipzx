package uk.co.bithatch.emuzx.api;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import uk.co.bithatch.bitzx.FileSet;

public interface IPreparationTarget {
	String init(IPreparationContext prepCtx) throws CoreException;
	
	IStatus prepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException;
	
	void cleanUp();

	default void preparationDone() {}

	/**
	 * Returns the absolute filesystem path to the preparation output.
	 * For directory-based targets this is the output directory; for FAT
	 * image targets this is the {@code .img} file.  Only valid after
	 * {@link #init(IPreparationContext)} has been called.
	 *
	 * @return absolute path, or {@code null} if not applicable
	 */
	default String outputPath() { return null; }
}
