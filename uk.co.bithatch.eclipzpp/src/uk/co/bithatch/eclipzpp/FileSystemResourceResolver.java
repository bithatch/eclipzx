package uk.co.bithatch.eclipzpp;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FileSystemResourceResolver implements ResourceResolver<Path> {

	public final static class Builder {

		private Optional<Path> workingdir = Optional.empty();
		private Optional<Path> runtimedir = Optional.empty();
		private final Set<Path> includePaths = new  LinkedHashSet<>();
		private boolean alwaysSearchPaths = true;

		public Builder withAlwaysSearchPaths() {
			return withAlwaysSearchPaths(true);
		}
		
		public Builder withAlwaysSearchPaths(boolean alwaysSearchPaths) {
			this.alwaysSearchPaths = alwaysSearchPaths;
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
	private final boolean alwaysSearchPaths;
	private final Optional<Path> runtimedir;
	
	private FileSystemResourceResolver(Builder bldr) {
    	includePaths = new LinkedHashSet<>(bldr.includePaths);
    	workingdir = bldr.workingdir.orElseGet(() -> Paths.get(System.getProperty("user.dir")));
    	runtimedir = bldr.runtimedir;
    	alwaysSearchPaths = bldr.alwaysSearchPaths;
    	
		
	}

	@Override
	public IncludeContext<Path> resolve(ResolveType resolveType, Path context, String name) {
		var oname = name;
		
		if(resolveType == ResolveType.RUNTIME && runtimedir.isPresent()) {
			name = name.substring(1, name.length() - 1);
			var lib = runtimedir.get().resolve(name);
			if(isValid(lib)) {
				return new IncludeContext<>(runtimedir.get(), 
						lib.toAbsolutePath().toString(), 
						new ReaderIterator(null), new AtomicBoolean(true));
			}
		}
		else {
			if(name.startsWith("<") && name.endsWith(">")) {
				name = name.substring(1, name.length() - 1);
				var ctx = searchPaths(name);
				if(ctx != null) {
					return ctx;
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
				
				if(isValid(lib)) {
					try {
						return new IncludeContext<>(nctx, lib.toAbsolutePath().toString(), new ReaderIterator(new InputStreamReader(Files.newInputStream(lib))));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					} 
				}
				else if(alwaysSearchPaths) {
					var ctx = searchPaths(name);
					if(ctx != null) {
						return ctx;
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

	private boolean isValid(Path lib) {
		return Files.exists(lib) && !Files.isDirectory(lib);
	}

	private IncludeContext<Path> searchPaths(String name) {
		for(var includePath : includePaths) {
			var lib = includePath.resolve(name);
			if(isValid(lib)) {
				try {
					return new IncludeContext<>(includePath, lib.toAbsolutePath().toString(), new ReaderIterator(new InputStreamReader(Files.newInputStream(lib))));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} 
			}
		}
		return null;
	}
	
}