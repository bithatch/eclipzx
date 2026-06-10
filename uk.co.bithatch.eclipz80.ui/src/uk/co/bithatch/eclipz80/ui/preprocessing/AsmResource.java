package uk.co.bithatch.eclipz80.ui.preprocessing;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.eclipz80.ui.preferences.AsmPreferencesAccess;
import uk.co.bithatch.eclipzpp.FileSystemResourceResolver;
import uk.co.bithatch.eclipzpp.GenericPreprocessor;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Builder;
import uk.co.bithatch.eclipzpp.ui.PPResource;
import uk.co.bithatch.eclipzpp.ui.PPResourceUtil;

public class AsmResource extends PPResource {


	public static GenericPreprocessor.Builder builderForProject(IProject project) {
		var pax = AsmPreferencesAccess.get();
		var fs = new FileSystemResourceResolver.Builder()
				.withIncludePaths(pax.getAllIncludePaths(project))
				.withWorkingDir(project.getLocation().toFile())
				.build();

		return new GenericPreprocessor.Builder().
				withResourceResolver(fs).
				withDefines(pax.getDefines(project));
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
