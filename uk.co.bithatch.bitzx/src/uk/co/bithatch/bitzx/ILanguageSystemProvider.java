package uk.co.bithatch.bitzx;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

public interface ILanguageSystemProvider {

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
			if (a.name().equals(archName))
				return a;
		}
		if (!archs.isEmpty()) {
			return archs.get(0);
		}
		throw new IllegalStateException("Could not get any architectures for this project!");
	}

	boolean isCompatible(IResource resource);

	File prepareForLaunch(IOutputFormat fmt, IFile file, ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException;

	boolean isLaunchable(IResource res);
}
