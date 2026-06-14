package uk.co.bithatch.zxbasic.ui.preprocessing;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.resource.Resource;

import uk.co.bithatch.eclipzpp.FileSystemResourceResolver;
import uk.co.bithatch.eclipzpp.GenericPreprocessor;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Builder;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Format;
import uk.co.bithatch.eclipzpp.ui.PPResource;
import uk.co.bithatch.eclipzpp.ui.PPResourceUtil;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicArchitecture;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ZXBasicResource extends PPResource {


	public static GenericPreprocessor.Builder defaultBuilderForProject(IProject project) {
		var pax = ZXBasicPreferencesAccess.get();
		var fs = resourceResolveForProject(project);
		return new GenericPreprocessor.Builder().
				withResourceResolver(fs).
				withFormat(Format.BORIEL).
				withDefines(pax.getDefines(project));
	}

	public static FileSystemResourceResolver resourceResolveForProject(IProject project) {
		var pax = ZXBasicPreferencesAccess.get();
		var fs = new FileSystemResourceResolver.Builder().withIncludeDirs(pax.getAllLibs(project))
				.withWorkingDir(project.getLocation().toFile())
				.withRuntimeDir(pax.getSDK(project).orElseThrow(() -> new IllegalStateException("No SDK configured for project.")).runtime((BorielZXBasicArchitecture)pax.getArchitecture(project))).build();
		return fs;
	}

	@Override
	protected Optional<IFile> getFile(Resource resource) {
		return PPResourceUtil.getFile(resource);
	}

	@Override
	public Builder builder(IProject project) {
		return defaultBuilderForProject(project);
	}
}
