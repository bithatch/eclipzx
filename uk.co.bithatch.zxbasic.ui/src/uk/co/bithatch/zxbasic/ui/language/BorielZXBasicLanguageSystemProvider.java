package uk.co.bithatch.zxbasic.ui.language;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicNature;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.preparation.DefaultPreparationContext;

public class BorielZXBasicLanguageSystemProvider implements ILanguageSystemProvider {

	@Override
	public List<IOutputFormat> outputFormats(IResource resource) {
		return Arrays.asList(BorielZXBasicOutputFormat.values());
	}

	@Override
	public List<IArchitecture> architectures(IResource resource) {
		return Arrays.asList(BorielZXBasicArchitecture.values());
	}

	@Override
	public File prepareForLaunch(IOutputFormat fmt, IFile file, ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		var ctx = new DefaultPreparationContext(configuration, file, fmt);
		ZXBasicBuilder.compileForLaunch(ctx, mode, ZXBasicBuilder.DEFAULT_REPORTER);
		return ctx.binaryFile();
	}

	@Override
	public boolean isCompatible(IResource resource) {
		try {
			return resource.getProject().hasNature(ZXBasicNature.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	@Override
	public boolean isLaunchable(IResource res) {
		if (res instanceof IFile file) {
			return FileNames.hasExtensions(file.getName(), ZXBasicBuilder.EXTENSIONS);
		} else if (res instanceof IContainer container) {
			return !container.getFullPath()
					.equals(ZXBasicPreferencesAccess.get().getOutputFolder(res.getProject()).getFullPath());
		} else {
			return false;
		}
	}

}
