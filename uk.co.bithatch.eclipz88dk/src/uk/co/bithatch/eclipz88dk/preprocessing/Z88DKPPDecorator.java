package uk.co.bithatch.eclipz88dk.preprocessing;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipz88dk.Z88DKLanguageSystemProvider;
import uk.co.bithatch.eclipzpp.FileSystemResourceResolver;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Builder;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Format;
import uk.co.bithatch.eclipzpp.ui.PPResource;
import uk.co.bithatch.eclipzpp.ui.PPResourcePreprocessorDecorator;

public class Z88DKPPDecorator implements PPResourcePreprocessorDecorator {

	@Override
	public void decorate(Builder bldr, PPResource resource, IFile file) {
		if (file.getFileExtension() != null
				&& Set.of(Z88DKLanguageSystemProvider.SOURCE_FILE_EXTENSIONS).contains(file.getFileExtension().toLowerCase())
				&& LanguageSystem.languageSystem(file) instanceof Z88DKLanguageSystemProvider zlang) {
			bldr.withFormat(Format.Z88DK);
			bldr.withDefines(zlang.findDefines(file));
			bldr.withResourceResolver(resourceResolveForProject(file.getProject()));
		}
	}


	public static FileSystemResourceResolver resourceResolveForProject(IProject project) {
		var zlang = LanguageSystem.languageSystem(project);
		var sdk = zlang.findRuntimeDir(project);
		var paths = zlang.findIncludeSourcePaths(project);
		if(sdk.isPresent()) {
			paths = Stream.concat(paths.stream(), Stream.of(sdk.get())).collect(Collectors.toSet());
		}
		var bldr = new FileSystemResourceResolver.Builder()
				.withIncludes(paths)
				.withWorkingDir(project.getLocation().toFile());
//		sdk.ifPresent(bldr::withRuntimeDir);
		return bldr.build();
	}
}
