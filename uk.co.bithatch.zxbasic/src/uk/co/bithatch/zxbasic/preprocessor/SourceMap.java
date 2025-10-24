package uk.co.bithatch.zxbasic.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceMap {

	public final static class Segment {
		private final int originalOffset;
		private final int originalLength;
		private final int originalLine;
		private final int originalLines;
		private final int preprocessedOffset;
		private final int preprocessedLength;
		private final int preprocessedLine;
		private final int preprocessedLines;
		private final String uri;

		public Segment(
				int originalOffset, 
				int originalLine, 
				int originalLength, 
				int originalLines, 
				int preprocessedOffset, 
				int preprocessedLine, 
				int preprocessedLength, 
				int preprocessedLines,
				String uri) {
			
			this.originalOffset = originalOffset;
			this.originalLength = originalLength;
			this.preprocessedOffset = preprocessedOffset;
			this.preprocessedLength = preprocessedLength;
			this.preprocessedLine = preprocessedLine;
			this.preprocessedLines = preprocessedLines;
			this.originalLine = originalLine;
			this.originalLines  = originalLines;
			this.uri = uri;
			
		}

		public int getOriginalLine() {
			return originalLine;
		}

		public int getOriginalLines() {
			return originalLines;
		}

		public int getPreprocessedLine() {
			return preprocessedLine;
		}

		public int getPreprocessedLines() {
			return preprocessedLines;
		}

		public String getUri() {
			return uri;
		}

		public boolean spansPreprocessed(int offset, int length) {
			return containsPreprocessedOffset(offset) || ( length > 0 && containsPreprocessedOffset(offset + length - 1));
		}

		public boolean containsPreprocessedOffset(int offset) {
			return offset >= preprocessedOffset && offset < (preprocessedOffset + preprocessedLength);
		}

		public int getOriginalOffset() {
			return originalOffset;
		}

		public int getOriginalLength() {
			return originalLength;
		}

		public int getPreprocessedOffset() {
			return preprocessedOffset;
		}

		public int getPreprocessedLength() {
			return preprocessedLength;
		}

		@Override
		public String toString() {
			return "Segment [originalOffset=" + originalOffset + ", originalLength=" + originalLength
					+ ", originalLine=" + originalLine + ", originalLines=" + originalLines + ", preprocessedOffset="
					+ preprocessedOffset + ", preprocessedLength=" + preprocessedLength + ", preprocessedLine="
					+ preprocessedLine + ", preprocessedLines=" + preprocessedLines + ", uri=" + uri + "]";
		}


	}

	private final List<Segment> segments = new ArrayList<>();
	private final Map<String, String> defines = new HashMap<>();

	public void addSegment(Segment segment) {
    	System.out.println("Adding segment " + segment);
		segments.add(segment);
	}

//	public Optional<Segment> getSegmentForPreprocessedOffset(int offset) {
//		for (Segment s : segments) {
//			if (s.containsPreprocessedOffset(offset)) {
//				return Optional.of(s);
//			}
//		}
//		return Optional.empty();
//	}
	
	public void clear() {
		segments.clear();
		defines.clear();
	}
	
	public Map<String, String> defines() {
		return defines;
	}

	public List<Segment> findSegments(int offset, int length) {
		return segments.stream().filter(s -> s.spansPreprocessed(offset, length)).toList();
	}

//	public Optional<ITextRegionWithLineInformation> mapPreprocessedRegionToOriginal(ITextRegionWithLineInformation preprocessed) {
//		for (Segment seg : segments) {
//			if (seg.containsPreprocessedOffset(preprocessed.getOffset())) {
//				int delta = preprocessed.getOffset() - seg.preprocessedOffset;
//				int lineDelta = preprocessed.getLineNumber() - seg.preprocessedLine;
//				return Optional.of(new TextRegionWithLineInformation(
//						seg.originalOffset + delta, 
//						preprocessed.getLength(),
//						seg.originalLine + lineDelta,
//						seg.originalLine + lineDelta + seg.preprocessedLines));
//			}
//		}
//		// fallback
//		return Optional.empty();
//	}
}
