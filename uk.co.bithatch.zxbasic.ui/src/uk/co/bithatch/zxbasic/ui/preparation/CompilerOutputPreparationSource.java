package uk.co.bithatch.zxbasic.ui.preparation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSource;
import uk.co.bithatch.zxbasic.ui.util.FileSet;
import uk.co.bithatch.zxbasic.ui.util.FileSet.Purpose;

public class CompilerOutputPreparationSource implements IPreparationSource {

	@Override
	public void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException {
		fileSets.add(new FileSet(Purpose.PROGRAM, ctx.binaryFile()));
	}

}
