package uk.co.bithatch.eclipzpp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import uk.co.bithatch.bitzx.AbstractTool;
import uk.co.bithatch.eclipzpp.SourceMap.Segment;


/**
 * Generic pre-process compatible with ZX Basic preprocess (zxbpp), but with some additional
 * features we need for editing.
 */
public class GenericPreprocessor extends AbstractTool {
	
	public enum Format {
		Z88DK, BORIEL, ALL
	}
	
	/**
	 * Builder
	 */
	public final static class Builder extends AbstractBuilder<Builder, GenericPreprocessor> {

		private final Map<String, String> defines = new  HashMap<>();
		private final Set<Warning> suppressedWarnings = new HashSet<Warning>(); 
		private Optional<BiConsumer<Warning, String>> onWarning = Optional.empty(); 
		private Optional<BiConsumer<Error, String>> onError = Optional.empty(); 
		private Optional<Consumer<String[]>> onPragma = Optional.empty();
		private Optional<ResourceResolver<Object>> resourceResolver = Optional.empty();
		private Optional<SourceMap> sourceMap = Optional.empty();
		private boolean expandRequire = false;
		private Mode mode= Mode.COMPILER;
		private Optional<Character> lineContinuations = Optional.of('\\');
		private Format format = Format.ALL;
		
		public Builder withFormat(Format format) {
			this.format = format;
			return this;
		}

		public Builder withoutLineContinuations() {
			this.lineContinuations = Optional.empty();
			return this;
		}
		
		public Builder withLineContinuations(char lineContinuations) {
			this.lineContinuations = Optional.of(lineContinuations);
			return this;
		}

		public Builder withMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder withExpandRequire() {
			return withExpandRequire(true);
		}
		
		public Builder withExpandRequire(boolean expandRequire) {
			this.expandRequire = expandRequire;
			return this;
		}
		
		public Builder withSuppressedWarnings(Warning... suppressed) {
			return withSuppressedWarnings(Arrays.asList(suppressed));
		}
		
		public Builder withSuppressedWarnings(Collection<Warning> suppressed) {
			this.suppressedWarnings.addAll(suppressed);
			return this;
		}

		public Builder addDefines(Map<String, String> defines) {
			this.defines.putAll(defines);
			return this;
		}
		
		public Builder withDefines(Map<String, String> defines) {
			this.defines.clear();
			addDefines(defines);
			return this;
		}
		
		public Builder withSourceMap(SourceMap sourceMap) {
			this.sourceMap = Optional.of(sourceMap);
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public Builder withResourceResolver(ResourceResolver<?> resolver) {
			this.resourceResolver = Optional.of((ResourceResolver<Object>)resolver);
			return this;
		}
		
		public Builder onWarning(BiConsumer<Warning, String> onWarning) {
			this.onWarning = Optional.of(onWarning);
			return this;
		}
		
		public Builder onPragma(Consumer<String[]> onPragma) {
			this.onPragma = Optional.of(onPragma);
			return this;
		}
		
		public Builder onError(BiConsumer<Error, String> onError) {
			this.onError= Optional.of(onError);
			return this;
		}
		
		public GenericPreprocessor build() {
			return new GenericPreprocessor(this);
		}
	}

	private final Map<String, String> defines;
	private final Set<Warning> suppressedWarnings;  
	private final Optional<BiConsumer<Warning, String>> onWarning;
	private final Optional<BiConsumer<Error, String>> onError;
	private final Optional<ResourceResolver<Object>> resourceResolver; 
	private final Optional<Consumer<String[]>> onPragma;
	private final Optional<SourceMap> sourceMap;
	private final boolean expandRequire;
	private final Mode mode;
	private final Optional<Character> lineContinuations;
	private final Format format;
    
    private GenericPreprocessor(Builder bldr) {
    	super(bldr);
    	
		suppressedWarnings = Collections.unmodifiableSet(new HashSet<>(bldr.suppressedWarnings));
		format = bldr.format;
		resourceResolver = bldr.resourceResolver;

		onWarning = bldr.onWarning;
		onError = bldr.onError;
		onPragma = bldr.onPragma;
		sourceMap = bldr.sourceMap;
		expandRequire = bldr.expandRequire;
		lineContinuations = bldr.lineContinuations;
		mode = bldr.mode;

		if(sourceMap.isPresent()) {
			defines = sourceMap.get().defines();
		}
		else {
			defines = new HashMap<>(bldr.defines);
		}
		defines.putAll(bldr.defines);
    }
    
    public Map<String, String> defines() {
    	return Collections.unmodifiableMap(new HashMap<>(defines));
    }

    public String process(String content) throws IOException {
    	return process(content, null);
    }

    public String process(String content, Object context) throws IOException {
    	try(var rdr = new StringReader(content)) {
        	try(var swtr = new StringWriter()) {
        		process(rdr, swtr, context);
            	return swtr.toString();
        	}
    	}
    }

    public String process(Path path) throws IOException {
    	try(var rdr = new InputStreamReader(Files.newInputStream(path))) {
        	try(var swtr = new StringWriter()) {
        		process(rdr, swtr, path);
            	return swtr.toString();
        	}
    	}
    }

    public void process(Reader rdr, Writer wtr,  Object context) throws IOException {
		var it = new ReaderIterator(rdr);
		var pwtr = wtr instanceof PrintWriter pw ? pw : new PrintWriter(wtr, true);
    	doRun(it, context).forEach(s -> {
    		var lines = s.split("\\r?\\n");
			for(var i = 0 ; i < lines.length; i++) {
				if(lineContinuations.isPresent() && i < lines.length - 1) {
					pwtr.println(lines[i] + lineContinuations.get());
				}
				else {
					pwtr.println(lines[i]);
				}
    		}
    	});
    }
    
	protected Stream<String> doRun(Iterator<String> root, Object context) {
		var sit = new SourceToTargetIterator(context, root);
		Iterable<String> iterable = () -> sit;
		return StreamSupport.stream(iterable.spliterator(), false);
	}


	private String evalConstExpression(String expr) {
		var evaluator = new ConstExpressionEvaluator(defines, this::emitWarning);
		return Long.toString(evaluator.evaluate(expr));
	}

	private void emitWarning(Warning warning, String message) {
		if(!suppressedWarnings.contains(warning)) {
			onWarning.ifPresent(ow -> ow.accept(warning, message));
		}
	}

	private final class SourceToTargetIterator implements Iterator<String> {
		private static final Pattern DEFL_STD_PATTERN =
				Pattern.compile("(?i)^defl\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(?:=\\s*)?(.*)$");
		private static final Pattern DEFL_ALT_PATTERN =
				Pattern.compile("(?i)^([A-Za-z_][A-Za-z0-9_]*)\\s+defl\\s*(?:=\\s*)?(.*)$");

		private String next;
		private ArrayDeque<String> pendingOutput = new ArrayDeque<>();
		private StringBuilder buf = new StringBuilder();
		private Set<String> included = new HashSet<>();
		private Stack<IncludeContext<Object>> stack = new Stack<>();
		private Map<String, MacroDefinition> macros = new HashMap<>();
		private Stack<MacroInvocationContext> macroStack = new Stack<>();
		private AtomicInteger preprocessedOffset = new AtomicInteger();
		private AtomicInteger preprocessedLine = new AtomicInteger();
		private AtomicInteger localCounter = new AtomicInteger();
		
		private final static byte[] LINESEP = System.lineSeparator().getBytes();
		
		private boolean rootMarkerOutput;

		private record MacroDefinition(String name, List<String> params, List<String> body) {
		}

		private final class MacroInvocationContext {
			private final int invocationId = localCounter.incrementAndGet();
			private final Map<String, String> locals = new HashMap<>();
			private boolean exitRequested;
		}

		private SourceToTargetIterator(Object context, Iterator<String> sourceIt) {
			stack.add(new IncludeContext<Object>(context, context == null ? null : context.toString(), sourceIt));
		}

		@Override
		public boolean hasNext() {
			checkNext();
			return next != null;
		}
		
		private void error(Error err, String text) {
			onError.ifPresentOrElse(oe -> {
				oe.accept(err, text);
			}, () -> {
				throw new IllegalArgumentException(text);
			});	
		}
		
		private Optional<String> process(String line) {
			if(!pendingOutput.isEmpty()) {
				return Optional.of(pendingOutput.poll());
			}
			var res = stack.peek();
			var lower = line.toLowerCase();
			var thisLineNo = res.lineNumber().get();
			
//			System.out.println(">>process " + thisLineNo  + " :" + line + " [" + res.lastPrintedLine().get() + "]");
			
			var ln = doProcess(line, res, lower, thisLineNo);
			if(ln.isPresent()) {
				res.lastPrintedLine().set(thisLineNo);
			}
			return ln;
		}
		

		private Optional<String> doProcess(String line, IncludeContext<Object> res, String lower, int thisLineNo) {
			
//			System.out.println("DO PROCESS: " + line);
			var cmd = lower.trim().split("\\s+");
			var directive = cmd[0];
			
			/* Condition branches */
			
			if(isElseDirective(directive)) {
				if(res.conditions().isEmpty() || res.conditions().peek().inElse) {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
				else {
					res.conditions().peek().enterElse();
					return includeDirectiveIfEditorMode(line, 1);
				}
			}
			else if(isElifDirective(directive)) {
				if(res.conditions().isEmpty() || res.conditions().peek().inElse) {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
				else {
					res.conditions().peek().applyElif(elif(line, directive));
					return includeDirectiveIfEditorMode(line, 1);
				}
			}
			else if(isElifdefDirective(directive)) {
				if(res.conditions().isEmpty() || res.conditions().peek().inElse) {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
				else {
					res.conditions().peek().applyElif(elifdef(line, directive));
					return includeDirectiveIfEditorMode(line, 1);
				}
			}
			else if(isElifndefDirective(directive)) {
				if(res.conditions().isEmpty() || res.conditions().peek().inElse) {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
				else {
					res.conditions().peek().applyElif(elifndef(line, directive));
					return includeDirectiveIfEditorMode(line, 1);
				}
			}
			else if(isEndifDirective(directive)) {
				if(res.conditions().isEmpty()) {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
				else {
					res.conditions().pop();
				}
				return includeDirectiveIfEditorMode(line, 1);
			}
			else if(!res.conditions().isEmpty()) {
				var cond = res.conditions().peek();
				if(!cond.matches())
					if(isDirectiveLine(line, directive))
						return includeDirectiveIfEditorMode(line, 1);
					else if(mode == Mode.EDITOR) {
						return Optional.of(replaceWithSpaces(line));
					}
					else {
						return Optional.empty();
					}
			}
			
			/* Generic directives supported in all formats */
			if(cmd[0].equals("#define")) {
				if(defineMacro(line.trim())) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
			}
			else if(cmd[0].equals("#undef")) {
				if(undefMacro(line.trim())) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
			}
			else if(isIfndefDirective(directive)) {
				if(ifndef(line.trim(), directive)) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
			}
			else if(isIfdefDirective(directive)) {
				if(ifdef(line.trim(), directive)) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					return Optional.of(line);
				}
			}
			
			/* Z88DK specific directives */
			if(format != Format.BORIEL) {
				if(isIfDirective(directive)) {
					if(ifExpr(line, directive)) {
						return includeDirectiveIfEditorMode(line, 1);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}

				if(directive.equals("macro") || isAltMacroDirective(lower)) {
					if(macroDirective(line.trim(), lower, res)) {
						return includeDirectiveIfEditorMode(line, 2);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("exitm")) {
					if(exitmDirective()) {
						return Optional.empty();
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("assert")) {
					if(assertDirective(line.trim())) {
						return includeDirectiveIfEditorMode(line, 2);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("binary") || directive.equals("incbin")) {
					var expanded = binaryDirective(line.trim(), directive);
					if(expanded.isPresent()) {
						return mode == Mode.EDITOR ? includeDirectiveIfEditorMode(line, 2) : expanded;
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("defl")) {
					if(deflDirective(line.trim())) {
						return includeDirectiveIfEditorMode(line, 2);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(cmd.length > 1 && cmd[1].equals("defl")) {
					if(deflDirectiveAlt(line.trim())) {
						return includeDirectiveIfEditorMode(line, 2);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("local")) {
					if(localDirective(line.trim())) {
						return includeDirectiveIfEditorMode(line, 2);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("rept")) {
					var expanded = reptDirective(line.trim(), res);
					if(expanded.isPresent()) {
						return expanded;
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(cmd[0].equals("undefine")) {
					if(undefine(line.trim())) {
						return includeDirectiveIfEditorMode(line, 1);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("reptc")) {
					var expanded = reptcDirective(line.trim(), res);
					if(expanded.isPresent()) {
						return expanded;
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("repti")) {
					var expanded = reptiDirective(line.trim(), res);
					if(expanded.isPresent()) {
						return expanded;
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("include")) {
					if(include(line.trim())) {
						return includeDirectiveIfEditorMode(line, 2);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
				else if(directive.equals("define")) {
					if(define(line.trim())) {
						return includeDirectiveIfEditorMode(line, 2);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
			}

			var macroExpanded = tryExpandMacroInvocation(line);
			if(macroExpanded.isPresent()) {
				return macroExpanded;
			}


			/* Boriel specific directives */
			if(format != Format.BORIEL) {
				if(directive.equals("#pragma")) {
					if(pragma(line.trim())) {
						return includeDirectiveIfEditorMode(line, 1);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
						return Optional.of(line);
					}
				}
				else  if(directive.equals("#error")) {
					onError.ifPresent(oe -> oe.accept(Error.ERROR_DIRECTIVE, line.trim().substring(7).trim()));
					return includeDirectiveIfEditorMode(line, 1);
				}
				else if(directive.equals("#warning")) {
					onWarning.ifPresent(ow -> ow.accept(Warning.WARNING_DIRECTIVE, line.trim().substring(9).trim()));
					return includeDirectiveIfEditorMode(line, 1);
				}
				else if(directive.equals("#require")) {
					if(expandRequire) {
						if(require(line.trim()))
							return Optional.empty();
						else {
							error(Error.SYNTAX_ERROR,"Syntax error " + line.trim() + ".");
							return Optional.of(line);
						}
					}
					else {
						return includeDirectiveIfEditorMode(line, 1);
					}
				}
				else if(directive.equals("#include")) {
					if(include(line.trim())) {
						return includeDirectiveIfEditorMode(line, 2);
					}
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line.translateEscapes() + ".");
						return Optional.of(line);
					}
				}
			}
			

			if (line.startsWith("#")) {

				// TODO
				
//	    	    INIT = "INIT"
//	    	    LINE = "LINE"
				
				onWarning.ifPresent(ow -> ow.accept(Warning.UNKNOWN_PREPROCESSOR_DIRECTIVE, "Unknown preprocessor directive " + line.trim() + "."));
				return includeDirectiveIfEditorMode(line, 1);
			}
			else if(mode == Mode.COMPILER || stack.size() == 1) {
				
				/* Only actually produce any output if we are in 
				 * COMPILER mode, or if this is the root source with EDITOR
				 * mode.
				 */
				
				var lastLine = res.lastPrintedLine().get();
				var dif = thisLineNo - lastLine - 1;
				if(dif > 0) {
//					System.out.println("Offset " + dif);
					nextSegment(res);
				}
				
//				if(mode == Mode.EDITOR)
//					// TODO not really sure ... macro expansion is the last knotty problem
//					return Optional.of(line);
//				else
					return Optional.of(MacroExpander.expandLine(line, defines));
			}
			else {
				return Optional.empty();
			}
		}

		private boolean isIfDirective(String directive) {
			return directive.equals("#if") || directive.equals("if");
		}

		private boolean isElifDirective(String directive) {
			return directive.equals("#elif") || directive.equals("elif");
		}

		private boolean isIfdefDirective(String directive) {
			return directive.equals("#ifdef") || directive.equals("ifdef");
		}

		private boolean isIfndefDirective(String directive) {
			return directive.equals("#ifndef") || directive.equals("ifndef");
		}

		private boolean isElifdefDirective(String directive) {
			return directive.equals("#elifdef") || directive.equals("elifdef");
		}

		private boolean isElifndefDirective(String directive) {
			return directive.equals("#elifndef") || directive.equals("elifndef");
		}

		private boolean isElseDirective(String directive) {
			return directive.equals("#else") || directive.equals("else");
		}

		private boolean isEndifDirective(String directive) {
			return directive.equals("#endif") || directive.equals("endif");
		}

		private boolean isEndrDirective(String directive) {
			return directive.equals("endr") || directive.equals("#endr");
		}

		private boolean isEndmDirective(String directive) {
			return directive.equals("endm") || directive.equals("#endm");
		}

		private boolean isDirectiveLine(String line, String directive) {
			if(line.startsWith("#")) {
				return true;
			}
			return isIfDirective(directive)
					|| isElifDirective(directive)
					|| isIfdefDirective(directive)
					|| isIfndefDirective(directive)
					|| isElifdefDirective(directive)
					|| isElifndefDirective(directive)
					|| isElseDirective(directive)
					|| isEndifDirective(directive)
					|| isEndrDirective(directive)
					|| directive.equals("binary")
					|| directive.equals("incbin")
					|| directive.equals("include")
					|| directive.equals("define");
		}

		private Optional<String> binaryDirective(String line, String directive) {
			var arg = conditionalExpr(line, directive);
			var filename = parseBinaryFilename(arg);
			if(filename.isEmpty()) {
				return Optional.empty();
			}

			try {
				var binary = readBinary(filename.get());
				if(binary.length == 0) {
					return Optional.empty();
				}

				var out = new StringBuilder();
				for(int i = 0; i < binary.length; i += 16) {
					var max = Math.min(i + 16, binary.length);
					out.append("defb ");
					for(int j = i; j < max; j++) {
						if(j > i) {
							out.append(',');
						}
						out.append(Byte.toUnsignedInt(binary[j]));
					}
					if(max < binary.length) {
						out.append(System.lineSeparator());
					}
				}
				return Optional.of(out.toString());
			}
			catch(IOException ioe) {
				throw new IllegalArgumentException("Could not read binary file '" + filename.get() + "'.", ioe);
			}
		}

		private Optional<String> parseBinaryFilename(String arg) {
			if(arg == null) {
				return Optional.empty();
			}
			var txt = arg.trim();
			if(!txt.startsWith("\"")) {
				return Optional.empty();
			}
			var end = txt.indexOf('"', 1);
			if(end < 0) {
				return Optional.empty();
			}
			var filename = txt.substring(1, end);
			var rest = txt.substring(end + 1);
			if(!onlyWhitespaceOrComment(rest)) {
				return Optional.empty();
			}
			return Optional.of(filename);
		}

		private boolean onlyWhitespaceOrComment(String txt) {
			for(int i = 0; i < txt.length(); i++) {
				char c = txt.charAt(i);
				if(Character.isWhitespace(c)) {
					continue;
				}
				return c == ';' || c == '\'';
			}
			return true;
		}

		private byte[] readBinary(String filename) throws IOException {
			if(resourceResolver.isPresent()) {
				var res = stack.peek();
				try {
					var resolved = resourceResolver.get().resolve(ResolveType.LIBRARIES, res.context(), "\"" + filename + "\"");
					return Files.readAllBytes(Path.of(resolved.uri()));
				}
				catch(IllegalArgumentException iae) {
					// Fall back to direct path probing below.
				}
			}

			var res = stack.peek();
			if(res.context() instanceof Path pctx) {
				var base = Files.isDirectory(pctx) ? pctx : pctx.getParent();
				var pth = base == null ? Path.of(filename) : base.resolve(filename);
				if(Files.exists(pth)) {
					return Files.readAllBytes(pth);
				}
				var cwdPath = Path.of(System.getProperty("user.dir")).resolve(filename);
				if(Files.exists(cwdPath)) {
					return Files.readAllBytes(cwdPath);
				}
			}

			return Files.readAllBytes(Path.of(filename));
		}

		private boolean ifExpr(String line, String directive) {
			var expr = conditionalExpr(line, directive);
			if(expr.isEmpty()) {
				return false;
			}
			stack.peek().conditions().push(new Condition(Long.parseLong(evalConstExpression(expr)) != 0L));
			return true;
		}

		private boolean elif(String line, String directive) {
			var expr = conditionalExpr(line, directive);
			if(expr.isEmpty()) {
				throw new IllegalArgumentException("Missing ELIF expression.");
			}
			return Long.parseLong(evalConstExpression(expr)) != 0L;
		}

		private boolean elifdef(String line, String directive) {
			var name = symbolFromDirective(line, directive);
			if(name == null) {
				throw new IllegalArgumentException("Missing ELIFDEF symbol.");
			}
			return defines.containsKey(name);
		}

		private boolean elifndef(String line, String directive) {
			var name = symbolFromDirective(line, directive);
			if(name == null) {
				throw new IllegalArgumentException("Missing ELIFNDEF symbol.");
			}
			return !defines.containsKey(name);
		}

		private boolean assertDirective(String line) {
			var body = conditionalExpr(line, "assert");
			if(body.isEmpty()) {
				return false;
			}

			var split = splitAssertArgs(body);
			var expr = split[0].trim();
			if(expr.isEmpty()) {
				return false;
			}

			if(Long.parseLong(evalConstExpression(expr)) != 0L) {
				return true;
			}

			var msg = split[1] == null ? "Assertion failed: " + expr : stripQuoted(split[1].trim());
			error(Error.ASSERT_FAILED, msg);
			return true;
		}

		private boolean deflDirective(String line) {
			Matcher m = DEFL_STD_PATTERN.matcher(line);
			if(!m.matches()) {
				return false;
			}
			return applyDefl(m.group(1), m.group(2));
		}

		private boolean deflDirectiveAlt(String line) {
			Matcher m = DEFL_ALT_PATTERN.matcher(line);
			if(!m.matches()) {
				return false;
			}
			return applyDefl(m.group(1), m.group(2));
		}

		private boolean localDirective(String line) {
			if(macroStack.isEmpty()) {
				return false;
			}
			var args = conditionalExpr(line, "local");
			if(args.isBlank()) {
				return false;
			}
			var parts = args.split(",");
			var macro = macroStack.peek();
			for(var part : parts) {
				var name = part.trim();
				if(name.isEmpty()) {
					return false;
				}
				macro.locals.put(name, name + "__" + macro.invocationId);
			}
			return true;
		}

		private boolean macroDirective(String line, String lower, IncludeContext<Object> res) {
			String name;
			String args;
			if(lower.startsWith("macro")) {
				var hdr = conditionalExpr(line, "macro");
				var parts = hdr.split("\\s+", 2);
				if(parts.length == 0 || parts[0].isBlank()) {
					return false;
				}
				name = parts[0].trim();
				args = parts.length > 1 ? parts[1].trim() : "";
			}
			else {
				var parts = line.trim().split("\\s+", 3);
				if(parts.length < 2 || !parts[1].equalsIgnoreCase("macro")) {
					return false;
				}
				name = parts[0].trim();
				args = parts.length > 2 ? parts[2].trim() : "";
			}

			if(name.isEmpty()) {
				return false;
			}

			var params = parseCommaArgs(args).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
			var body = readMacroBody(res);
			macros.put(name, new MacroDefinition(name, params, body));
			return true;
		}

		private boolean exitmDirective() {
			if(macroStack.isEmpty()) {
				return false;
			}
			macroStack.peek().exitRequested = true;
			return true;
		}

		private Optional<String> tryExpandMacroInvocation(String line) {
			var invocation = parseMacroInvocation(line);
			if(invocation.isEmpty()) {
				return Optional.empty();
			}

			var inv = invocation.get();
			var def = macros.get(inv.name);
			if(def == null) {
				return Optional.empty();
			}

			var args = parseCommaArgs(inv.args);
			if(args.size() != def.params.size()) {
				throw new IllegalArgumentException("Macro '" + def.name + "' expects " + def.params.size() + " argument(s).");
			}

			var paramMap = new HashMap<String, String>();
			for(int i = 0; i < def.params.size(); i++) {
				paramMap.put(def.params.get(i), args.get(i).trim());
			}

			var ctx = new MacroInvocationContext();
			macroStack.push(ctx);
			try {
				for(var bodyLine : def.body) {
					if(ctx.exitRequested) {
						break;
					}
					var expandedLine = substituteMacroLine(bodyLine, paramMap, ctx.locals);
					var out = process(expandedLine);
					out.ifPresent(pendingOutput::add);
				}
			}
			finally {
				macroStack.pop();
			}

			if(!pendingOutput.isEmpty()) {
				return Optional.of(pendingOutput.poll());
			}
			return Optional.empty();
		}

		private String substituteMacroLine(String line, Map<String, String> params, Map<String, String> locals) {
			var out = line;
			for(var e : params.entrySet()) {
				out = out.replaceAll("\\b" + Pattern.quote(e.getKey()) + "\\b", Matcher.quoteReplacement(e.getValue()));
			}
			for(var e : locals.entrySet()) {
				out = out.replaceAll("\\b" + Pattern.quote(e.getKey()) + "\\b", Matcher.quoteReplacement(e.getValue()));
			}
			return out;
		}

		private List<String> readMacroBody(IncludeContext<Object> res) {
			var body = new ArrayList<String>();
			var sourceIt = res.stream();
			int depth = 1;
			while(sourceIt.hasNext()) {
				var ln = sourceIt.next();
				consumeSourceLineAccounting(res, ln);
				var d = ln.trim().toLowerCase().split("\\s+")[0];
				if(d.equals("macro") || isAltMacroDirective(ln.trim().toLowerCase())) {
					depth++;
					body.add(ln);
				}
				else if(isEndmDirective(d)) {
					depth--;
					if(depth == 0) {
						return body;
					}
					body.add(ln);
				}
				else {
					body.add(ln);
				}
			}
			throw new IllegalArgumentException("Missing ENDM for macro block.");
		}

		private boolean isAltMacroDirective(String lowerLine) {
			var parts = lowerLine.trim().split("\\s+");
			return parts.length >= 2 && parts[1].equals("macro");
		}

		private Optional<MacroInvocation> parseMacroInvocation(String line) {
			var raw = line;
			var labelPrefix = "";
			var idxColon = raw.indexOf(':');
			if(idxColon >= 0) {
				labelPrefix = raw.substring(0, idxColon + 1);
				raw = raw.substring(idxColon + 1);
			}

			var trimmed = raw.stripLeading();
			if(trimmed.isEmpty()) {
				return Optional.empty();
			}
			var parts = trimmed.split("\\s+", 2);
			var name = parts[0];
			if(!macros.containsKey(name)) {
				return Optional.empty();
			}
			var args = parts.length > 1 ? parts[1] : "";
			if(!labelPrefix.isEmpty()) {
				pendingOutput.add(labelPrefix);
			}
			return Optional.of(new MacroInvocation(name, args));
		}

		private record MacroInvocation(String name, String args) {
		}

		private Optional<String> reptDirective(String line, IncludeContext<Object> res) {
			var expr = conditionalExpr(line, "rept");
			if(expr.isBlank()) {
				return Optional.empty();
			}
			var count = Long.parseLong(evalConstExpression(expr));
			if(count < 0) {
				return Optional.empty();
			}
			var block = readRepeatBlock(res);
			var expanded = new ArrayList<String>();
			for(int i = 0; i < count; i++) {
				expanded.addAll(block);
			}
			return expanded.isEmpty() ? Optional.empty() : Optional.of(String.join(System.lineSeparator(), expanded));
		}

		private Optional<String> reptcDirective(String line, IncludeContext<Object> res) {
			var args = splitTopLevelComma(conditionalExpr(line, "reptc"));
			if(args.size() != 2) {
				return Optional.empty();
			}
			var token = args.get(0).trim();
			if(token.isEmpty()) {
				return Optional.empty();
			}
			var values = resolveReptcValues(args.get(1));
			var block = readRepeatBlock(res);
			var expanded = new ArrayList<String>();
			for(var value : values) {
				for(var blockLine : block) {
					expanded.add(blockLine.replaceAll("\\b" + Pattern.quote(token) + "\\b", Matcher.quoteReplacement(value)));
				}
			}
			return expanded.isEmpty() ? Optional.empty() : Optional.of(String.join(System.lineSeparator(), expanded));
		}

		private Optional<String> reptiDirective(String line, IncludeContext<Object> res) {
			var args = splitTopLevelComma(conditionalExpr(line, "repti"));
			if(args.size() < 2) {
				return Optional.empty();
			}
			var token = args.get(0).trim();
			if(token.isEmpty()) {
				return Optional.empty();
			}
			var block = readRepeatBlock(res);
			var expanded = new ArrayList<String>();
			for(int i = 1; i < args.size(); i++) {
				var value = args.get(i).trim();
				for(var blockLine : block) {
					expanded.add(blockLine.replaceAll("\\b" + Pattern.quote(token) + "\\b", Matcher.quoteReplacement(value)));
				}
			}
			return expanded.isEmpty() ? Optional.empty() : Optional.of(String.join(System.lineSeparator(), expanded));
		}

		private List<String> readRepeatBlock(IncludeContext<Object> res) {
			var block = new ArrayList<String>();
			var sourceIt = res.stream();
			int depth = 1;
			while(sourceIt.hasNext()) {
				var ln = sourceIt.next();
				consumeSourceLineAccounting(res, ln);
				var lower = ln.trim().toLowerCase();
				var d = lower.split("\\s+")[0];
				if(d.equals("rept") || d.equals("reptc") || d.equals("repti")) {
					depth++;
					block.add(ln);
				}
				else if(isEndrDirective(d)) {
					depth--;
					if(depth == 0) {
						return block;
					}
					block.add(ln);
				}
				else {
					block.add(ln);
				}
			}
			throw new IllegalArgumentException("Missing ENDR for repeat block.");
		}

		private void consumeSourceLineAccounting(IncludeContext<Object> res, String line) {
			var inBytes = line.getBytes().length + LINESEP.length;
			res.originalLength().addAndGet(inBytes);
			res.originalLines().addAndGet(1);
			res.lineNumber().addAndGet(1);
		}

		private List<String> resolveReptcValues(String source) {
			var src = source.trim();
			if(src.isEmpty()) {
				return List.of();
			}
			String content;
			if((src.startsWith("\"") && src.endsWith("\"")) || (src.startsWith("'") && src.endsWith("'"))) {
				content = src.substring(1, src.length() - 1);
			}
			else {
				content = MacroExpander.expandLine(src, defines).trim();
			}
			var vals = new ArrayList<String>();
			for(int i = 0; i < content.length(); i++) {
				vals.add(String.valueOf(content.charAt(i)));
			}
			return vals;
		}

		private List<String> splitTopLevelComma(String text) {
			var args = new ArrayList<String>();
			boolean inSingle = false;
			boolean inDouble = false;
			int parenDepth = 0;
			int bracketDepth = 0;
			int start = 0;
			for(int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if(c == '\'' && !inDouble) {
					inSingle = !inSingle;
				}
				else if(c == '"' && !inSingle) {
					inDouble = !inDouble;
				}
				else if(!inSingle && !inDouble) {
					if(c == '(') {
						parenDepth++;
					}
					else if(c == ')') {
						parenDepth = Math.max(0, parenDepth - 1);
					}
					else if(c == '[') {
						bracketDepth++;
					}
					else if(c == ']') {
						bracketDepth = Math.max(0, bracketDepth - 1);
					}
					else if(c == ',' && parenDepth == 0 && bracketDepth == 0) {
						args.add(text.substring(start, i));
						start = i + 1;
					}
				}
			}
			args.add(text.substring(start));
			return args;
		}

		private List<String> parseCommaArgs(String text) {
			if(text == null || text.isBlank()) {
				return List.of();
			}
			return splitTopLevelComma(text).stream().map(String::trim).toList();
		}

		private boolean applyDefl(String name, String expr) {
			if(name == null || name.isBlank()) {
				return false;
			}

			var key = name.trim();
			var previous = Optional.ofNullable(defines.get(key)).orElse("");
			var valueExpr = expr == null ? "" : expr;
			var value = valueExpr.replaceAll("\\b" + Pattern.quote(key) + "\\b", Matcher.quoteReplacement(previous));
			defines.put(key, value);
			return true;
		}

		private String[] splitAssertArgs(String body) {
			boolean inSingle = false;
			boolean inDouble = false;
			int parenDepth = 0;
			int bracketDepth = 0;

			for(int i = 0; i < body.length(); i++) {
				char c = body.charAt(i);
				if(c == '\'' && !inDouble) {
					inSingle = !inSingle;
				}
				else if(c == '"' && !inSingle) {
					inDouble = !inDouble;
				}
				else if(!inSingle && !inDouble) {
					if(c == '(') {
						parenDepth++;
					}
					else if(c == ')') {
						parenDepth = Math.max(0, parenDepth - 1);
					}
					else if(c == '[') {
						bracketDepth++;
					}
					else if(c == ']') {
						bracketDepth = Math.max(0, bracketDepth - 1);
					}
					else if(c == ',' && parenDepth == 0 && bracketDepth == 0) {
						return new String[] { body.substring(0, i), body.substring(i + 1) };
					}
				}
			}
			return new String[] { body, null };
		}

		private String stripQuoted(String txt) {
			var t = txt.trim();
			if(t.length() >= 2 && ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))) {
				return t.substring(1, t.length() - 1);
			}
			return t;
		}

		private String conditionalExpr(String line, String directive) {
			var trimmed = line.trim();
			if(trimmed.length() <= directive.length()) {
				return "";
			}
			return trimmed.substring(directive.length()).trim();
		}

		private String symbolFromDirective(String line, String directive) {
			return extractName(directive.length(), line.trim());
		}

		private Optional<String> includeDirectiveIfEditorMode(String line, int stackSize) {
			if(mode == Mode.EDITOR && stack.size() <= stackSize) {
				/* In EDITOR mode, we output #include statements, but
				 * only in the top level (we do still continue pre-processing
				 * the include itself though)
				 */
				return Optional.of(line);
			}
			else {
				return Optional.empty();
			}
		}

		private String lineMarker(int lineNumber, String uri) {
			return String.format("#line %d \"%s\"", lineNumber, uri == null ? "(stdin)" : uri);
		}
		
		private boolean require(String line) {
			if(resourceResolver.isPresent()) {
				var defln = line.substring(9);
				var idx = defln.lastIndexOf('"');
				int sidx;
				
				String resname;
				
				if(idx == -1) {
					return false;
				}
				else {
					sidx = defln.indexOf('"');
					if(sidx == -1) {
						return false;
					}
					else {
						resname = defln.substring(sidx, idx + 1);
					}
				}
				
				var rr = resourceResolver.get();
				var res = stack.peek();
				res.runtimeModule().set(true);
				var rslv = rr.resolve(ResolveType.RUNTIME, res.context(), resname);
				
				if(included.contains(rslv.uri())) {
					return true;
				}
				
				stack.push(rslv);
				included.add(rslv.uri());
				return true;
			}
			else {
				throw new UnsupportedOperationException("No resource resolver, set one to resolve includes.");
			}
		}
		
		private boolean include(String line) {
			
			if(resourceResolver.isPresent()) {
				
				var defln = line.startsWith("#") ? line.substring(9) : line.substring(8);
				var idx = defln.lastIndexOf('>');
				int sidx;
				
				String resname;
				
				if(idx == -1) {
					idx = defln.lastIndexOf('"');
					if(idx == -1) {
						return false;				
					}
					else {
						sidx = defln.indexOf('"');
						if(sidx == -1) {
							return false;
						}
						else {
							resname = defln.substring(sidx, idx + 1);
						}
					}
				}
				else {
					sidx = defln.indexOf('<');
					if(sidx == -1) {
						return false;
					}
					else {
						resname = defln.substring(sidx, idx + 1);
					}
				}
				
				var opts = Arrays.asList(defln.substring(0, idx).toLowerCase().trim().split("\\s+"));
				
				var rr = resourceResolver.get();
				var res = stack.peek();
				
				try {
					var rslv = rr.resolve(res.runtimeModule().get() ? ResolveType.RUNTIME : ResolveType.LIBRARIES, res.context(), resname);
					
					if(opts.contains("once") && included.contains(rslv.uri())) {
						return true;
					}

					stack.push(rslv);
					included.add(rslv.uri());
				}
				catch(IllegalArgumentException iae) {
					error(Error.MISSING_INCLUDE, iae.getMessage());
				}
				return true;
			}
			else {
				throw new UnsupportedOperationException("No resource resolver, set one to resolve includes.");
			}
		}

		private boolean define(String line) {
			// DEFINE 
			var nameAndVal = line.substring(7);
			var valSepIdx = nameAndVal.indexOf('=');
			String val = "";
			String name = nameAndVal;
			if(valSepIdx > -1) {
				name = nameAndVal.substring(0, valSepIdx).trim();
				val = evalConstExpression(nameAndVal.substring(valSepIdx + 1).trim());
			}
			defines.put(name, val);
			return true;
		}

		private boolean defineMacro(String line) {
			/* NOTE: below is for benefit of boriel that sometimes seems to use _ separators for continuations! */
			var chars = line.substring(8).replace("_" + System.lineSeparator(), " ").trim().toCharArray();
			var notWs = notWs(chars, 0);
			if(notWs == -1) {
				return false;
			}
			else {
				var ws = ws(chars, notWs);
				/* This is the key */
				var key = new String(chars, 0, ws);
				var contentIdx = notWs(chars, ws);
				var value = contentIdx == -1 ? null : new String(chars, contentIdx, chars.length - contentIdx);
				
				if(defines.put(key, value) != null) {
					emitWarning(Warning.MACRO_REDEFINED, String.format("'%s' redefined.", key));
				};
			}
			return true;
		}
		
		private String extractName(int sidx, String line) {
			if(sidx > line.length())
				return null;
			var chars = line.substring(sidx).toCharArray();
			var notWs = notWs(chars, 0);
			if(notWs == -1) {
				return null;
			}
			else {
				var ws = ws(chars, notWs);
				return new String(chars, notWs, ws - notWs);
			}
		}

		private boolean pragma(String line) {
			onPragma.ifPresent(op -> op.accept(line.substring(8).split("\\s+")));
			return true;
		}

		private boolean ifndef(String line, String directive) {
			var name = symbolFromDirective(line, directive);
			if(name == null)
				return false;
			else {
				stack.peek().conditions().push(new Condition(!defines.containsKey(name)));
				return true;
			}
		}

		private boolean ifdef(String line, String directive) {
			var name = symbolFromDirective(line, directive);
			if(name == null)
				return false;
			else {
				stack.peek().conditions().push(new Condition(defines.containsKey(name)));
				return true;
			}
		}

		private boolean undefine(String line) {
			var name = extractName(9, line);
			if(name == null)
				return false;
			else {
				defines.remove(name);
				return true;
			}
		}

		private boolean undefMacro(String line) {
			var name = extractName(7, line);
			if(name == null)
				return false;
			else {
				macros.remove(name);
				return true;
			}
		}

		private void checkNext() {
			if(next == null) {
				
				while(!stack.isEmpty()) {
					var res = stack.peek();

					
					if(!rootMarkerOutput) {
						rootMarkerOutput = true;
						if(mode == Mode.COMPILER) {
							next = lineMarker(res.lineNumber().get() + 1, res.uri());
							adjustForNext(res, next);
							nextSegment(res);
							return;
						}
					}
					
					var sourceIt = res.stream();
					if(sourceIt instanceof SourceToTargetIterator)
						throw new UnsupportedOperationException("WTF");
				
					while(sourceIt.hasNext()) {
						
						var nextSourceLine = sourceIt.next();
						var inBytes = nextSourceLine.getBytes().length + LINESEP.length;
						
						res.originalLength().addAndGet(inBytes);
						res.originalLines().addAndGet(1);
						
						res.lineNumber().addAndGet(1);
						
						var trimmed = nextSourceLine.trim();
						
						if( lineContinuations.isPresent() && ( ( (buf.length() == 0 && trimmed.startsWith("#")) || buf.length() > 0) 
								&& nextSourceLine.endsWith(lineContinuations.get().toString()))) {
							buf.append(nextSourceLine.substring(0, nextSourceLine.length() - 1) + System.lineSeparator());
						}
						else {
//							if(buf.length() > 0)
//								buf.append(nextSourceLine.stripLeading());
//							else
								buf.append(nextSourceLine);
							
							var procesableLine = buf.toString();
							buf.setLength(0);
							
							try {
								var result = process(procesableLine);
								if(result.isPresent()) {
									next = result.get();
//									System.out.println("output: " + next);
									adjustForNext(res, next);
									break;
								}	
								else {
									var newRes = stack.peek();
									if(newRes != res) {
										/* If COMPILER mode, the start a new segment
										 * for the new context. In EDITOR mode, we
										 * won't be outputting anything from an included
										 * file, just processing its #defines. 
										 */
										if(mode == Mode.COMPILER) {
											try {
												next = lineMarker(res.lineNumber().get(), newRes.uri());									
												adjustForNext(newRes, next);
												break;
											}
											finally { 
												/* Started a new segment, commit the previous one  if there is content */
												nextSegment(res);
											}
										}
										
										res = newRes;
										sourceIt = res.stream();
									}
								}
							}
							catch(IllegalArgumentException iae) {
								throw new PreprocessorParseException(res.uri(), res.lineNumber().get(), iae.getMessage(), iae);
							}
						}
					}
					
					if(next == null) {

						if(mode == Mode.COMPILER || stack.size() == 1) {
							nextSegment(res);
								
						}
						res = stack.pop();
						
						if(mode == Mode.COMPILER && !stack.isEmpty()) {
							next = lineMarker(res.lineNumber().get(), stack.peek().uri());									
							adjustForNext(res, next);
							break;
						}
					}
					else
						break;
				}
			}
		}

		protected void adjustForNext(IncludeContext<Object> res, String txt) {
			var outlineBytes = txt.getBytes().length + LINESEP.length;
			res.preprocessedLength().addAndGet(outlineBytes);
			res.preprocessedLines().addAndGet(1);
		}

		protected void nextSegment(IncludeContext<Object> res) {
			var pplen = res.preprocessedLength().getAndSet(0);
			var pplines = res.preprocessedLines().getAndSet(0);
			if(pplen > 0) {
				if(sourceMap.isPresent()) {
					var sm = sourceMap.get();
					sm.addSegment(new Segment(
						res.originalOffset().get(), 
						res.originalLine().get(),
						res.originalLength().get(), 
						res.originalLines().get(),
						preprocessedOffset.get(), 
						preprocessedLine.get(),
						pplen,
						pplines,
						res.uri()));
				}
				res.originalOffset().addAndGet(res.originalLength().getAndSet(0));
				res.originalLine().addAndGet(res.originalLines().getAndSet(0));

				preprocessedOffset.addAndGet(pplen); 
				preprocessedLine.addAndGet(pplines);
			}
		}

		@Override
		public String next() {
			checkNext();
			try {
				return next;
			}finally {
				next = null;
			}
		}
	}
	
	private static int ws(char[] chars, int start) {
		for(var i = start ; i < chars.length; i++) {
			if(Character.isWhitespace(chars[i])) {
				return i;
			}
		}
		return chars.length;
	}
	
	private static String replaceWithSpaces(String line) {
		var c = line.toCharArray();
		var b = new StringBuilder();
		for(var a : c) {
			if(a == ' ' || a == '\t' || a == '\r' || a == '\n')
				b.append(a);
			else
				b.append(' ');
		}
		return b.toString();
	}

	private static int notWs(char[] chars, int start) {
		for(var i = start ; i < chars.length; i++) {
			if(!Character.isWhitespace(chars[i])) {
				return i;
			}
		}
		return -1;
	}
}
