package uk.co.bithatch.zxbasic.preprocessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import uk.co.bithatch.zxbasic.preprocessor.SourceMap.Segment;
import uk.co.bithatch.zxbasic.tools.AbstractTool;

public class ZXPreprocessor extends AbstractTool {
	
	public enum Mode {
		COMPILER, EDITOR
	}
	
	public final static class FileSystemResourceResolver implements ResourceResolver<Path> {

		public final static class Builder {

			private Optional<Path> workingdir = Optional.empty();
			private Optional<Path> runtimedir = Optional.empty();
			private final Set<Path> includePaths = new  LinkedHashSet<>();
			
			public Builder addIncludes(String... includePaths) {
				return addIncludes(Arrays.asList(includePaths));
			}
			
			public Builder addIncludes(Collection<String> includePaths) {
				return addIncludePaths(includePaths.stream().map(Paths::get).toList());
			}

			public Builder withIncludes(String... includePaths) {
				return withIncludes(Arrays.asList(includePaths));
			}
			
			public Builder withIncludes(Collection<String> includePaths) {
				return withIncludePaths(includePaths.stream().map(Paths::get).toList());
			}
			
			public Builder addIncludePaths(Collection<Path> includePaths) {
				this.includePaths.addAll(includePaths);
				return this;
			}
			
			public Builder addIncludePaths(Path... includePaths) {
				return addIncludePaths(Arrays.asList(includePaths));
			}

			public Builder withIncludePaths(Path... includePaths) {
				return withIncludePaths(Arrays.asList(includePaths));
			}
			
			public Builder withIncludePaths(Collection<Path> includePaths) {
				this.includePaths.clear();
				addIncludePaths(includePaths);
				return this;
			}
			
			public Builder addIncludeDirs(File... includePaths) {
				return addIncludeDirs(Arrays.asList(includePaths));
			}
			
			public Builder addIncludeDirs(Collection<File> includePaths) {
				return addIncludePaths(includePaths.stream().map(File::toPath).toList());
			}

			public Builder withIncludeDirs(File... includePaths) {
				return withIncludeDirs(Arrays.asList(includePaths));
			}
			
			public Builder withIncludeDirs(Collection<File> includePaths) {
				return withIncludePaths(includePaths.stream().map(File::toPath).toList());
			}
			
			public Builder withWorkingDir(File workingdir) {
				return withWorkingDir(workingdir.toPath()); 
			}
			
			public Builder withWorkingDir(Path workingdir) {
				this.workingdir = Optional.of(workingdir);
				return this;
			}
			
			public Builder withRuntimeDir(File workingdir) {
				return withRuntimeDir(workingdir.toPath()); 
			}
			
			public Builder withRuntimeDir(Path runtimedir) {
				this.runtimedir = Optional.of(runtimedir);
				return this;
			}
			
			public FileSystemResourceResolver build() {
				return new FileSystemResourceResolver(this);
			}
		}
		
		private final Set<Path> includePaths;
		private final Path workingdir;
		private final Optional<Path> runtimedir;
		
		private FileSystemResourceResolver(Builder bldr) {
	    	includePaths = new LinkedHashSet<>(bldr.includePaths);
	    	workingdir = bldr.workingdir.orElseGet(() -> Paths.get(System.getProperty("user.dir")));
	    	runtimedir = bldr.runtimedir;
			
		}

		@Override
		public IncludeContext<Path> resolve(ResolveType resolveType, Path context, String name) {
			var oname = name;
			
			if(resolveType == ResolveType.RUNTIME && runtimedir.isPresent()) {
				name = name.substring(1, name.length() - 1);
				var lib = runtimedir.get().resolve(name);
				if(Files.exists(lib)) {
					return new IncludeContext<>(runtimedir.get(), 
							lib.toAbsolutePath().toString(), 
							new ReaderIterator(null), new AtomicBoolean(true));
				}
			}
			else {
				if(name.startsWith("<") && name.endsWith(">")) {
					name = name.substring(1, name.length() - 1);
					for(var includePath : includePaths) {
						var lib = includePath.resolve(name);
						if(Files.exists(lib)) {
							try {
								return new IncludeContext<>(includePath, lib.toAbsolutePath().toString(), Files.readAllLines(lib).stream().iterator());
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							} 
						}
					}
				}
				else if(name.startsWith("\"") && name.endsWith("\"")) {
					name = name.substring(1, name.length() - 1);
					
					Path nctx;
					if(context == null) {
						nctx = workingdir;
					}
					else {
						if(Files.isDirectory(context))
							nctx = context;
						else
							nctx = context.getParent();
					}
					
					var lib = nctx.resolve(name);
					
					if(Files.exists(lib)) {
						try {
							return new IncludeContext<>(nctx, lib.toAbsolutePath().toString(), Files.readAllLines(lib).stream().iterator());
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						} 
					}
				}
			}

			if(oname.startsWith("<"))
				throw new IllegalArgumentException("The include " + name + " does not exist in the include search path " + String.join(File.pathSeparator, includePaths.stream().map(Path::toString).toList()) + ".");
			else {
				if(context == null)
					throw new IllegalArgumentException("The include " + name + " does not exist in the current working directory.");
				else
					throw new IllegalArgumentException("The include " + name + " does not exist in " + context + ".");
			}
		}
		
	}
	

	/**
	 * Builder
	 */
	public final static class Builder extends AbstractBuilder<Builder, ZXPreprocessor> {

		private final Map<String, String> defines = new  HashMap<>();
		private final Set<Warning> suppressedWarnings = new HashSet<Warning>(); 
		private Optional<BiConsumer<Warning, String>> onWarning = Optional.empty(); 
		private Optional<BiConsumer<Error, String>> onError = Optional.empty(); 
		private Optional<Consumer<String[]>> onPragma = Optional.empty();
		private Optional<ResourceResolver<Object>> resourceResolver = Optional.empty();
		private Optional<SourceMap> sourceMap = Optional.empty();
		private boolean expandRequire = false;
		private Mode mode= Mode.COMPILER;

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
		
		public ZXPreprocessor build() {
			return new ZXPreprocessor(this);
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
    
    private ZXPreprocessor(Builder bldr) {
    	super(bldr);
    	
		suppressedWarnings = Collections.unmodifiableSet(new HashSet<>(bldr.suppressedWarnings));
		resourceResolver = bldr.resourceResolver;

		onWarning = bldr.onWarning;
		onError = bldr.onError;
		onPragma = bldr.onPragma;
		sourceMap = bldr.sourceMap;
		expandRequire = bldr.expandRequire;
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
    		pwtr.println(s);
    	});
    }
    
	protected Stream<String> doRun(Iterator<String> root, Object context) {
		var sit = new SourceToTargetIterator(context, root);
		Iterable<String> iterable = () -> sit;
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	private final class SourceToTargetIterator implements Iterator<String> {
		private String next;
		private StringBuilder buf = new StringBuilder();
		private Set<String> included = new HashSet<>();
		private Stack<IncludeContext<Object>> stack = new Stack<>();
		private AtomicInteger preprocessedOffset = new AtomicInteger();
		private AtomicInteger preprocessedLine = new AtomicInteger();
		
		private final static byte[] LINESEP = System.lineSeparator().getBytes();
		
		private boolean rootMarkerOutput;

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
			
			if(lower.startsWith("#else")) {
				if(res.conditions().isEmpty() || res.conditions().peek().inElse)
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
				else {
					res.conditions().peek().inElse = true;
					return includeDirectiveIfEditorMode(line, 1);
				}
			}
			else if(lower.startsWith("#endif")) {
				if(res.conditions().isEmpty())
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
				else {
					res.conditions().pop();
				}
				return includeDirectiveIfEditorMode(line, 1);
			}
			else if(!res.conditions().isEmpty()) {
				var cond = res.conditions().peek();
				if(!cond.matches())
					if(line.startsWith("#"))
						return includeDirectiveIfEditorMode(line, 1);
					else if(mode == Mode.EDITOR) {
						return Optional.of(replaceWithSpaces(line));
					}
					else {
						return Optional.empty();
					}
			}

			
			
			if(lower.startsWith("#define")) {
				if(define(line)) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
				}
			}
			else if(lower.startsWith("#undef ")) {
				if(undef(line)) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
				}
			}
			else if(lower.startsWith("#pragma ")) {
				if(pragma(line)) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
				}
			}
			else if(lower.startsWith("#ifndef ")) {
				if(ifndef(line)) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
				}
			}
			else if(lower.startsWith("#ifdef ")) {
				if(ifdef(line)) {
					return includeDirectiveIfEditorMode(line, 1);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
				}
			}
			else  if(lower.startsWith("#error ")) {
				onError.ifPresent(oe -> oe.accept(Error.ERROR_DIRECTIVE, line.substring(7).trim()));
				return includeDirectiveIfEditorMode(line, 1);
			}
			else if(lower.startsWith("#warning ")) {
				onWarning.ifPresent(ow -> ow.accept(Warning.WARNING_DIRECTIVE, line.substring(9).trim()));
				return includeDirectiveIfEditorMode(line, 1);
			}
			else if(lower.startsWith("#require ")) {
				if(expandRequire) {
					if(require(line))
						return Optional.empty();
					else {
						error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
					}
				}
				else {
					return includeDirectiveIfEditorMode(line, 1);
				}
			}
			else if(lower.startsWith("#include ")) {
				if(include(line)) {
					return includeDirectiveIfEditorMode(line, 2);
				}
				else {
					error(Error.SYNTAX_ERROR,"Syntax error " + line + ".");
				}
			}
			else if (line.startsWith("#")) {

				// TODO
				
//	    	    ELIF = "ELIF"
//	    	    IF = "IF"   (AND ALL CONDITIONS)
//	    	    INIT = "INIT"
//	    	    LINE = "LINE"
//	    	    REQUIRE = "REQUIRE"
				
				onWarning.ifPresent(ow -> ow.accept(Warning.UNKNOWN_PREPROCESSOR_DIRECTIVE, "Unknown preprocessor directive " + line + "."));
				return includeDirectiveIfEditorMode(line, 1);
			}
			else {
				if(mode == Mode.COMPILER || stack.size() == 1) {
					
					/* Only actually produce any output if we are in 
					 * COMPILER mode, or if this is the root source with EDITOR
					 * mode.
					 */
					
					var lastLine = res.lastPrintedLine().get();
					var dif = thisLineNo - lastLine - 1;
					if(dif > 0) {
//						System.out.println("Offset " + dif);
						nextSegment(res);
					}
					
//					if(mode == Mode.EDITOR)
//						// TODO not really sure ... macro expansion is the last knotty problem
//						return Optional.of(line);
//					else
						return Optional.of(MacroExpander.expandLine(line, defines));
				}
				else {
					return Optional.empty();
				}
			}
			return Optional.of(line);
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
				
				var defln = line.substring(9);
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
			var chars = line.substring(8).replace("_" + System.lineSeparator(), " ").toCharArray();
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
					if(!suppressedWarnings.contains(Warning.MACRO_REDEFINED))
						onWarning.ifPresent(ow -> ow.accept(Warning.MACRO_REDEFINED, String.format("'%s' redefined.", key)));
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
				return new String(chars, 0,  ws(chars, notWs));
			}
		}

		private boolean pragma(String line) {
			onPragma.ifPresent(op -> op.accept(line.substring(8).split("\\s+")));
			return true;
		}

		private boolean ifndef(String line) {
			var name = extractName(8, line);
			if(name == null)
				return false;
			else {
				stack.peek().conditions().push(new Condition(!defines.containsKey(name)));
				return true;
			}
		}

		private boolean ifdef(String line) {
			var name = extractName(7, line);
			if(name == null)
				return false;
			else {
				stack.peek().conditions().push(new Condition(defines.containsKey(name)));
				return true;
			}
		}

		private boolean undef(String line) {
			var name = extractName(7, line);
			if(name == null)
				return false;
			else {
				defines.remove(name);
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
						
						if( ( (buf.length() == 0 && trimmed.startsWith("#")) || buf.length() > 0) 
								&& nextSourceLine.endsWith("\\")) {
							buf.append(nextSourceLine + System.lineSeparator());
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
