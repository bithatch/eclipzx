package uk.co.bithatch.zxbasic.ui.preprocessing;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.DefaultLocationInFileProvider;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.util.ITextRegionWithLineInformation;
import org.eclipse.xtext.util.TextRegion;
import org.eclipse.xtext.util.TextRegionWithLineInformation;

import uk.co.bithatch.zxbasic.scoping.SourceMapRegistry;

public class ZXBasicLocationInFileProvider extends DefaultLocationInFileProvider {

	@Override
	public ITextRegion getFullTextRegion(EObject obj) {
		var preprocessedRegion = (ITextRegionWithLineInformation) super.getFullTextRegion(obj);
		return mapRegion(obj, preprocessedRegion);
	}

	protected ITextRegion mapRegion(EObject obj, ITextRegionWithLineInformation preprocessedRegion) {
//		var map = SourceMapRegistry.get(obj.eResource());
//		if (map.isEmpty())
//			return preprocessedRegion;
//
//		System.out.println("preprocessed region preprocessedRegion  " + preprocessedRegion);
//
//		var startOffset = preprocessedRegion.getOffset();
//		var startLine = preprocessedRegion.getLineNumber();
//		var preprocessedLines = preprocessedRegion.getEndLineNumber() - preprocessedRegion.getLineNumber();
//
//		// Break region across segments
//		var matching = map.get().findSegments(startOffset, preprocessedRegion.getLength());
//
//		if (matching.isEmpty())
//			return preprocessedRegion;
//
//		// Map only the first segment for now
////		ITextRegionWithLineInformation newRegion = null;
//
//
//		for (var seg : matching) {
//			System.out.println("    -> " + seg);
//
//			if (seg.getOriginalLength() <= 0 || seg.getUri() == null) {
//				System.out.println(" skipping because empty region");
//				return TextRegion.EMPTY_REGION;
//			}
//
////			if (startOffset >= seg.getPreprocessedOffset()
////					&& startOffset < seg.getPreprocessedOffset() + seg.getPreprocessedLength()) {
//				
//				var delta = startOffset - seg.getPreprocessedOffset();
//				var lineDelta = startLine - seg.getPreprocessedLine();
//
//				var originalOffset = seg.getOriginalOffset() + delta;
//				var originalLine = seg.getOriginalLine() + lineDelta;
//				
//				var originalLength = Math.min(preprocessedRegion.getLength(), seg.getPreprocessedLength() - delta);
//				var originalLines = Math.min(preprocessedLines, seg.getPreprocessedLine() - lineDelta);
//
//				System.out.println("    maps to " + originalOffset + "," + originalLength + " [" + originalLine + " -> "
//						+ (originalLine + originalLines) + "] " + "(" + originalLines + ")");
//				System.out.println("    delta " + delta + " / " + lineDelta);
//
//				return new TextRegionWithLineInformation(originalOffset, originalLength, originalLine,
//						originalLine + originalLines);
////			}
//		}

//		if (newRegion == null) {
			return preprocessedRegion;
//		}
//
//		return newRegion;
	}

	@Override
	public ITextRegion getSignificantTextRegion(EObject obj) {
		var preprocessedRegion = (ITextRegionWithLineInformation) super.getFullTextRegion(obj);
		return mapRegion(obj, preprocessedRegion);
	}
}
