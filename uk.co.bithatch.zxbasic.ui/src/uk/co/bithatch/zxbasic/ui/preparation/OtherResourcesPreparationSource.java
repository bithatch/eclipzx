package uk.co.bithatch.zxbasic.ui.preparation;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_OTHER_FILES;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSource;
import uk.co.bithatch.zxbasic.ui.util.FileSet;
import uk.co.bithatch.zxbasic.ui.util.FileSet.Purpose;

public class OtherResourcesPreparationSource implements IPreparationSource {

	@Override
	public void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException {

		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
		
		/* Other files */
		var processedOtherFiles = ctx.launchConfiguration().getAttribute(PREPARATION_OTHER_FILES, Collections.emptyList()).
				stream().
				map(s -> {
					try {
						return strmgr.performStringSubstitution(s);
					} catch (CoreException e) {
						throw new IllegalStateException(e);
					}
				}).
				toList();
		
		for (var path : processedOtherFiles) {
			var otherFile = new File(path);
			if (!otherFile.isAbsolute()) {
				otherFile = new File(PlatformUI.getWorkbench().getAdapter(IWorkspace.class).getRoot()
						.getLocation().toFile(), otherFile.toString());
			}
			fileSets.add(new FileSet(Purpose.ANCILLARY, otherFile));
		}
		
	}

}
