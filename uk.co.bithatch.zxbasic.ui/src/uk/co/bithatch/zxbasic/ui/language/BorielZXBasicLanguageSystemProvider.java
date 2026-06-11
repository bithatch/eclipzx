package uk.co.bithatch.zxbasic.ui.language;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.ISourceAdressMap;
import uk.co.bithatch.bitzx.LanguageSystemPreferencesAccess;
import uk.co.bithatch.bitzx.SourceLocation;
import uk.co.bithatch.emuzx.DefaultPreparationContext;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicNature;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

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
	public List<? extends IArchitecture> architectures(IProject project, String sdkName) {
		return Arrays.asList(BorielZXBasicArchitecture.values());
	}

	@Override
	public Path prepareForInternalLaunch(IOutputFormat fmt, IFile file, ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		var ctx = new DefaultPreparationContext(configuration, file, fmt);
		ZXBasicBuilder.compileForLaunch(ctx, mode, ZXBasicBuilder.DEFAULT_REPORTER);
		return ctx.launchFile();
	}

	@Override
	public LanguageSystemPreferencesAccess preferenceAccess() {
		return ZXBasicPreferencesAccess.get();
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

	@Override
	public String[] sourceFileExtensions() {
		return new String[] { "bas", "zxbasic" };
	}

	@Override
	public ISourceAdressMap createSourceAddressMap(Path file) {
		// TODO
		return new ISourceAdressMap() {

			@Override
			public int getAddress(String fileName, int line) {
				return 0;
			}

			@Override
			public SourceLocation getSourceLocation(int address) {
				return null;
			}

			@Override
			public int getSymbolAddress(String symbolName) {
				return 0;
			}

			@Override
			public boolean hasDebugInfo() {
				return false;
			}
		};
	}

	@Override
	public Set<String> findIncludeSourcePaths(IResource file) {
		var allLibs = ZXBasicPreferencesAccess.get().getAllLibURIs(file.getProject());
		return allLibs.stream().map(p -> p.toString()).collect(Collectors.toSet());
	}

	@Override
	public Map<String, String> findDefines(IResource baseFile) {
		return ZXBasicPreferencesAccess.get().getDefines(baseFile.getProject());
	}

	@Override
	public Optional<String> findRuntimeDir(IResource baseFile) {
		var pax = ZXBasicPreferencesAccess.get();
		return pax.getSDK(baseFile.getProject()).map(sdk -> sdk.runtime(pax.getArchitecture(baseFile.getProject())).getAbsolutePath());
	}

}
