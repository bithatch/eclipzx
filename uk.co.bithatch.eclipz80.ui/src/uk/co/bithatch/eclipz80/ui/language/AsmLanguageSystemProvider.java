package uk.co.bithatch.eclipz80.ui.language;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import org.eclipse.emf.common.util.URI;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.ISourceAdressMap;
import uk.co.bithatch.bitzx.LanguageSystemPreferencesAccess;
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
	public Path prepareForInternalLaunch(IOutputFormat fmt, IFile file, ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
		return AsmBuilder.prepareForEmulatorLaunch(fmt, file, AsmBuilder.prepareForGenericLaunch(file, mode));
		
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
		return new String[] { "asm" };
	}

	@Override
	public ISourceAdressMap createSourceAddressMap(Path file) {
		try {
			return new AsmSourceAddressMap(file);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public Set<String> findIncludeSourcePaths(IFile file) {
		return AsmPreferencesAccess.get().getProjectReferencesURIs(file.getProject()).stream().map(URI::toString)
				.collect(Collectors.toSet());
	}

	@Override
	public Map<String, String> findDefines(IFile baseFile) {
		return AsmPreferencesAccess.get().getDefinesMap(baseFile.getProject());
	}

}
