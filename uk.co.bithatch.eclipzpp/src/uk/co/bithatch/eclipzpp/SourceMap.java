package uk.co.bithatch.eclipzpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;

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
		
		public boolean originalLine(int original) {
			return original >= originalLine && original < originalLine + Math.max(1, originalLines);
		}
		
		public boolean preprocessedLine(int preprocessed) {
			return preprocessed >= preprocessedLine && preprocessed < preprocessedLine + Math.max(1, preprocessedLines);
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
	private final Map<String, DefineDef> defines = new HashMap<>();
	private final Map<Integer, String> hiddenLines = new HashMap<>();

	public void addSegment(Segment segment) {
		segments.add(segment);
	}

	public void clear() {
		segments.clear();
		defines.clear();
		hiddenLines.clear();
	}
	
	public List<Segment> segments() {
		return segments;
	}
	
	public Map<String, DefineDef> defines() {
		return defines;
	}
	
	public Map<Integer, String> hiddenLines() {
		return hiddenLines;
	}
	
	public int translatePreprocessedToOriginalLine(int preprocessedLine, String uri) {
		for(var seg : segments) {
			if(Objects.equal(uri, seg.uri) && seg.preprocessedLine(preprocessedLine)) {
				var delta = preprocessedLine - seg.getPreprocessedLine() - 1;
				return seg.getOriginalLine() + delta;
			}
		}
		return preprocessedLine;
	}

	public List<Segment> findSegments(int offset, int length) {
		return segments.stream().filter(s -> s.spansPreprocessed(offset, length)).toList();
	}

}
