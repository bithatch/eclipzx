package uk.co.bithatch.zxbasic.ui.contentassist;

import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.antlr.DelegatingContentAssistContextFactory;
import org.eclipse.xtext.util.TextRegion;

import com.google.inject.Inject;
import com.google.inject.Provider;
 
import uk.co.bithatch.eclipzpp.ui.PPPreprocessingSupport;
import uk.co.bithatch.eclipzpp.ui.PPResource;
import uk.co.bithatch.zxbasic.ui.preprocessing.ZXBasicResource;

public class ZXBasicPreprocessingContentAssistContextFactory extends DelegatingContentAssistContextFactory {
	@Inject
	private Provider<PreprocessingStatefulFactory> statefulFactoryProvider;

	@Override
	public Provider<? extends StatefulFactory> getStatefulFactoryProvider() {
		return statefulFactoryProvider;
	}

	public static class PreprocessingStatefulFactory extends StatefulFactory {
		private static final ILog LOG = ILog.of(PreprocessingStatefulFactory.class);

		@Override
		protected ContentAssistContext[] doCreateContexts(int offset) {
			var selection = (ITextSelection) viewer.getSelectionProvider().getSelection();
			var region = new TextRegion(selection.getOffset(), selection.getLength());
			var text = viewer.getDocument().get();
			text = preprocessForContentAssist(resource, text);

			var contexts = getDelegate().create(text, region, offset, resource);
			var result = new ContentAssistContext[contexts.length];
			for (var i = 0; i < contexts.length; i++) {
				result[i] = convert(contexts[i]).toContext();
			}
			return result;
		}

		private String preprocessForContentAssist(XtextResource resource, String text) {
			if (!(resource instanceof PPResource ppResource)) {
				return text;
			}
			var fileOr = ppResource.getFile();
			if (fileOr.isEmpty()) {
				return text;
			}
			try {
				var map = ppResource.map();
				map.clear();
				return PPPreprocessingSupport.preprocess(
						ZXBasicResource.defaultBuilderForProject(fileOr.get().getProject()),
						ppResource,
						fileOr.get(),
						map,
						LOG,
						text);
			} catch (IOException ioe) {
				LOG.warn("Failed to preprocess document for content assist. Falling back to raw text.", ioe);
				return text;
			}
		}
	}
}
