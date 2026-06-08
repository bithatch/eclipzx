package uk.co.bithatch.eclipzpp.preprocessor;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;

import uk.co.bithatch.eclipzpp.pp.BinaryExpr;
import uk.co.bithatch.eclipzpp.pp.IntegralLiteral;
import uk.co.bithatch.eclipzpp.pp.PPAssert;
import uk.co.bithatch.eclipzpp.pp.PPDefine;
import uk.co.bithatch.eclipzpp.pp.PPDefineMacro;
import uk.co.bithatch.eclipzpp.pp.PPDirective;
import uk.co.bithatch.eclipzpp.pp.PPExpression;
import uk.co.bithatch.eclipzpp.pp.PPIf;
import uk.co.bithatch.eclipzpp.pp.PPIfDef;
import uk.co.bithatch.eclipzpp.pp.PPIfNDef;
import uk.co.bithatch.eclipzpp.pp.PPInclude;
import uk.co.bithatch.eclipzpp.pp.PPIndirect;
import uk.co.bithatch.eclipzpp.pp.PPNotExpr;
import uk.co.bithatch.eclipzpp.pp.PPProgram;
import uk.co.bithatch.eclipzpp.pp.PPSignedExpr;
import uk.co.bithatch.eclipzpp.pp.PPUndef;
import uk.co.bithatch.eclipzpp.pp.PPUndefine;
import uk.co.bithatch.eclipzpp.pp.RawTextBlock;
import uk.co.bithatch.eclipzpp.pp.StringLiteral;
import uk.co.bithatch.eclipzpp.pp.SymbolLiteral;

public class PP {
	
	public interface Results {
		Path mapFile();
	}
	
	/**
	 * Callback for non-fatal warnings emitted during assembly.
	 */
	@FunctionalInterface
	public interface WarningCallback {
		void warn(String filename, int line, String warning);
	}

	/**
	 * Thrown when the assembler encounters a fatal error (e.g. an
	 * unimplemented instruction or directive).
	 */
	public static class PreprocessingException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final String filename;
		private final int line;

		public PreprocessingException(String filename, int line, String message) {
			super(filename + ":" + line + ": " + message);
			this.filename = filename;
			this.line = line;
		}

		public String getFilename() { return filename; }
		public int getLine() { return line; }
	}

	private final List<String> warnings = new ArrayList<>();
	private final Map<String, String> defines;
	private final Map<Resource, String> resourceTextCache = new LinkedHashMap<>();
	private final Map<Resource, List<TextRange>> suppressedRawRanges = new LinkedHashMap<>();
	private String effectiveSource;
	private Path sourceDir;
	private int currentLine = -1;
	private int currentOutputLine = 1;
	private int lastMappedOutputLine = -1;
	private WarningCallback warningCallback;

	private final Optional<Path> mapFile;
	private final Optional<Path> outputDir;
	private final boolean mapEnabled;
	private final List<MapEntry> mapEntries = new ArrayList<>();
	private final List<Path> includePaths;

	/**
	 * An entry in the line-to-line map.
	 */
	public static class MapEntry {
		private final String fileName;
		private final int lineNumber;
		private final int originalLine;

		MapEntry(String fileName, int lineNumber, int originalLine) {
			this.fileName = fileName;
			this.lineNumber = lineNumber;
			this.originalLine = originalLine;
		}

		public String getFileName() { return fileName; }
		public int getLineNumber() { return lineNumber; }
		public int getOriginalLine() { return originalLine; }
	}

	/**
	 * Builder for configuring a {@link PP}.
	 */
	public static class Builder {
		private Optional<Path> mapFile = Optional.empty();
		private Optional<Path> outputDir = Optional.empty();
		private boolean mapEnabled;
		private final Map<String, String> defines = new LinkedHashMap<>();
		private WarningCallback warningCallback;
		private List<Path> includePaths = new ArrayList<>();

		private Builder() {}

		/**
		 * Add an array of paths to the list of those searched when locating
		 * INCLUDE resources.
		 * 
		 * @param includePaths include paths
		 * @return this for chaining
		 */
		public Builder addIncludePaths(Path... includePaths) {
			return addIncludePaths(List.of(includePaths));
		}

		/**
		 * Add a list of paths to the list of those searched when locating
		 * INCLUDE resources.
		 * 
		 * @param includePaths include paths
		 * @return this for chaining
		 */
		public Builder addIncludePaths(Collection<Path> includePaths) {
			this.includePaths.addAll(includePaths);
			return this;
		}

		/**
		 * Set the paths searched when locating
		 * INCLUDE resources.
		 * 
		 * @param includePaths include paths
		 * @return this for chaining
		 */
		public Builder withIncludePaths(Path... includePaths) {
			return withIncludePaths(List.of(includePaths));
		}
		
		/**
		 * Set the paths searched when locating
		 * INCLUDE resources.
		 * 
		 * @param includePaths include paths
		 * @return this for chaining
		 */
		public Builder withIncludePaths(Collection<Path> includePaths) {
			this.includePaths.clear();
			return addIncludePaths(includePaths);
		}
		
		/**
		 * Enable .pmap output. The map file path will be derived from the
		 * input path (replacing the extension with {@code .pmap}).
		 * This acts as a flag — the actual path is resolved at write time
		 * if no explicit path is given via {@link #withMap(Path)}.
		 * 
		 * @return this for chaining
		 */
		public Builder withMap() {
			return withMap(true);
		}
		
		/**
		 * Enable .pmap output. The map file path will be derived from the
		 * binary output path (replacing the extension with {@code .pmap}).
		 * This acts as a flag — the actual path is resolved at write time
		 * if no explicit path is given via {@link #withMap(Path)}.
		 * 
		 * @param map whether to enable map
		 * @return this for chaining
		 */
		public Builder withMap(boolean map) {
			this.mapEnabled = map;
			return this;
		}

		/**
		 * Enable .pmap output and specify an explicit file path.
		 * 
		 * @param mapFile map file
		 * @return this for chaining
		 */
		public Builder withMap(Path mapFile) {
			this.mapFile = Optional.of(mapFile);
			this.mapEnabled = true;
			return this;
		}

		/**
		 * Set output directory. When not set, uses same directory as source.
		 * 
		 * @param output dir
		 * @return this for chaining
		 */
		public Builder withOutputDir(Path outputDir) {
			this.outputDir = Optional.of(outputDir);
			return this;
		}

		/**
		 * Set a single defines given its name and value. Value
		 * may be indicating it will evaluated to true but won't
		 * expand to anything.
		 * 
		 * @param name name
		 * @param value value
		 */
		public Builder withDefine(String name, String value) {
			defines.put(name, value);
			return this;
		}
		

		/**
		 * Set the defines. Each string can be either just the 
		 * key, or key=value format. The former will result in a 
		 * an empty define (i.e. still evaluates to true).
		 * 
		 * @param defineSpecs define specs
		 */
		public Builder withDefines(String... defines) {
			return withDefines(Arrays.asList(defines));
		}

		/**
		 * Set the defines. Each string can be either just the 
		 * key, or key=value format. The former will result in a 
		 * an empty define (i.e. still evaluates to true).
		 * 
		 * @param defineSpecs define specs
		 */
		public Builder withDefines(Collection<String> defineSpecs) {
			defineSpecs.forEach(d -> {
				var idx = d.indexOf('=');
				defines.put(
					idx == -1 ? d : d.substring(0, idx), 
					idx == -1 ? null : d.substring(idx + 1)
				);
			});
			return this;
		}

		/**
		 * Set a callback that receives non-fatal warnings during assembly,
		 * including the source filename and line number.
		 */
		public Builder withWarningCallback(WarningCallback warningCallback) {
			this.warningCallback = warningCallback;
			return this;
		}

		public PP build() {
			return new PP(this);
		}
	}

	/**
	 * Create a new builder for configuring a {@link PP}.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Default constructor for backwards compatibility.
	 */
	public PP() {
		this.mapFile = Optional.empty();
		this.mapEnabled = false;
		this.defines = new LinkedHashMap<>();
		this.outputDir = Optional.empty();
		this.includePaths = Collections.emptyList();
	}

	private PP(Builder builder) {
		this.outputDir = builder.outputDir;
		this.mapFile = builder.mapFile;
		this.mapEnabled = builder.mapEnabled;
		this.defines = new LinkedHashMap<>(builder.defines);
		this.warningCallback = builder.warningCallback;
		this.includePaths = Collections.unmodifiableList(new ArrayList<>(builder.includePaths));
	}

	/**
	 * Returns whether map generation was requested (via builder).
	 */
	boolean isMapEnabled() {
		return mapEnabled;
	}

	/**
	 * Process the input program, writing bytes to the given output stream.
	 * If a map file was configured, the .pmap file is written after processing.
	 */
	public Results process(String sourceFileName, PPProgram program, OutputStream out) {
		warnings.clear();
		mapEntries.clear();
		resourceTextCache.clear();
		suppressedRawRanges.clear();
		currentOutputLine = 1;
		lastMappedOutputLine = -1;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// Derive source file name from the resource URI if not explicitly set
		this.effectiveSource = calcEffectiveSource(sourceFileName, program);

		// Derive the source directory for resolving relative paths (e.g. INCBIN)
		if (program.eResource() != null && program.eResource().getURI().isFile()) {
			this.sourceDir = Path.of(program.eResource().getURI().toFileString()).getParent();
		} else {
			this.sourceDir = Path.of("").toAbsolutePath();
		}


		preprocessLines(program, baos);

		try {
			baos.writeTo(out);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write assembled output", e);
		}

		// Write map file if configured
		Path mapOutputFile;
		if (mapEnabled) {
			mapOutputFile = mapFile.orElseGet(() -> {
				int dot = effectiveSource.lastIndexOf('.');
				return outputDir.orElse(this.sourceDir).resolve((dot >= 0 ? effectiveSource.substring(0, dot) : effectiveSource) + ".pmap");
			});
			writeMapFile(mapOutputFile);
		}
		else {
			mapOutputFile = null;
		}
		
		return new Results() {
			@Override
			public Path mapFile() {
				return mapOutputFile;
			}
		};
	}

	protected String calcEffectiveSource(String sourceFileName, PPProgram program) {
		String effectiveSource = sourceFileName;
		if (effectiveSource == null && program.eResource() != null) {
			effectiveSource = program.eResource().getURI().lastSegment();
		}
		if (effectiveSource == null) {
			effectiveSource = "unknown.asm";
		}
		return effectiveSource;
	}

	/**
	 * Returns the collected line-to-line map entries from the last processing.
	 */
	public List<MapEntry> getMapEntries() {
		return mapEntries;
	}

	/**
	 * Returns any warnings collected during the last processing.
	 */
	public List<String> getWarnings() {
		return warnings;
	}


	// ─────────────── Line / source tracking helpers ───────────────

	private void writeMapFile(Path mapFile) {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(mapFile))) {
			String lastFile = null;
			for (MapEntry entry : mapEntries) {
				String fileCol;
				if (lastFile == null || !entry.fileName.equals(lastFile)) {
					fileCol = entry.fileName;
					lastFile = entry.fileName;
				} else {
					fileCol = "";
				}
				pw.println(fileCol + "|" + entry.lineNumber + "|" + entry.originalLine);
			}
		} catch (IOException e) {
			warn("Failed to write map file: " + e.getMessage());
		}
	}

	// ─────────────── Program / line processing ───────────────

	/**
	 * Walk the lines of an {@link PPProgram} and process each directive.
	 * Used both for the top-level program and for conditional branch bodies.
	 */
	private void preprocessLines(PPProgram program, ByteArrayOutputStream out) {
		for (EObject line : program.getLines()) {

			INode lineNode = NodeModelUtils.getNode(line);
			currentLine = lineNode == null ? -1 : lineNode.getStartLine();

			// ── Statement line (may have an optional label prefix) ──
			if (line instanceof PPDirective ppdir) {
				processDirective(ppdir, out);
			}
			else if (line instanceof RawTextBlock rawText) {
				writeRawText(rawText, out);
			}
		}
	}

	private void writeRawText(RawTextBlock rawText, ByteArrayOutputStream out) {
		INode node = NodeModelUtils.getNode(rawText);
		if (node == null) {
			return;
		}
		Resource resource = rawText.eResource();
		if (resource == null) {
			writeProcessedText(out, node.getText(), node.getStartLine());
			return;
		}
		String source = readResourceText(resource);
		if (source == null || source.isEmpty()) {
			writeProcessedText(out, node.getText(), node.getStartLine());
			return;
		}

		int nodeStart = node.getOffset();
		int nodeEnd = Math.min(source.length(), nodeStart + node.getLength());
		if (nodeStart >= nodeEnd) {
			return;
		}

		List<TextRange> ranges = suppressedRawRanges.get(resource);
		if (ranges == null || ranges.isEmpty()) {
			writeProcessedSourceSlice(out, source, nodeStart, nodeEnd);
			return;
		}

		int cursor = nodeStart;
		for (TextRange r : ranges) {
			if (r.end <= cursor) {
				continue;
			}
			if (r.start >= nodeEnd) {
				break;
			}
			int keepStart = cursor;
			int keepEnd = Math.min(nodeEnd, r.start);
			if (keepEnd > keepStart) {
				writeProcessedSourceSlice(out, source, keepStart, keepEnd);
			}
			cursor = Math.max(cursor, Math.min(nodeEnd, r.end));
			if (cursor >= nodeEnd) {
				break;
			}
		}
		if (cursor < nodeEnd) {
			writeProcessedSourceSlice(out, source, cursor, nodeEnd);
		}
	}

	private void writeProcessedSourceSlice(ByteArrayOutputStream out, String source, int start, int end) {
		if (source == null || start >= end) {
			return;
		}
		int cursor = start;
		int sourceLine = lineNumberAtOffset(source, start);
		while (cursor < end) {
			int lineEnd = findLineEnd(source, cursor);
			int chunkEnd = Math.min(lineEnd, end);
			int next = chunkEnd;
			if (chunkEnd < end) {
				next = Math.min(end, skipLineBreak(source, chunkEnd));
			}
			writeProcessedText(out, source.substring(cursor, next), sourceLine);
			if (chunkEnd < end) {
				sourceLine++;
			}
			cursor = next;
		}
	}

	private void writeProcessedText(ByteArrayOutputStream out, String text, int sourceLine) {
		writeMappedText(out, expandDefinesInRawText(text), sourceLine);
	}

	private String expandDefinesInRawText(String text) {
		if (text == null || text.isEmpty() || defines.isEmpty()) {
			return text;
		}

		StringBuilder sb = new StringBuilder(text.length());
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '"' || c == '\'') {
				i = appendQuoted(text, i, sb, c);
				continue;
			}

			if (isIdentifierStart(c)) {
				int start = i;
				i++;
				while (i < text.length() && isIdentifierPart(text.charAt(i))) {
					i++;
				}
				String ident = text.substring(start, i);
				if (defines.containsKey(ident)) {
					String replacement = defines.get(ident);
					if (replacement != null) {
						sb.append(replacement);
					}
				} else {
					sb.append(ident);
				}
				continue;
			}

			sb.append(c);
			i++;
		}
		return sb.toString();
	}

	private int appendQuoted(String text, int from, StringBuilder out, char quote) {
		int i = from;
		out.append(text.charAt(i++));
		while (i < text.length()) {
			char ch = text.charAt(i);
			out.append(ch);
			i++;
			if (ch == '\\' && i < text.length()) {
				out.append(text.charAt(i));
				i++;
				continue;
			}
			if (ch == quote) {
				break;
			}
		}
		return i;
	}

	private void writeText(ByteArrayOutputStream out, String text) {
		try {
			out.write(text.getBytes(StandardCharsets.UTF_8));
			advanceOutputLineCounter(text);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to write raw text block", e);
		}
	}

	private void writeMappedText(ByteArrayOutputStream out, String text, int sourceLine) {
		if (text == null || text.isEmpty()) {
			return;
		}
		if (sourceLine > 0 && currentOutputLine != lastMappedOutputLine) {
			mapEntries.add(new MapEntry(effectiveSource, currentOutputLine, sourceLine));
			lastMappedOutputLine = currentOutputLine;
		}
		writeText(out, text);
	}

	private void advanceOutputLineCounter(String text) {
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '\n') {
				currentOutputLine++;
			} else if (ch == '\r') {
				currentOutputLine++;
				if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
			}
		}
	}

	// ─────────────── Statement dispatch ───────────────

	private void processDirective(PPDirective stmt, ByteArrayOutputStream out) {

		if (stmt instanceof PPDefineMacro) {
			processDefineMacro((PPDefineMacro) stmt);
			return;
		}
		
		if (stmt instanceof PPUndef) {
			processUndef((PPUndef) stmt);
			return;
		}
		
		if (stmt instanceof PPInclude) {
			processInclude((PPInclude) stmt, out);
			return;
		}
		
		if (stmt instanceof PPAssert) {
			processAssert((PPAssert) stmt, out);
			return;
		}
		
		if (stmt instanceof PPDefine) {
			processDefine((PPDefine) stmt, out);
			return;
		}
		
		if (stmt instanceof PPUndefine udefn) {
			processUndefine(udefn, out);
			return;
		}

		// ── Conditional processing ──
		if (stmt instanceof PPIf) {
			processIf((PPIf) stmt, out);
			return;
		}
		if (stmt instanceof PPIfDef) {
			processIfDef((PPIfDef) stmt, out);
			return;
		}
		if (stmt instanceof PPIfNDef) {
			processIfNDef((PPIfNDef) stmt, out);
			return;
		}

		// If we get here, the directive is not yet supported — hard fail
		throw new PreprocessingException(effectiveSource, currentLine,
				"Unsupported instruction/directive: " + stmt.eClass().getName());
	}

	/**
	 * #DEFINE name <replacement-list>
	 *
	 * A trailing '\\' continues the definition onto the next physical line.
	 * Remaining '\\' characters are translated to newlines in the captured
	 * replacement text, matching z88dk semantics.
	 */
	private void processDefineMacro(PPDefineMacro directive) {
		DefineCapture define = extractDefine(directive);
		if (define == null || define.name == null || define.name.isBlank()) {
			warn("#DEFINE: missing symbol name");
			return;
		}
		defines.put(define.name, define.body);
		if (define.suppressStart >= 0 && define.suppressEnd > define.suppressStart && directive.eResource() != null) {
			addSuppressedRange(directive.eResource(), define.suppressStart, define.suppressEnd);
		}
	}
	private void processUndef(PPUndef directive) {
		defines.remove(directive.getName());
	}

	private void addSuppressedRange(Resource resource, int start, int end) {
		List<TextRange> ranges = suppressedRawRanges.computeIfAbsent(resource, k -> new ArrayList<>());
		ranges.add(new TextRange(start, end));
		ranges.sort((a, b) -> Integer.compare(a.start, b.start));
	}

	private DefineCapture extractDefine(PPDefineMacro directive) {
		INode defineNode = NodeModelUtils.getNode(directive);
		if (defineNode == null) {
			return null;
		}
		String text = readResourceText(directive.eResource());
		if (text == null || text.isEmpty()) {
			return null;
		}

		int lineStart = defineNode.getOffset();
		if (lineStart >= text.length()) {
			return null;
		}
		int lineEnd = findLineEnd(text, lineStart);
		String firstLine = text.substring(lineStart, lineEnd);

		int keywordIdx = indexOfIgnoreCase(firstLine, "#DEFINE");
		if (keywordIdx < 0) {
			keywordIdx = indexOfIgnoreCase(firstLine, "DEFINE");
			if (keywordIdx < 0) {
				return null;
			}
		}
		int pos = keywordIdx + (firstLine.startsWith("#", keywordIdx) ? 7 : 6);
		while (pos < firstLine.length() && (firstLine.charAt(pos) == ' ' || firstLine.charAt(pos) == '\t')) {
			pos++;
		}
		if (pos >= firstLine.length()) {
			return new DefineCapture(null, null, -1, -1);
		}

		int nameStart = pos;
		if (!isIdentifierStart(firstLine.charAt(nameStart))) {
			return new DefineCapture(null, null, -1, -1);
		}
		pos++;
		while (pos < firstLine.length() && isIdentifierPart(firstLine.charAt(pos))) {
			pos++;
		}
		String name = firstLine.substring(nameStart, pos);

		int cursor = lineStart + pos;
		StringBuilder body = new StringBuilder();
		boolean firstBodyLine = true;
		while (cursor < text.length()) {
			int bodyLineEnd = findLineEnd(text, cursor);
			String segment = text.substring(cursor, bodyLineEnd);
			if (firstBodyLine) {
				segment = stripLeadingHorizontalWhitespace(segment);
				firstBodyLine = false;
			}
			boolean continued = endsWithContinuation(segment);
			if (continued) {
				segment = removeTrailingContinuation(segment);
			}
			body.append(segment);
			if (!continued) {
				break;
			}
			cursor = skipLineBreak(text, bodyLineEnd);
		}

		String defineBody = body.isEmpty() ? null : body.toString().replace('\\', '\n');
		int suppressStart = lineStart + pos;
		int suppressEnd = Math.min(text.length(), skipLineBreak(text, findLineEnd(text, cursor)));
		if (cursor >= text.length()) {
			suppressEnd = text.length();
		}
		return new DefineCapture(name, defineBody, suppressStart, suppressEnd);
	}

	private String readResourceText(Resource resource) {
		String cached = resourceTextCache.get(resource);
		if (cached != null) {
			return cached;
		}

		String text = null;
		if (resource == null) {
			return null;
		}
		URI uri = resource.getURI();
		if (uri != null && uri.isFile()) {
			try {
				text = Files.readString(Path.of(uri.toFileString()), StandardCharsets.UTF_8);
			} catch (IOException e) {
				warn("#DEFINE: failed to read source text: " + e.getMessage());
			}
		}
		if (text == null && resource instanceof XtextResource xtextResource) {
			INode root = xtextResource.getParseResult() == null ? null : xtextResource.getParseResult().getRootNode();
			text = root == null ? null : root.getText();
		}
		if (text != null) {
			resourceTextCache.put(resource, text);
		}
		return text;
	}

	private int indexOfIgnoreCase(String text, String pattern) {
		return text.toUpperCase().indexOf(pattern.toUpperCase());
	}

	private boolean isIdentifierStart(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
	}

	private boolean isIdentifierPart(char c) {
		return isIdentifierStart(c) || (c >= '0' && c <= '9');
	}

	private static final class DefineCapture {
		private final String name;
		private final String body;
		private final int suppressStart;
		private final int suppressEnd;

		private DefineCapture(String name, String body, int suppressStart, int suppressEnd) {
			this.name = name;
			this.body = body;
			this.suppressStart = suppressStart;
			this.suppressEnd = suppressEnd;
		}
	}

	private static final class TextRange {
		private final int start;
		private final int end;

		private TextRange(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	private int findLineEnd(String text, int from) {
		int i = from;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				break;
			}
			i++;
		}
		return i;
	}

	private int skipLineBreak(String text, int index) {
		if (index >= text.length()) {
			return index;
		}
		char c = text.charAt(index);
		if (c == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
			return index + 2;
		}
		if (c == '\r' || c == '\n') {
			return index + 1;
		}
		return index;
	}

	private int lineNumberAtOffset(String text, int offset) {
		int line = 1;
		int i = 0;
		int end = Math.min(offset, text.length());
		while (i < end) {
			char c = text.charAt(i);
			if (c == '\n') {
				line++;
			}
			else if (c == '\r') {
				line++;
				if (i + 1 < end && text.charAt(i + 1) == '\n') {
					i++;
				}
			}
			i++;
		}
		return line;
	}

	private boolean endsWithContinuation(String segment) {
		int i = segment.length() - 1;
		while (i >= 0) {
			char c = segment.charAt(i);
			if (c == ' ' || c == '\t') {
				i--;
				continue;
			}
			return c == '\\';
		}
		return false;
	}

	private String removeTrailingContinuation(String segment) {
		int i = segment.length() - 1;
		while (i >= 0 && (segment.charAt(i) == ' ' || segment.charAt(i) == '\t')) {
			i--;
		}
		if (i >= 0 && segment.charAt(i) == '\\') {
			return segment.substring(0, i);
		}
		return segment;
	}

	private String stripLeadingHorizontalWhitespace(String s) {
		int i = 0;
		while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
			i++;
		}
		return i == 0 ? s : s.substring(i);
	}

//	/**
//	 * BINARY / INCBIN — load a binary file at the current location in the
//	 * object file. The file path is resolved relative to the source file
//	 * being assembled.
//	 */
//	private void assembleIncBin(PPIncBin directive, ByteArrayOutputStream out) {
//		String fileName = directive.getFile();
//		if (fileName == null || fileName.isEmpty()) {
//			warn("INCBIN: no file specified");
//			return;
//		}
//		Path filePath = sourceDir.resolve(fileName);
//		try {
//			byte[] data = Files.readAllBytes(filePath);
//			for (byte b : data) {
//				emit8(out, b & 0xFF);
//			}
//		} catch (IOException e) {
//			throw new PreprocessingException(effectiveSource, currentLine,
//					"INCBIN: cannot read file '" + filePath + "': " + e.getMessage());
//		}
//	}

	/**
	 * INCLUDE "file" — process the included source file at the
	 * current position. The included file must already be loaded into the
	 * resource set (Xtext's {@code importURI} mechanism handles this).
	 * Source-to-address map entries use the included file's name.
	 */
	private void processInclude(PPInclude directive, ByteArrayOutputStream out) {
		String importURI = directive.getImportURI();
		if (importURI == null || importURI.isEmpty()) {
			warn("INCLUDE: no file specified");
			return;
		}

		// Strip surrounding quotes / angle brackets
		boolean angleInclude = importURI.startsWith("<") && importURI.endsWith(">");
		importURI = stripQuotes(importURI);
		if (angleInclude) {
			// stripQuotes won't handle angle brackets, strip them manually
			importURI = importURI.substring(1, importURI.length() - 1);
		}

		// Resolve the include path
		Resource containingResource = directive.eResource();
		if (containingResource == null) {
			throw new PreprocessingException(effectiveSource, currentLine,
					"INCLUDE: cannot resolve '" + importURI + "' — no containing resource");
		}

		URI resolvedURI = null;

		// For quoted includes, try relative to the containing resource first
		if (!angleInclude) {
			URI baseURI = containingResource.getURI();
			URI candidateURI = URI.createFileURI(importURI).resolve(baseURI);
			try {
				Path candidatePath = Path.of(java.net.URI.create(candidateURI.toString()));
				if (Files.exists(candidatePath)) {
					resolvedURI = candidateURI;
				}
			} catch (Exception e) {
				// fall through to include path search
			}
		}

		// Search the builder-provided include paths
		if (resolvedURI == null && !includePaths.isEmpty()) {
			for (Path incDir : includePaths) {
				Path candidate = incDir.resolve(importURI);
				if (Files.exists(candidate)) {
					resolvedURI = URI.createFileURI(candidate.toAbsolutePath().toString());
					break;
				}
			}
		}

		// Last resort: resolve relative to containing resource even if file may not exist
		if (resolvedURI == null) {
			URI baseURI = containingResource.getURI();
			resolvedURI = URI.createFileURI(importURI).resolve(baseURI);
		}

		// Look up or load the resource from the resource set
		ResourceSet resourceSet = containingResource.getResourceSet();
		Resource includedResource = null;
		try {
			includedResource = resourceSet.getResource(resolvedURI, true);
		} catch (Exception e) {
			throw new PreprocessingException(effectiveSource, currentLine,
					"INCLUDE: cannot load '" + importURI + "': " + e.getMessage());
		}

		if (includedResource == null || includedResource.getContents().isEmpty()) {
			throw new PreprocessingException(effectiveSource, currentLine,
					"INCLUDE: empty or unresolvable resource '" + importURI + "'");
		}

		if (!(includedResource.getContents().get(0) instanceof PPProgram)) {
			throw new PreprocessingException(effectiveSource, currentLine,
					"INCLUDE: resource '" + importURI + "' does not contain an AsmProgram");
		}

		PPProgram includedProgram = (PPProgram) includedResource.getContents().get(0);

		// Save and switch source context for map entries
		String previousSource = this.effectiveSource;
		Path previousSourceDir = this.sourceDir;

		this.effectiveSource = resolvedURI.lastSegment();
		if (resolvedURI.isFile()) {
			this.sourceDir = Path.of(resolvedURI.toFileString()).getParent();
		}

		writeMappedText(out, System.lineSeparator(), currentLine);
		
		// Assemble the included program inline
		preprocessLines(includedProgram, out);

		writeMappedText(out, System.lineSeparator(), currentLine);

		// Restore source context
		this.effectiveSource = previousSource;
		this.sourceDir = previousSourceDir;
	}

	// ─────────────── Conditional compilation ───────────────
	private void processUndefine(PPUndefine directive, ByteArrayOutputStream out) {
		if(!defines.containsKey(directive.getName())) {
			warn(String.format("Undefining undefined %s", directive.getName()));
		}
		else {
			defines.remove(directive.getName());
		}
	}
	
	private void processDefine(PPDefine directive, ByteArrayOutputStream out) {
		if(defines.containsKey(directive.getName())) {
			warn(String.format("Redefining %s", directive.getName()));
		}
		defines.put(directive.getName(), String.valueOf(resolveImmediate(directive.getExpression())));
	}
	
	private void processAssert(PPAssert directive, ByteArrayOutputStream out) {
		// Check the primary IF condition
		if (resolveImmediate(directive.getCondition()) != 0) {
			return;
		}
		
		throw new PreprocessingException(effectiveSource, currentLine, directive.getMessage() == null ? "Assertion failed.": directive.getMessage());
	}

	/**
	 * IF condition ... ELIF ... ELSE ... ENDIF
	 * Evaluates expression conditions; a non-zero result is true.
	 */
	private void processIf(PPIf directive, ByteArrayOutputStream out) {
		// Check the primary IF condition
		if (resolveImmediate(directive.getCondition()) != 0) {
			preprocessLines(directive.getProgram(), out);
			return;
		}

		// Check ELIF branches
		EList<PPExpression> elifConditions = directive.getElifCondition();
		EList<PPProgram> elifPrograms = directive.getElifProgram();
		for (int i = 0; i < elifConditions.size(); i++) {
			if (resolveImmediate(elifConditions.get(i)) != 0) {
				preprocessLines(elifPrograms.get(i), out);
				return;
			}
		}

		// ELSE fallback
		if (directive.getElseProgram() != null) {
			preprocessLines(directive.getElseProgram(), out);
		}
	}

	/**
	 * IFDEF name ... ELIFDEF ... ELSE ... ENDIF
	 * Checks whether a symbol is present in the defines map.
	 */
	private void processIfDef(PPIfDef directive, ByteArrayOutputStream out) {
		// Check the primary IFDEF name
		if (defines.containsKey(directive.getName())) {
			preprocessLines(directive.getProgram(), out);
			return;
		}

		// Check ELIFDEF branches
		EList<String> elifNames = directive.getElifName();
		EList<PPProgram> elifPrograms = directive.getElifProgram();
		for (int i = 0; i < elifNames.size(); i++) {
			if (defines.containsKey(elifNames.get(i))) {
				preprocessLines(elifPrograms.get(i), out);
				return;
			}
		}

		// ELSE fallback
		if (directive.getElseProgram() != null) {
			preprocessLines(directive.getElseProgram(), out);
		}
	}

	/**
	 * IFNDEF name ... ELIFNDEF ... ELSE ... ENDIF
	 * Checks whether a symbol is <em>not</em> present in the defines map.
	 */
	private void processIfNDef(PPIfNDef directive, ByteArrayOutputStream out) {
		// Check the primary IFNDEF name
		if (!defines.containsKey(directive.getName())) {
			preprocessLines(directive.getProgram(), out);
			return;
		}

		// Check ELIFNDEF branches
		EList<String> elifNames = directive.getElifName();
		EList<PPProgram> elifPrograms = directive.getElifProgram();
		for (int i = 0; i < elifNames.size(); i++) {
			if (!defines.containsKey(elifNames.get(i))) {
				preprocessLines(elifPrograms.get(i), out);
				return;
			}
		}

		// ELSE fallback
		if (directive.getElseProgram() != null) {
			preprocessLines(directive.getElseProgram(), out);
		}
	}

	/**
	 * Resolve an operand to an integer value. Recursively evaluates
	 * expressions including binary operators, unary sign/not, literals,
	 * strings (first char ordinal), and symbols (looked up from the symbol
	 * table populated during pass&nbsp;1).
	 */
	private int resolveImmediate(PPExpression operand) {
		if (operand instanceof IntegralLiteral) {
			return resolveIntegralLiteral((IntegralLiteral) operand);
		}
		if (operand instanceof BinaryExpr) {
			BinaryExpr bin = (BinaryExpr) operand;
			int left = resolveImmediate(bin.getLeft());
			int right = resolveImmediate(bin.getRight());
			switch (bin.getOp()) {
				case "+":  return left + right;
				case "-":  return left - right;
				case "*":  return left * right;
				case "/":
					if (right == 0) { warn("Division by zero"); return 0; }
					return left / right;
				case "%":
					if (right == 0) { warn("Modulo by zero"); return 0; }
					return left % right;
				case "<<": return left << right;
				case ">>": return left >> right;
				default:
					warn("Unknown operator: " + bin.getOp());
					return 0;
			}
		}
		if (operand instanceof PPSignedExpr) {
			PPSignedExpr signed = (PPSignedExpr) operand;
			int val = resolveImmediate(signed.getExpr());
			// Determine the sign from the source text node
			INode node = NodeModelUtils.getNode(signed);
			if (node != null && node.getText().trim().startsWith("-")) {
				return -val;
			}
			return val;
		}
		if (operand instanceof PPNotExpr) {
			int val = resolveImmediate(((PPNotExpr) operand).getExpr());
			return val == 0 ? 1 : 0;
		}
		if (operand instanceof StringLiteral) {
			String s = resolveString(operand);
			if (s != null && !s.isEmpty()) {
				return s.charAt(0) & 0xFF;
			}
			return 0;
		}
		if(operand instanceof SymbolLiteral slit) {
			String s = slit.getValue();
			String v = defines.get(s);
			return v == null || "0".equals(v) ? 0 : 1;
		}

		if (operand instanceof PPIndirect) {
			return resolveImmediate(((PPIndirect) operand).getExpr());
		}
		warn("Cannot resolve operand to immediate value: " + operand.eClass().getName());
		return 0;
	}

	/**
	 * Resolve a StringLiteral to its raw string value (quotes stripped).
	 * Returns null if the operand is not a StringLiteral.
	 */
	private String resolveString(PPExpression operand) {
		if (operand instanceof StringLiteral) {
			return ((StringLiteral) operand).getValue();
		}
		return null;
	}

	/**
	 * Parse an IntegralLiteral to an int, handling decimal, hex ($, 0x, h suffix),
	 * and binary (%, 0b, b suffix) formats.
	 */
	private int resolveIntegralLiteral(IntegralLiteral lit) {
		String litStr = lit.getLitvalue();
		if (litStr != null && !litStr.isEmpty()) {
			return parseLitvalue(litStr);
		}
		// Decimal — the parser already converted to int
		return lit.getValue();
	}

	private int parseLitvalue(String s) {
		s = s.trim();
		// Hex: $XXXX or 0xXXXX
		if (s.startsWith("$")) {
			return (int) Long.parseLong(s.substring(1), 16);
		}
		if (s.toLowerCase().startsWith("0x")) {
			return (int) Long.parseLong(s.substring(2), 16);
		}
		// Hex: XXXXh or XXXXH
		if (s.endsWith("h") || s.endsWith("H")) {
			return (int) Long.parseLong(s.substring(0, s.length() - 1), 16);
		}
		// Binary: %XXXX or 0bXXXX
		if (s.startsWith("%")) {
			return (int) Long.parseLong(s.substring(1), 2);
		}
		if (s.toLowerCase().startsWith("0b")) {
			return (int) Long.parseLong(s.substring(2), 2);
		}
		// Binary: XXXXb or XXXXB
		if (s.endsWith("b") || s.endsWith("B")) {
			return (int) Long.parseLong(s.substring(0, s.length() - 1), 2);
		}
		// Fallback decimal
		return (int) Long.parseLong(s);
	}


	private void warn(String message) {
		warnings.add(message);
		if (warningCallback != null && currentLine > 0) {
			warningCallback.warn(effectiveSource, currentLine, message);
		}
	}

	/**
	 * Strip surrounding quotes from a string, if present.
	 */
	private String stripQuotes(String s) {
		s = s.trim();
		if (s.startsWith("\"") && s.endsWith("\"")) {
			return s.substring(1, s.length() - 1);
		}
		if (s.startsWith("'") && s.endsWith("'")) {
			return s.substring(1, s.length() - 1);
		}
		return s;
	}
	
}
