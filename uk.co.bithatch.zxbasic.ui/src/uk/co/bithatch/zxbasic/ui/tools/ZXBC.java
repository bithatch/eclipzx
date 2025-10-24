package uk.co.bithatch.zxbasic.ui.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;

public class ZXBC extends AbstractTool {
	public static final int DEFAULT_HEAP_SIZE = 4768;
	
	public final static BorielZXBasicOutputFormat DEFAULT_FORMAT = BorielZXBasicOutputFormat.SNA;
	
	public enum Warning {
		DEFAULT_IMPLICIT_TYPE(100),
		SUPERFLUOUS_CONDITION(110),
		CONVERSION_LOSES_DIGITS(120),
		EMPTY_LOOP(130),
		EMPTY_IF(140),
		UNUSED_VARIABLE(150),
		FASTCALL_WITH_N_PARAMETERS(160),
		UNUSED_FUNCTION(170),
		UNREACHABLE_CODE(180),
		NO_RETURN(190),
		TRUNCATED_VALUE(200),
		UNKNOWN_PRAGMA(300);
		
		private int code;

		Warning(int code) {
			this.code = code;
		}
		
		public String description() {
			switch(this) {
			case DEFAULT_IMPLICIT_TYPE:
				return "Default implicit type used";
			case SUPERFLUOUS_CONDITION:
				return "Superfluous condition";
			case CONVERSION_LOSES_DIGITS:
				return "Conversion loses digits";
			case EMPTY_LOOP:
				return "Empty loop";
			case EMPTY_IF:
				return "Empty IF";
			case UNUSED_VARIABLE:
				return "Unused variable";
			case FASTCALL_WITH_N_PARAMETERS:
				return "FASTCALL with N parameters";
			case UNUSED_FUNCTION:
				return "Unused function";
			case NO_RETURN:
				return "No RETURN";
			case TRUNCATED_VALUE:
				return "Truncated value";
			case UNKNOWN_PRAGMA:
				return "Unknown pragma";
			default:
				return name();
			}
		}
		
		public int code() {
			return code;
		}
	}
	
	public final static class Builder {
		private Optional<File> outdir = Optional.empty();
		private Optional<File> workingdir = Optional.empty();
		private BorielZXBasicOutputFormat outputFormat = BorielZXBasicOutputFormat.ASM;
		private Optional<Boolean> autorun = Optional.empty();
		private Optional<Integer> optimizationLevel = Optional.empty();
		private Optional<Boolean> basicLoader = Optional.empty();
		private Optional<Consumer<ToolMessage>> errorHandler = Optional.empty();
		private int verbosity = 0;
		private final Map<String, String> defines = new  HashMap<>();
		private final Set<Path> includePaths = new  LinkedHashSet<>();
		private boolean memoryMap;
		private boolean updateFileTimes = true;
		private boolean strict = false;
		private boolean debugArrays = false;
		private boolean debugMemory= false;
		private boolean strictBoolean = false;
		private boolean ignoreVariableCase = false;
		private boolean legacyInstructions = false;
		private boolean explicitDeclaration = false;
		private int arrayBase;
		private int stringBase;
		private boolean breakDetection = false;
		private Optional<Integer> heapSize = Optional.empty();
		private Optional<Integer> heapAddress= Optional.empty();
		private Optional<Integer> orgAddress= Optional.empty();
		private final Path zxbcHome;
		private final Set<Warning> suppressedWarnings = new HashSet<ZXBC.Warning>(); 
		
		public Builder(Path zxbcHome) {
			this.zxbcHome =zxbcHome;
		}
		
		public Builder withSuppressedWarnings(Warning... suppressed) {
			return withSuppressedWarnings(Arrays.asList(suppressed));
		}
		
		public Builder withSuppressedWarnings(Collection<Warning> suppressed) {
			this.suppressedWarnings.addAll(suppressed);
			return this;
		}

		public Builder withHeapSize(int heapSize) {
			this.heapSize = Optional.of(heapSize);
			return this;
		}

		public Builder withHeapAddress(int heapAddress) {
			this.heapAddress = Optional.of(heapAddress);
			return this;
		}

		public Builder withOrgAddress(int orgAddress) {
			this.orgAddress = Optional.of(orgAddress);
			return this;
		}
		
		public Builder withBreakDetection() {
			return withBreakDetection(true);
		}
		
		public Builder withBreakDetection(boolean breakDetection) {
			this.breakDetection = breakDetection;
			return this;
		}
		
		public Builder withArrayBase(int arrayBase) {
			this.arrayBase = arrayBase;
			return this;
		}
		
		public Builder withStringBase(int stringBase) {
			this.stringBase = stringBase;
			return this;
		}

		public Builder withExplicitDeclaration() {
			return withExplicitDeclaration(true);
		}
		
		public Builder withExplicitDeclaration(boolean explicitDeclaration) {
			this.explicitDeclaration = explicitDeclaration;
			return this;
		}

		public Builder withLegacyInstructions() {
			return withLegacyInstructions(true);
		}
		
		public Builder withLegacyInstructions(boolean legacyInstructions) {
			this.legacyInstructions = legacyInstructions;
			return this;
		}

		public Builder withIgnoreVariableCase() {
			return withIgnoreVariableCase(true);
		}
		
		public Builder withIgnoreVariableCase(boolean ignoreVariableCase) {
			this.ignoreVariableCase = ignoreVariableCase;
			return this;
		}
		
		public Builder withStrictBoolean() {
			return withStrictBoolean(true);
		}
		
		public Builder withStrictBoolean(boolean strictBoolean) {
			this.strictBoolean = strictBoolean;
			return this;
		}

		public Builder withDebugArrays() {
			return withDebugArrays(true);
		}
		
		public Builder withDebugArrays(boolean debugArrays) {
			this.debugArrays = debugArrays;
			return this;
		}

		public Builder withDebugMemory() {
			return withDebugMemory(true);
		}
		
		public Builder withDebugMemory(boolean debugMemory) {
			this.debugMemory = debugMemory;
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
		
		public Builder withErrorHandler(Consumer<ToolMessage> errorHandler) {
			this.errorHandler = Optional.of(errorHandler);
			return this;
		}

		public Builder withoutUpdateFileTimes() {
			return withUpdateFileTimes(false);
		}
		
		public Builder withUpdateFileTimes(boolean updateFileTimes) {
			this.updateFileTimes = updateFileTimes;
			return this;
		}
		
		public Builder withAutorun(boolean autorun) {
			this.autorun = Optional.of(autorun);
			return this;
		}

		public Builder withStrict() {
			return withStrict(false);
		}
		
		public Builder withStrict(boolean strict) {
			this.strict = strict;
			return this;
		}
		
		public Builder withBasicLoader(boolean basicLoader) {
			this.basicLoader = Optional.of(basicLoader);
			return this;
		}
		
		public Builder withVerbosity(int verbosityh) {
			this.verbosity= verbosityh;
			return this;
		}
		
		public Builder withWorkingdir(File workingdir) {
			this.workingdir = Optional.of(workingdir);
			return this;
		}
		
		public Builder withOutdir(File outdir) {
			this.outdir = Optional.of(outdir);
			return this;
		}
		
		public Builder withOutputFormat(BorielZXBasicOutputFormat outputFormat) {
			this.outputFormat = outputFormat;
			return this;
		}
		
		public Builder withMemoryMap(boolean memoryMap) {
			this.memoryMap = memoryMap;
			return this;
		}
		
		public Builder withOptimizationLevel(int optimizationLevel) {
			this.optimizationLevel = Optional.of(optimizationLevel);
			return this;
		}
		
		public ZXBC build() {
			return new ZXBC(this);
		}
	}
	
	private final BorielZXBasicOutputFormat outputFormat;
	private final boolean autorun;
	private final boolean basicLoader;
	private final Optional<Consumer<ToolMessage>> errorHandler;
	private final Optional<File> outdir;
	private Optional<File> workingdir = Optional.empty();
	private final int verbosity;
	private final Map<String, String> defines;
	private final Set<Path> includePaths;
	private final boolean memoryMap;
	private final boolean strict;
	private final boolean updateFileTimes;
	private final Path zxbcHome;
	private final Optional<Integer> optimizationLevel;
	private final boolean debugArrays;
	private final boolean debugMemory;
	private final boolean strictBoolean;
	private final boolean ignoreVariableCase;
	private final boolean legacyInstructions;
	private final boolean explicitDeclaration;
	private final int arrayBase;
	private final int stringBase;
	private final boolean breakDetection;
	private final Optional<Integer> heapSize;
	private final Optional<Integer> heapAddress;
	private final Optional<Integer> orgAddress;
	private final Set<Warning> suppressedWarnings; 

	private ZXBC(Builder bldr) {

		defines = Collections.unmodifiableMap(new  HashMap<>(bldr.defines));
		includePaths = Collections.unmodifiableSet(new LinkedHashSet<>(bldr.includePaths));

		this.orgAddress = bldr.orgAddress;
		this.heapAddress = bldr.heapAddress;
		this.heapSize = bldr.heapSize;
		this.breakDetection = bldr.breakDetection;
		this.explicitDeclaration = bldr.explicitDeclaration;
		this.arrayBase = bldr.arrayBase;
		this.stringBase = bldr.stringBase;
		this.debugArrays = bldr.debugArrays;
		this.debugMemory = bldr.debugMemory;
		this.strictBoolean = bldr.strictBoolean;
		this.ignoreVariableCase = bldr.ignoreVariableCase;
		this.legacyInstructions = bldr.legacyInstructions;
		this.optimizationLevel = bldr.optimizationLevel;
		this.zxbcHome = bldr.zxbcHome;
		this.updateFileTimes = bldr.updateFileTimes;
		this.memoryMap = bldr.memoryMap;
		this.outputFormat = bldr.outputFormat;
		this.autorun = bldr.autorun.orElse(false) || this.outputFormat.snapshot();
		this.basicLoader = bldr.basicLoader.orElse(false) || this.outputFormat.snapshot();
		this.errorHandler = bldr.errorHandler;
		this.outdir = bldr.outdir;
		this.workingdir = bldr.workingdir;
		this.verbosity = bldr.verbosity;
		this.strict = bldr.strict;
		this.suppressedWarnings = Collections.unmodifiableSet(new HashSet<>(bldr.suppressedWarnings));
	}

	@Override
	public File targetFile(File srcfile) {
		return targetFile(srcfile, outdir.orElse(null), workingdir.orElse(null), outputFormat);
	}
	
	public static File targetFile(File srcfile, File outdir, BorielZXBasicOutputFormat outputFormat) {
		return targetFile(srcfile, outdir, null, outputFormat);
	}
	
	public static File targetFile(File srcfile, File outdir, File workingdir, BorielZXBasicOutputFormat outputFormat) {
		return FileNames.targetFile(srcfile, outdir, outputFormat.changeExtension(srcfile.getName()));
	}
	
	public File compile(File file) throws IOException {
		var args = new ArrayList<String>();
		var touch = new ArrayList<File>();

		args.add(Python.get().getInterpreter().toString());
		args.add(zxbcHome.toAbsolutePath().toString() + File.separator + "zxbc.py");
		
		if(autorun) {
			args.add("-a");
		}
		
		if(basicLoader) {
			args.add("-B");
		}
		
		for(int i = 0 ; i < verbosity ; i++) {
			args.add("-d");
		}
		
		args.add("-f");
		args.add(outputFormat.name().toLowerCase());
		
		defines.forEach((k,v) -> {
			args.add("-D");
			if(v == null || v.length() == 0)
				args.add(k);
			else
				args.add(k + "=" + v);
		});
		
		if(!includePaths.isEmpty()) {
			args.add("-I");
			args.add(String.join(":", includePaths.stream().map(Path::toString).toList()));
		}
		
		optimizationLevel.ifPresent(lvl -> {
			args.add("-O");
			args.add(String.valueOf(lvl));
		});
		
		if(breakDetection) {
			args.add("--enable-break");
		}
		
		orgAddress.ifPresent(v -> {
			args.add("--org");
			args.add(String.valueOf(v));
		});
		
		heapAddress.ifPresent(v -> {
			args.add("--heap-address");
			args.add(String.valueOf(v));
		});
		
		heapSize.ifPresent(v -> {
			args.add("--heap-size");
			args.add(String.valueOf(v));
		});
		
		if(strict) { 
			args.add("--strict");
		}
		
		if(debugArrays)
			args.add("--debug-array");
		
		if(debugMemory)
			args.add("--debug-memory");

		if(strictBoolean)
			args.add("--strict-bool");

		if(ignoreVariableCase)
			args.add("--ignore-case");

		if(legacyInstructions)
			args.add("--sinclair");

		if(explicitDeclaration) {
			args.add("--explicit");
		}

		if(arrayBase > 0) {
			args.add("--array-base");
			args.add(String.valueOf(arrayBase));
		}

		if(stringBase > 0) {
			args.add("--string-base");
			args.add(String.valueOf(stringBase));
		}
		
		suppressedWarnings.forEach(w -> { 
			args.add("-W"); 
			args.add(String.valueOf(w.code()));  
		});

		outdir.ifPresentOrElse(dir -> {

			var rel = workingdir.orElse(dir).toPath().relativize(file.toPath()).getParent();
			if(rel != null) {
				dir = new File(dir, rel.toString());
			}
			dir.mkdirs();
			
			if(memoryMap) {
				args.add("-M");
				var mmfile = new File(dir, FileNames.changeExtension(file.getName(), "map"));
				args.add(mmfile.getAbsolutePath());
				touch.add(mmfile);
			}
			
			args.add("-o");
			var outfile = new File(dir, outputFormat.changeExtension(file.getName()));
			args.add(outfile.getAbsolutePath());
			
			touch.add(outfile);
		}, () -> {

			if(memoryMap) {
				args.add("-M");
				var mmfile = new File(file.getParentFile(), FileNames.changeExtension(file.getName(), "map"));
				args.add(mmfile.getAbsolutePath());
				touch.add(mmfile);
			}
			touch.add(FileNames.changeExtension(file, outputFormat.name().toLowerCase()));
		});
		
		var  outfile = touch.getLast();
		
		args.add(file.getAbsolutePath().toString());
		
		var pbldr = new ProcessBuilder(args);
		workingdir.ifPresent(pbldr::directory);
		pbldr.redirectInput(Redirect.INHERIT);
		pbldr.redirectOutput(Redirect.INHERIT);
		var prc = pbldr.start();
		try(var rdr = new BufferedReader(new InputStreamReader(prc.getErrorStream()))) {
			String line = null;
			while( ( line = rdr.readLine() ) != null) {
				System.out.println(line);
				var eo = ToolMessage.parse(line);
				errorHandler.ifPresent(err ->  err.accept(eo));
			}
			var exit = prc.waitFor();
			if(exit > 0) {
				throw new IOException("Compiler exited with status " + exit);
			}
			
			var lastMod = file.lastModified();
			if(updateFileTimes) {
				touch.forEach(f -> {
					f.setLastModified(lastMod);
				});
			}
		}
		catch(InterruptedException ie) {
			throw new IOException("Interrupted.", ie);
		}
		
		return outfile;
	} 

	public static ZXBC create(File home) {
		return create(home.toPath());
	}
	
	public static ZXBC create(Path home) {
		return new Builder(home).build();
	}
}
