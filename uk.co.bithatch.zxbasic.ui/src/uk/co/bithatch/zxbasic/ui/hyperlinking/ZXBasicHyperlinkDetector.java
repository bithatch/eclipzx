package uk.co.bithatch.zxbasic.ui.hyperlinking;

import java.nio.file.Path;

import com.google.inject.Inject;

import uk.co.bithatch.eclipzpp.ui.AbstractPPHyperlinkDetector;
import uk.co.bithatch.eclipzpp.ui.PPResource;
import uk.co.bithatch.zxbasic.ZxBasicIncludeSource;

public class ZXBasicHyperlinkDetector extends AbstractPPHyperlinkDetector {

	@Inject(optional = true)
	private ZxBasicIncludeSource includeSource;
 
	@Override
	protected Path findInclude(PPResource resource, String filename) {
		return includeSource.find(resource, filename);
	}

}
