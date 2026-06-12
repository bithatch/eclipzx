package uk.co.bithatch.zxbasic.ui.preprocessing;

import java.util.Set;

import org.eclipse.core.resources.IFile;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Builder;
import uk.co.bithatch.eclipzpp.GenericPreprocessor.Format;
import uk.co.bithatch.eclipzpp.ui.PPResource;
import uk.co.bithatch.eclipzpp.ui.PPResourcePreprocessorDecorator;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicLanguageSystemProvider;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ZXBasicPPDecorator implements PPResourcePreprocessorDecorator {

	@Override
	public void decorate(Builder bldr, PPResource resource, IFile file) {
		if (file.getFileExtension() != null
				&& Set.of(ZXBasicBuilder.EXTENSIONS).contains(file.getFileExtension().toLowerCase())
				&& LanguageSystem.languageSystem(file) instanceof BorielZXBasicLanguageSystemProvider) {

			var pax = ZXBasicPreferencesAccess.get();
			bldr.withFormat(Format.BORIEL);
			bldr.withDefines(pax.getDefines(file.getProject()));
			bldr.withResourceResolver(ZXBasicResource.resourceResolveForProject(file.getProject()));
		}
	}

}
