package uk.co.bithatch.zxbasic.ui.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import uk.co.bithatch.bitzx.FileNames;

public class ZXBPP extends AbstractTool {
	public static final int DEFAULT_HEAP_SIZE = 4768;
	
	public final static class Builder {
		private Optional<File> workingdir = Optional.empty();
		private Optional<Consumer<ToolMessage>> errorHandler = Optional.empty();
		private int verbosity = 0;
		private final Map<String, String> defines = new  HashMap<>();
		private final Set<Path> includePaths = new  LinkedHashSet<>();
		private final Path zxbcHome;
		private Optional<Integer>  expectWarnings = Optional.empty();
		
		public Builder(Path zxbcHome) {
			this.zxbcHome =zxbcHome;
		}
		
		public Builder withExpectWarnings(int expectWarnings) {
			this.expectWarnings = Optional.of(expectWarnings);
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

		public Builder withVerbosity(int verbosityh) {
			this.verbosity= verbosityh;
			return this;
		}
		
		public Builder withWorkingdir(File workingdir) {
			this.workingdir = Optional.of(workingdir);
			return this;
		}
		
		public ZXBPP build() {
			return new ZXBPP(this);
		}
	}
	
	private final Optional<Consumer<ToolMessage>> errorHandler;
	private final Optional<File> workingdir;
	private final int verbosity;
	private final Map<String, String> defines;
	private final Set<Path> includePaths;
	private final Path zxbcHome;
	private final Optional<Integer>  expectWarnings;

	private ZXBPP(Builder bldr) {

		defines = Collections.unmodifiableMap(new  HashMap<>(bldr.defines));
		includePaths = Collections.unmodifiableSet(new LinkedHashSet<>(bldr.includePaths));

		this.zxbcHome = bldr.zxbcHome;
		this.errorHandler = bldr.errorHandler;
		this.workingdir = bldr.workingdir;
		this.verbosity = bldr.verbosity;
		this.expectWarnings = bldr.expectWarnings;
	}


	@Override
	public File targetFile(File srcfile) {
		return null;
	}


	public Reader preprocess(Reader original) throws IOException {
		var args = new ArrayList<String>();

		buildArgs(args);

		var pbldr = new ProcessBuilder(args);
		workingdir.ifPresent(pbldr::directory);
		var prc = pbldr.start();
		
		new Thread(() -> {
			try(var rdr = new BufferedReader(new InputStreamReader(prc.getErrorStream()))) {
				String line = null;
				while( ( line = rdr.readLine() ) != null) {
					System.out.println(line);
					var eo = ToolMessage.parse(line);
					errorHandler.ifPresent(err ->  err.accept(eo));
				}
				
			}
			catch(IOException ie) {
				throw new UncheckedIOException(ie);
			}	
		}
		, "ZXBPPErrReader") {
		}.start();
		
		new Thread(() -> {
			try(var rdr = new BufferedReader(original)) {
				try(var wtr = new PrintWriter(new OutputStreamWriter(prc.getOutputStream()), true)) {
					
					/* TODO can't see an option to set defines */
					defines.forEach((k,v) -> {
						args.add("-D");
						if(v == null || v.length() == 0)
							wtr.format("#define %s%n", k);
						else
							wtr.format("#define %s %s%n", k, v);
					});
					
					rdr.transferTo(wtr);
				}
			}
			catch(IOException ie) {
				throw new UncheckedIOException(ie);
			}	
		}
		, "ZXBPPOutReader") {
		}.start();

		new Thread(() -> {
			try(var rdr = new BufferedReader(new InputStreamReader(prc.getErrorStream()))) {
				String line = null;
				while( ( line = rdr.readLine() ) != null) {
					System.out.println(line);
					var eo = ToolMessage.parse(line);
					errorHandler.ifPresent(err ->  err.accept(eo));
				}
				
			}
			catch(IOException ie) {
				throw new UncheckedIOException(ie);
			}	
		}
		, "ZXBPPErrReader") {
		}.start();
		
		return new InputStreamReader(prc.getInputStream()) {

			@Override
			public void close() throws IOException {
				try {
					super.close();
				}
				finally {
					try {
						var exit = prc.waitFor();
						if(exit > 0) {
							throw new IOException("Preprocessor exited with status " + exit);
						}
					}
					catch(InterruptedException ie) {
					}
				}
			}
			
		};
	}


	protected void buildArgs(ArrayList<String> args) throws IOException {
		args.add(FileNames.findCommand("python3", "pyton").toString());
		args.add(zxbcHome.toAbsolutePath().toString() + File.separator + "zxbpp.py");
		
		
		for(int i = 0 ; i < verbosity ; i++) {
			args.add("-d");
		}
		
		if(!includePaths.isEmpty()) {
			args.add("-I");
			args.add(String.join(":", includePaths.stream().map(Path::toString).toList()));
		}
		
		expectWarnings.ifPresent(ew -> {
			args.add("--expect-warnings");
			args.add(String.valueOf(ew));
		});
		
	} 

	public static ZXBPP create(File home) {
		return create(home.toPath());
	}
	
	public static ZXBPP create(Path home) {
		return new Builder(home).build();
	}
}
