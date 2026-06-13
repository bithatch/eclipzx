package uk.co.bithatch.eclipz80.ui.preprocessing;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipzpp.FileSystemResourceResolver;
import uk.co.bithatch.eclipzpp.GenericPreprocessor;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Builder;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Format;
import uk.co.bithatch.eclipzpp.ui.PPResource;
import uk.co.bithatch.eclipzpp.ui.PPResourceUtil;

public class AsmResource extends PPResource {

	public static GenericPreprocessor.Builder builderForProject(IProject project) {
		var lang = LanguageSystem.languageSystem(project);
		
		var bldr = new FileSystemResourceResolver.Builder()
				.withIncludes(lang.findIncludeSourcePaths(project, IResource.DEPTH_ONE))
				.withWorkingDir(project.getLocation().toFile());
		
		lang.findRuntimeDir(project).ifPresent(bldr::withRuntimeDir);
		
		var fs = bldr
				.build();

		return new GenericPreprocessor.Builder().
				withResourceResolver(fs).
				withFormat(Format.ASM).
				withDefines(lang.findDefines(project));
	}

	@Override
	protected Optional<IFile> getFile(Resource resource) {
		return PPResourceUtil.getFile(resource);
	}

	@Override
	public Builder builder(IProject project) {
		return builderForProject(project);
	}
}
