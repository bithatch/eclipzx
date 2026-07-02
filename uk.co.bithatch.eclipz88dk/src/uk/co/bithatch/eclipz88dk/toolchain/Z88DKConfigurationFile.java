package uk.co.bithatch.eclipz88dk.toolchain;

import static java.nio.file.Files.newBufferedReader;
import static java.util.Collections.unmodifiableMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.ILog;

public class Z88DKConfigurationFile {
	private final static ILog LOG = ILog.of(Z88DKConfigurationFile.class);

	public enum CLibraryAttribute {
		COMPILER_INCLUDE_PATH(true, "-I", "-isystem"),
		ASSEMBLER_INCLUDE_PATH(true, "-Ca-I", "-Cl-I"),
		ALL_INCLUDE_PATH(true, "-I", "-isystem", "-Ca-I", "-Cl-I"),
		DEFINE(false, "-D", "-Ca-D", "-Cl-D");

		private final boolean pathAttribute;
		private final String[] prefixes;

		CLibraryAttribute(boolean pathAttribute, String... prefixes) {
			this.pathAttribute = pathAttribute;
			this.prefixes = prefixes;
		}

		public boolean pathAttribute() {
			return pathAttribute;
		}

		public String[] prefixes() {
			return prefixes;
		}
	}

	public enum Key {
		Z88MATHFLG, Z88MATHLIB, CRT0, CLIB, SUBTYPE, INCLUDE, OPTIONS, STARTUPLIB, GENMATHLIB, ALIAS, COPTRULESTARGET, WARNING
	}

	public record Entry(Key key, @Deprecated String... options) {
	}

	protected static void readEntries(Path file, Path base, Reader rdr, LinkedHashMap<Key, List<Entry>> entries)
			throws IOException {
		try (var br = rdr instanceof BufferedReader bbr ? bbr : new BufferedReader(rdr)) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();

				if (line.equals("") || line.startsWith("#"))
					continue;

				var args = line.split("\\s+");
				if (args.length < 2) {
					LOG.warn("Skipping line with no value: " + line + " in " + file);
					continue;
				}

				Key key;
				try {
					key = Key.valueOf(args[0]);
				} catch (IllegalArgumentException iae) {
					LOG.warn("Skipping invalid key " + args[0] + " [in " + file + "]");
					continue;
				}

				if (key == Key.WARNING) {
					LOG.warn(line.substring(7).trim() + " [in " + file + "]");
					continue;
				}
				else if (key == Key.INCLUDE) {
					var pbase = base.resolve(args[1]);
					try (var in = newBufferedReader(pbase)) {
						readEntries(pbase, pbase.getParent(), in, entries);
					} catch (IOException ioe) {
						throw new UncheckedIOException(ioe);
					}

				} else {

					var options = new String[args.length - 1];
					System.arraycopy(args, 1, options, 0, options.length);
					var entryList = entries.get(key);
					if (entryList == null) {
						entryList = new ArrayList<Z88DKConfigurationFile.Entry>();
						entries.put(key, entryList);
					}
					entryList.add(new Entry(key, options));
				}
			}
		}
	}
	
	private static void addCleanValue(Set<String> values, String value) {
		var clean = value;
		while (clean.startsWith("=")) {
			clean = clean.substring(1);
		}
		if (!clean.isBlank()) {
			values.add(clean);
		}
	}
	
	private static void collectValues(String[] options, CLibraryAttribute attribute, Set<String> values) {
		for (int i = 0; i < options.length; i++) {
			var option = options[i];
			for (var prefix : attribute.prefixes()) {
				if (option.equals(prefix)) {
					if (i + 1 < options.length) {
						addCleanValue(values, options[++i]);
					}
					break;
				}
				if (option.startsWith(prefix)) {
					addCleanValue(values, option.substring(prefix.length()));
					break;
				}
			}
		}
	}

	private static Map<Key, List<Entry>> read(Path file, Path base, Reader rdr) throws IOException {
		var entries = new LinkedHashMap<Key, List<Entry>>();
		readEntries(file, base, rdr, entries);
		return entries;
	}

	private final Map<Key, List<Entry>> entries;
	private final Path path;
	private final String name;

	public Z88DKConfigurationFile(Path path) {
		var pn = path.getFileName().toString();
		if (!pn.toLowerCase().endsWith(".cfg")) {
			throw new IllegalArgumentException("Configuration file must end with .cfg");
		}

		var nm = path.getFileName().toString();
		var idx = nm.lastIndexOf('.');

		name = idx == -1 ? nm : nm.substring(0, idx);

		this.path = path;

		try (var in = newBufferedReader(path)) {
			entries = unmodifiableMap(read(path, path.getParent(), in));
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	public Map<String, String> allDefines(String clib) {
		return Stream.concat(commonDefines().entrySet().stream(), definesForLibrary(clib).entrySet().stream())
				  .collect(Collectors.toMap(
						    Map.Entry::getKey, 
						    Map.Entry::getValue));
	}

	public Set<Path> assemblerPaths(String library) {
		return Stream.concat(
				allPaths(library, CLibraryAttribute.ALL_INCLUDE_PATH).stream(),
				allPaths(library, CLibraryAttribute.ASSEMBLER_INCLUDE_PATH).stream()
			).collect(Collectors.toSet());
	}

	public List<String> cLibraries() {
		return getValues(Key.CLIB).stream().map(l -> l.options()[0]).toList();
	}


	public Set<Path> compilerPaths(String library) {
		return Stream.concat(
				allPaths(library, CLibraryAttribute.ALL_INCLUDE_PATH).stream(),
				allPaths(library, CLibraryAttribute.COMPILER_INCLUDE_PATH).stream()
			).collect(Collectors.toSet());
	}

	public String[][] getAllCLibrariesAsOptions() {
		return cLibraries().stream().map(clib -> new String[] { clib, clib }).toList().toArray(new String[0][0]);
	}

	public String name() {
		return name;
	}

	public List<String> subtypes() {
		return getValues(Key.SUBTYPE).stream().map(l -> l.options()[0]).toList();
	}

	@Override
	public String toString() {
		return "Z88DKConfigurationFile [entries=" + entries + ", path=" + path + ", name=" + name + "]";
	}

	@Deprecated
	Optional<Entry> cLibraryFor(String name) {
		return getValues(Key.CLIB).stream().filter(e -> e.options()[0].equals(name)).findFirst();
	}

	@Deprecated
	Optional<Entry> getValue(Key key) {
		var l = entries.get(key);
		if (l == null) {
			return Optional.empty();
		} else {
			return Optional.of(l.get(0));
		}
	}

	private Set<Path> allPaths(String library, CLibraryAttribute attribute) {
		return Stream.concat(commonPaths(attribute).stream(),
				pathsForLibrary(library, attribute).stream()).collect(Collectors.toSet());
	}

	private Set<String> attributesForLibrary(String libraryName, CLibraryAttribute attribute) {
		return cLibraryFor(libraryName).map(entry -> {
			Set<String> values = new LinkedHashSet<String>();
			collectValues(entry.options(), attribute, values);
			return values;
		}).orElseGet(Collections::emptySet);
	}

	private List<String> attributesForOptions(CLibraryAttribute attribute) {
		Optional<Entry> opts = options();
		return opts.map(entry -> {
			var values = new LinkedHashSet<String>();
			collectValues(entry.options(), attribute, values);
			return List.copyOf(values);
		}).orElseGet(Collections::emptyList);
	}

	private Map<String, String> commonDefines() {
		List<String> defines = attributesForOptions(CLibraryAttribute.DEFINE);
		return defines.stream().collect(Collectors.toMap(str -> {
			var idx = str.indexOf('=');
			return idx == -1 ? str : str.substring(0, idx);
		}, str -> {
			var idx = str.indexOf('=');
			return idx == -1 ? "" : str.substring(idx + 1);
		}));
	}
	
	private List<Path> commonPaths(CLibraryAttribute attribute) {
		if (!attribute.pathAttribute()) {
			throw new IllegalArgumentException("Attribute " + attribute + " is not a path attribute");
		}
		return attributesForOptions(attribute).stream().map(this::resolveFromSDKRoot).toList();
	}

	private Map<String, String> definesForLibrary(String clib) {
		return attributesForLibrary(clib, CLibraryAttribute.DEFINE).stream().collect(Collectors.toMap(str -> {
			var idx = str.indexOf('=');
			return idx == -1 ? str : str.substring(0, idx);
		}, str -> {
			var idx = str.indexOf('=');
			return idx == -1 ? "" : str.substring(idx + 1);
		}));
	}

	private List<Entry> getValues(Key key) {
		var l = entries.get(key);
		return l == null ? Collections.emptyList() : l;
	}

	private Optional<Entry> options() {
		return getValues(Key.OPTIONS).stream().findFirst();
	}

	private List<Path> pathsForLibrary(String libraryName, CLibraryAttribute attribute) {
		if (!attribute.pathAttribute()) {
			throw new IllegalArgumentException("Attribute " + attribute + " is not a path attribute");
		}
		return attributesForLibrary(libraryName, attribute).stream().map(this::resolveFromSDKRoot).toList();
	}

	private Path resolveFromSDKRoot(String pathValue) {
		var sdkRoot = sdkRoot();
		if (pathValue.equals("DESTDIR")) {
			return sdkRoot;
		}
		var normalized = pathValue.startsWith("DESTDIR/") ? pathValue.substring("DESTDIR/".length()) : pathValue;
		var parsed = Path.of(normalized);
		if (parsed.isAbsolute()) {
			return parsed.normalize();
		}
		return sdkRoot.resolve(parsed).normalize();
	}

	private Path sdkRoot() {
		var parent = path.getParent();
		if (parent == null || parent.getParent() == null || parent.getParent().getParent() == null) {
			throw new IllegalStateException("Cannot infer SDK root from configuration path " + path);
		}
		return parent.getParent().getParent();
	}
}
