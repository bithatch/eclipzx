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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import uk.co.bithatch.bitzx.IOutputFormat;

public class Z88DKConfigurationFile {

	public enum Key {
		Z88MATHFLG, Z88MATHLIB, CRT0, CLIB, SUBTYPE, INCLUDE, OPTIONS, STARTUPLIB, GENMATHLIB, ALIAS, COPTRULESTARGET
	}

	public record Entry(Key key, String value, String... options) {
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
			entries = unmodifiableMap(read(path.getParent(), in));
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	public String name() {
		return name;
	}

	public Entry get(Key key) {
		return getValue(key).orElseThrow(() -> new IllegalArgumentException("No configuration with key " + key));
	}

	public Optional<Entry> getValue(Key key) {
		var l = entries.get(key);
		if (l == null) {
			return Optional.empty();
		} else {
			return Optional.of(l.get(0));
		}
	}

	public List<Entry> getValues(Key key) {
		var l = entries.get(key);
		return l == null ? Collections.emptyList() : l;
	}

	@Override
	public String toString() {
		return "Z88DKConfigurationFile [entries=" + entries + ", path=" + path + ", name=" + name + "]";
	}

	private static Map<Key, List<Entry>> read(Path base, Reader rdr) throws IOException {
		var entries = new LinkedHashMap<Key, List<Entry>>();
		readEntries(base, rdr, entries);
		return entries;
	}

	protected static void readEntries(Path base, Reader rdr, LinkedHashMap<Key, List<Entry>> entries)
			throws IOException {
		try (var br = rdr instanceof BufferedReader bbr ? bbr : new BufferedReader(rdr)) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();

				if (line.equals("") || line.startsWith("#"))
					continue;

				var args = line.split("\\s+");
				if (args.length < 2) {
					System.out.println("Skipping line with no value: " + line);
					continue;
				}

				Key key;
				try {
					key = Key.valueOf(args[0]);
				} catch (IllegalArgumentException iae) {
					System.out.println("Skipping invalid key " + args[0]);
					continue;
				}

				if (key == Key.INCLUDE) {
					var pbase = base.resolve(args[1]);
					try (var in = newBufferedReader(pbase)) {
						readEntries(pbase.getParent(), in, entries);
					} catch (IOException ioe) {
						throw new UncheckedIOException(ioe);
					}

				} else {

					var options = new String[args.length - 2];
					System.arraycopy(args, 2, options, 0, options.length);
					var entryList = entries.get(key);
					if (entryList == null) {
						entryList = new ArrayList<Z88DKConfigurationFile.Entry>();
						entries.put(key, entryList);
					}
					entryList.add(new Entry(key, args[1], options));
				}
			}
		}
	}

	public Optional<Entry> cLibraryFor(String name) {
		return getValues(Key.CLIB).stream().filter(e -> e.value().equals(name)).findFirst();
	}

	public List<String> cLibraries() {
		return getValues(Key.CLIB).stream().map(l -> l.value()).toList();
	}

	public String[][] getAllCLibrariesAsOptions() {
		return cLibraries().stream().map(clib -> new String[] { clib, clib }).toList().toArray(new String[0][0]);
	}

	public List<String> subtypes() {
		return getValues(Key.SUBTYPE).stream().map(l -> l.value()).toList();
	}
}
