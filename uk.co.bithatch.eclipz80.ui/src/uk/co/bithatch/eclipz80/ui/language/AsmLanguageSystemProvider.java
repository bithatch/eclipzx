package uk.co.bithatch.eclipz80.ui.language;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

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
import uk.co.bithatch.eclipz80.ui.builder.AsmBuilder;
import uk.co.bithatch.eclipz80.ui.builder.AsmNature;
import uk.co.bithatch.eclipz80.ui.preferences.AsmPreferencesAccess;

public class AsmLanguageSystemProvider implements ILanguageSystemProvider {

	@Override
	public List<IOutputFormat> outputFormats(IResource resource) {
		return Arrays.asList(AsmOutputFormat.values());
	}

	@Override
	public List<IArchitecture> architectures(IResource resource) {
		return Arrays.asList(AsmArchitecture.values());
	}

	@Override
	public List<? extends IArchitecture> architectures(IProject project, String sdkName) {
		return Arrays.asList(AsmArchitecture.values());
	}

	@Override
	public File prepareForLaunch(IOutputFormat fmt, IFile file, ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// TODO
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public LanguageSystemPreferencesAccess preferenceAccess() {
		return AsmPreferencesAccess.get();
	}

	@Override
	public boolean isCompatible(IResource resource) {
		try {
			return resource.getProject().hasNature(AsmNature.NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	@Override
	public boolean isLaunchable(IResource res) {
		if (res instanceof IFile file) {
			return FileNames.hasExtensions(file.getName(), AsmBuilder.EXTENSIONS);
		} else if (res instanceof IContainer container) {
			return !container.getFullPath()
					.equals(AsmPreferencesAccess.get().getOutputFolder(res.getProject()).getFullPath());
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
			public NavigableMap<Integer, SourceLocation> getAddressToLineMap() {
				return Collections.emptyNavigableMap();
			}

			@Override
			public Map<SourceLocation, Integer> getLineToAddressMap() {
				return Collections.emptyMap();
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
	public Set<String> findIncludeSourcePaths(IFile file) {
//		var allLibs = ZXBasicPreferencesAccess.get().getAllLibURIs(file.getProject());
//		return allLibs.stream().map(p -> p.toString()).collect(Collectors.toSet());
		return Collections.emptySet();
	}

}
