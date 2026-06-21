package uk.co.bithatch.eclipzpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Objects;

public class SourceMap {

	public final static class Segment {
		private final int originalLine;
		private final int originalLines;
		private final int preprocessedLine;
		private final int preprocessedLines;
		private final String uri;

		public Segment(
				int originalLine, 
				int originalLines, 
				int preprocessedLine, 
				int preprocessedLines,
				String uri) {
			
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

		public boolean originalLine(int original) {
			return original >= originalLine && original < originalLine + Math.max(1, originalLines);
		}
		
		public boolean preprocessedLine(int preprocessed) {
			return preprocessed >= preprocessedLine && preprocessed < preprocessedLine + Math.max(1, preprocessedLines);
		}

		@Override
		public String toString() {
			return "Segment [originalLine=" + originalLine + ", originalLines=" + originalLines + ", preprocessedLine="
					+ preprocessedLine + ", preprocessedLines=" + preprocessedLines + ", uri=" + uri + "]";
		}


	}

	public record TranslatedLocation(String uri, int originalLine, int preprocessedLine) {
	}

	private final List<Segment> segments = new ArrayList<>();
	private final Map<String, DefineDef> defines = new HashMap<>();
	private final Map<Integer, String> hiddenOffsets = new HashMap<>();
	private final Set<String> inits = new LinkedHashSet<>();
	public void addSegment(Segment segment) {
		if(!segments.isEmpty()) {
			var previous = segments.get(segments.size() - 1);
			if(segment.getPreprocessedLine() < previous.getPreprocessedLine()) {
				throw new IllegalArgumentException("Segments must be added in encounter order.");
			}
		}
		segments.add(segment);
	}

	public void clear() {
		segments.clear();
		defines.clear();
		hiddenOffsets.clear();
		inits.clear();
	}

	
	public Set<String> inits() {
		return inits;
	}
	
	public List<Segment> segments() {
		return segments;
	}
	
	public Map<String, DefineDef> defines() {
		return defines;
	}
	
	public Map<Integer, String> hiddenOffsets() {
		return hiddenOffsets;
	}
	
	public String closestHiddenOffset(int offset) {
		var closest = -1;
		for(var line : hiddenOffsets.keySet()) {
			if(line <= offset && line > closest) {
				closest = line;
			}
		}
		return closest == -1 ? null : hiddenOffsets.get(closest);
	}
	
	/**
	 * Translate a zero-based global preprocessed line index to source provenance.
	 * Returned uri may be null for root source.
	 */
	public Optional<TranslatedLocation> translatePreprocessedToOriginal(int preprocessedLine) {
		for(var seg : segments) {
			// Count prior segments for the uri of the matching segment; segment order is encounter order.
			if(seg.preprocessedLine(preprocessedLine)) {
				var uri = seg.getUri();
				int prior = 0;
				for(var prev : segments) {
					if(prev == seg) {
						break;
					}
					if(Objects.equal(prev.getUri(), uri)) {
						prior++;
					}
				}
				var delta = preprocessedLine - seg.getPreprocessedLine();
				int originalLine = seg.getOriginalLine() + delta - prior;
				return Optional.of(new TranslatedLocation(uri, originalLine, preprocessedLine));
			}
		}
		return Optional.empty();
	}

	/**
	 * Translate a zero-based global preprocessed line index to a zero-based line
	 * index in the original source identified by {@code uri}.
	 */
	public int translatePreprocessedToOriginalLine(int preprocessedLine, String uri) {
		var translated = translatePreprocessedToOriginal(preprocessedLine);
		if(translated.isEmpty()) {
			return preprocessedLine;
		}
		var loc = translated.get();
		return Objects.equal(uri, loc.uri()) ? loc.originalLine() : preprocessedLine;
	}

	/**
	 * Diagnostic helper for translation issues. Returns a readable trace of the
	 * segment scan and the exact arithmetic used for the selected segment.
	 */
	public String explainPreprocessedToOriginalLine(int preprocessedLine, String uri) {
		var out = new StringBuilder();
		out.append("translatePreprocessedToOriginalLine(preprocessedLine=")
				.append(preprocessedLine)
				.append(", uri=")
				.append(uri)
				.append(")")
				.append(System.lineSeparator());

		for(int i = 0; i < segments.size(); i++) {
			var seg = segments.get(i);
			var sameUri = isSameUri(uri, seg);
			var inRange = seg.preprocessedLine(preprocessedLine);
			out.append("[").append(i).append("] ").append(seg)
					.append(" sameUri=").append(sameUri)
					.append(" inRange=").append(inRange)
					.append(System.lineSeparator());

			if(sameUri && inRange) {
				int prior = 0;
				for(int p = 0; p < i; p++) {
					if(Objects.equal(segments.get(p).getUri(), seg.getUri())) {
						prior++;
					}
				}
				var delta = preprocessedLine - seg.getPreprocessedLine();
				var result = seg.getOriginalLine() + delta - prior;
				out.append("  MATCH: delta = ")
						.append(preprocessedLine)
						.append(" - ")
						.append(seg.getPreprocessedLine())
						.append(" = ")
						.append(delta)
						.append(System.lineSeparator());
				out.append("  ADJUST: subtract prior same-uri segments = ")
						.append(prior)
						.append(System.lineSeparator());
				out.append("  RESULT: originalLine = ")
						.append(seg.getOriginalLine())
						.append(" + ")
						.append(delta)
						.append(" - ")
						.append(prior)
						.append(" = ")
						.append(result)
						.append(System.lineSeparator());
				return out.toString();
			}
		}

		out.append("  NO MATCH: returns input line ").append(preprocessedLine).append(System.lineSeparator());
		return out.toString();
	}

	public boolean isSameUri(String uri, Segment seg) {
		return Objects.equal(uri, seg.uri);
	}

}