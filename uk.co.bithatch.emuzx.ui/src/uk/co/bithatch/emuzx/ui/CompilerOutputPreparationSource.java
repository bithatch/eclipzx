package uk.co.bithatch.emuzx.ui;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.bitzx.FileSet.Purpose;
import uk.co.bithatch.emuzx.api.IPreparationContext;
import uk.co.bithatch.emuzx.api.IPreparationSource;

public class CompilerOutputPreparationSource implements IPreparationSource {

	@Override
	public void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException {
		fileSets.add(new FileSet(Purpose.PROGRAM, ctx.binaryFile()));
	}

}
