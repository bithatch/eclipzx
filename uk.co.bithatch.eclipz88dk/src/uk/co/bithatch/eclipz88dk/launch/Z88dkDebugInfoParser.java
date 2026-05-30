package uk.co.bithatch.eclipz88dk.launch;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.bitzx.ISourceAdressMap;
import uk.co.bithatch.bitzx.SourceLocation;

public class Z88dkDebugInfoParser implements ISourceAdressMap {

	private static final ILog LOG = ILog.of(Z88dkDebugInfoParser.class);
	private static final int FUZZY_SEARCH_WINDOW = 10;

	private final Map<SourceLocation, Integer> lineToAddress = new HashMap<>();
	/** Absolute address → source location */
	private final NavigableMap<Integer, SourceLocation> addressToLine = new TreeMap<>();

	/** Symbol name → address from .map file */
	private final Map<String, Integer> symbols = new HashMap<>();

	/** Record known offsets deduced from #line directives and fuzzy breakpoint matching. fileName -> (C_LINE -> offset) */
	private final Map<String, NavigableMap<Integer, Integer>> learnedOffsets = new HashMap<>();

	/**
	 * Parse debug information from the .map file and .c.asm files.
	 * 
	 * @param binaryPath Path to the binary file (e.g. .zx0 or .zx7 file)
	 */
	public void parse(Path binaryPath) {
		if (binaryPath == null) {
			LOG.warn("No binary path provided for debug info parsing");
			return;
		}

		var dir = binaryPath.getParent();
		var baseName = stripExtension(binaryPath.getFileName().toString());

		var mapFile = dir.resolve(baseName + ".map");
		if (Files.exists(mapFile)) {
			LOG.info("Parsing map file: " + mapFile);
			parseMapFile(mapFile);
		} else {
			LOG.warn("No .map file found at: " + mapFile);
		}

		findAndParseCasmFiles(dir);
		var projectDir = dir.getParent();
		if (projectDir != null && !projectDir.equals(dir)) {
			findAndParseCasmFiles(projectDir);
		}

		LOG.info("Debug info parsed: " + lineToAddress.size() + " line mappings, "
				+ symbols.size() + " symbols");
	}

	private static final Pattern C_LINE_DIRECTIVE = Pattern.compile(
			"^\\s*C_LINE\\s+(\\d+)\\s*,\\s*\"(.+?)\"\\s*$");
	private static final Pattern LABEL_DEF = Pattern.compile(
			"^\\.(\\w+)\\s*$");
	private static final Pattern HASH_LINE_DIRECTIVE = Pattern.compile(
			"^;#line\\s+(\\d+)\\s+\"(.+?)\"\\s*$");
	private static final Pattern ASM_INSTRUCTION = Pattern.compile(
			"^\\s+(\\w+)\\b.*$");

	private void findAndParseCasmFiles(Path dir) {
		try {
			try (var stream = Files.list(dir)) {
				stream.filter(p -> p.toString().endsWith(".c.asm"))
					  .filter(Files::isRegularFile)
					  .forEach(this::parseCasmFile);
			}
		} catch (IOException e) {
			LOG.error("Failed to scan for .c.asm files in " + dir, e);
		}
	}

	private void parseCasmFile(Path casmFile) {
		LOG.info("Parsing .c.asm file: " + casmFile);

		var clines = new ArrayList<int[]>(); 
		var labels = new TreeMap<Integer, String>(); 
		var fileNames = new ArrayList<String>();
		var fileNameIndex = new HashMap<String, Integer>();
		var instrCountAtLine = new TreeMap<Integer, Integer>(); 
		var labelForLine = new TreeMap<Integer, String>(); // tracks which label is "active" at each line

		var pendingHashLine = new HashMap<String, Integer>(); // normFile → original line from most recent #line

		int asmLineNum = 0;
		int instrCountSinceLabel = 0;
		String currentLabel = null;

		try (BufferedReader reader = Files.newBufferedReader(casmFile)) {
			String line;
			while ((line = reader.readLine()) != null) {
				asmLineNum++;

				var hlMatch = HASH_LINE_DIRECTIVE.matcher(line);
				if (hlMatch.matches()) {
					continue;
				}

				var clMatch = C_LINE_DIRECTIVE.matcher(line);
				if (clMatch.matches()) {
					int srcLine = Integer.parseInt(clMatch.group(1));
					var rawPath = clMatch.group(2);
					/* Strip scope info: main.c::main::0::1 → main.c */
					var colonColon = rawPath.indexOf("::");
					var filePath = colonColon >= 0 ? rawPath.substring(0, colonColon) : rawPath;
					var normName = normaliseFileName(filePath);

					if (normName.endsWith(".c") && srcLine > 0) {
						/* If we just saw a #line directive, seed our offset map */
						if (pendingHashLine.containsKey(normName)) {
							int origLine = pendingHashLine.remove(normName);
							/* #line N means next line is N. The current srcLine is the C_LINE.
							 * So offset = origLine - srcLine. */
							int offset = origLine - srcLine;
							var offsets = learnedOffsets.computeIfAbsent(normName, k -> new TreeMap<>());
							offsets.put(srcLine, offset);
							LOG.info("Learned initial baseline offset for " + normName + " at C_LINE " + srcLine + ": +" + offset + " (from #line " + origLine + ")");
						}

						if (!fileNameIndex.containsKey(normName)) {
							fileNameIndex.put(normName, fileNames.size());
							fileNames.add(normName);
						}
						clines.add(new int[]{ fileNameIndex.get(normName), srcLine, asmLineNum });
						instrCountAtLine.put(asmLineNum, instrCountSinceLabel);
						if (currentLabel != null) {
							labelForLine.put(asmLineNum, currentLabel);
						}
					}
					continue;
				}

				var labelMatch = LABEL_DEF.matcher(line);
				if (labelMatch.matches()) {
					var labelName = labelMatch.group(1);
					labels.put(asmLineNum, labelName);
					var addr = symbols.get(labelName);
					if (addr == null) addr = symbols.get("_" + labelName);
					if (addr != null) {
						currentLabel = labelName;
						instrCountSinceLabel = 0;
					}
					continue;
				}

				var instrMatch = ASM_INSTRUCTION.matcher(line);
				if (instrMatch.matches()) {
					var mnemonic = instrMatch.group(1).toLowerCase();
					if (!mnemonic.equals("section") && !mnemonic.equals("module")
							&& !mnemonic.equals("include") && !mnemonic.equals("global")
							&& !mnemonic.equals("extern") && !mnemonic.equals("defc")
							&& !mnemonic.equals("defb") && !mnemonic.equals("defw")
							&& !mnemonic.equals("defs") && !mnemonic.equals("org")
							&& !mnemonic.equals("if") && !mnemonic.equals("endif")
							&& !mnemonic.equals("else") && !mnemonic.equals("c_line")) {
						instrCountSinceLabel++;
					}
				}
			}
		} catch (IOException e) {
			LOG.error("Failed to parse .c.asm file: " + casmFile, e);
			return;
		}

		int mapped = 0;
		for (var cl : clines) {
			var fileName = fileNames.get(cl[0]);
			int srcLine = cl[1];
			int clAsmLine = cl[2];

			String resolvedLabel = labelForLine.get(clAsmLine);
			if (resolvedLabel == null) {
				var floorEntry = labels.floorEntry(clAsmLine);
				while (floorEntry != null) {
					var lbl = floorEntry.getValue();
					var a = symbols.get(lbl);
					if (a == null) a = symbols.get("_" + lbl);
					if (a != null) {
						resolvedLabel = lbl;
						break;
					}
					floorEntry = labels.lowerEntry(floorEntry.getKey());
				}
			}

			if (resolvedLabel == null) continue;

			var baseAddr = symbols.get(resolvedLabel);
			if (baseAddr == null) baseAddr = symbols.get("_" + resolvedLabel);
			if (baseAddr == null) continue;

			int instrCount = instrCountAtLine.getOrDefault(clAsmLine, 0);
			int estimatedOffset = instrCount * 2;
			int addr = baseAddr + estimatedOffset;

			var loc = new SourceLocation(fileName, srcLine);
			lineToAddress.putIfAbsent(loc, addr);
			addressToLine.putIfAbsent(addr, loc);
			mapped++;
		}
		LOG.info("Parsed " + casmFile.getFileName() + ": " + clines.size() + " C_LINE directives, "
				+ labels.size() + " labels, " + mapped + " new mappings ("
				+ lineToAddress.size() + " total)");
	}

	private static final Pattern MAP_LINE = Pattern.compile(
			"^\\s*(\\S+)\\s*=\\s*\\$([0-9A-Fa-f]+)\\b.*$");

	private void parseMapFile(Path mapFile) {
		try (BufferedReader reader = Files.newBufferedReader(mapFile)) {
			String line;
			while ((line = reader.readLine()) != null) {
				var m = MAP_LINE.matcher(line);
				if (m.matches()) {
					var name = m.group(1);
					try {
						long addr = Long.parseUnsignedLong(m.group(2), 16);
						if (addr <= 0xFFFF) {
							symbols.put(name, (int) addr);
						}
					} catch (NumberFormatException e) {
					}
				}
			}
		} catch (IOException e) {
			LOG.error("Failed to parse .map file: " + mapFile, e);
		}
	}

	@Override
	public int getAddress(String fileName, int line) {
		var normName = normaliseFileName(fileName);
		var offsets = learnedOffsets.computeIfAbsent(normName, k -> new TreeMap<>());

		/* We want to find a C_LINE that is close to the requested 'line'.
		 * We estimate the expected C_LINE based on the closest known offset below the requested line. */
		var floorOffset = offsets.floorEntry(line);
		int currentOffset = floorOffset != null ? floorOffset.getValue() : 0;
		int expectedCLine = line - currentOffset;

		/* Try exact match first */
		var loc = new SourceLocation(normName, expectedCLine);
		var addr = lineToAddress.get(loc);
		if (addr != null) {
			offsets.put(expectedCLine, line - expectedCLine);
			return addr;
		}

		/* Fuzzy search */
		for (int delta = 1; delta <= FUZZY_SEARCH_WINDOW; delta++) {
			/* Search below first */
			var locBelow = new SourceLocation(normName, expectedCLine - delta);
			addr = lineToAddress.get(locBelow);
			if (addr != null) {
				int matchedCLine = expectedCLine - delta;
				offsets.put(matchedCLine, line - matchedCLine);
				LOG.info("Fuzzy breakpoint match: requested line " + line
						+ " → matched C_LINE " + matchedCLine + " for " + normName + " (learned offset: +" + (line - matchedCLine) + ")");
				return addr;
			}
			/* Search above */
			var locAbove = new SourceLocation(normName, expectedCLine + delta);
			addr = lineToAddress.get(locAbove);
			if (addr != null) {
				int matchedCLine = expectedCLine + delta;
				offsets.put(matchedCLine, line - matchedCLine);
				LOG.info("Fuzzy breakpoint match: requested line " + line
						+ " → matched C_LINE " + matchedCLine + " for " + normName + " (learned offset: +" + (line - matchedCLine) + ")");
				return addr;
			}
		}
		return -1;
	}

	@Override
	public SourceLocation getSourceLocation(int address) {
		var entry = addressToLine.floorEntry(address);
		if (entry != null) {
			var loc = entry.getValue();
			var offsets = learnedOffsets.get(loc.fileName());
			if (offsets != null) {
				var offsetEntry = offsets.floorEntry(loc.line());
				if (offsetEntry != null) {
					return new SourceLocation(loc.fileName(), loc.line() + offsetEntry.getValue());
				}
			}
			return loc;
		}
		return null;
	}

	@Override
	public int getSymbolAddress(String symbolName) {
		var addr = symbols.get(symbolName);
		return addr != null ? addr : -1;
	}

	@Override
	public boolean hasDebugInfo() {
		return !lineToAddress.isEmpty();
	}

	private static String normaliseFileName(String name) {
		var idx = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		if (idx >= 0) name = name.substring(idx + 1);
		if (name.endsWith(".c.asm")) name = name.substring(0, name.length() - 4);
		return name.toLowerCase();
	}

	private static String stripExtension(String name) {
		var dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}
}
