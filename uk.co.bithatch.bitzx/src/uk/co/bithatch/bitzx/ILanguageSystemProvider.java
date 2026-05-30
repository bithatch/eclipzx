package uk.co.bithatch.bitzx;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

public interface ILanguageSystemProvider {
	
	ISourceAdressMap createSourceAddressMap(Path file);

	List<? extends IArchitecture> architectures(IResource resource);

	default List<IOutputFormat> outputFormats(IResource resource) {
		var l = new LinkedHashSet<IOutputFormat>();
		for(var arch : architectures(resource)) {
			l.addAll(arch.supportedFormats());
		}
		return l.stream().toList();
	}

	default IArchitecture architectureOrDefault(IProject project, String archName) {
		var archs = architectures(project);
		for (var a : archs) {
			if (a.name().equalsIgnoreCase(archName))
				return a;
		}
		if (!archs.isEmpty()) {
			return archs.get(0);
		}
		throw new IllegalStateException("Could not get any architectures for this project!");
	}
	
	LanguageSystemPreferencesAccess preferenceAccess();

	boolean isCompatible(IResource resource);

	Path prepareForInternalLaunch(IOutputFormat fmt, IFile file, ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException;

	boolean isLaunchable(IResource res);
	
	String[] sourceFileExtensions();

	List<? extends IArchitecture> architectures(IProject project, String sdkName);
	
	Set<String> findIncludeSourcePaths(IFile baseFile);
	
	Map<String, String> findDefines(IFile baseFile);
}
