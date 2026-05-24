package uk.co.bithatch.emuzx.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.ILog;

/**
 * Parses z88dk {@code .c.asm} files (produced by {@code --c-code-in-asm --assemble-only})
 * and {@code .map} files to build bidirectional mappings between C source
 * file:line and absolute memory addresses.
 * <p>
 * The {@code .c.asm} contains {@code C_LINE} directives mapping assembly to C
 * source lines, and labels (e.g. {@code ._main}, {@code .i_2}) whose absolute
 * addresses are in the {@code .map} file. Assembly instructions between labels
 * are counted to estimate byte offsets for finer-grained line mapping.
 */
public class Z88dkDebugInfoParser {

	private static final ILog LOG = ILog.of(Z88dkDebugInfoParser.class);

	public record SourceLocation(String fileName, int line) {
		@Override
		public String toString() {
			return fileName + ":" + line;
		}
	}

	/** Source line → first absolute address on that line */
	private final Map<SourceLocation, Integer> lineToAddress = new HashMap<>();

	/** Absolute address → source location */
	private final NavigableMap<Integer, SourceLocation> addressToLine = new TreeMap<>();

	/** Symbol name → address from .map file */
	private final Map<String, Integer> symbols = new HashMap<>();

	/**
	 * Parse debug information from the .map file and .c.asm files.
	 *
	 * @param binaryPath path to the compiled binary (e.g. Debug/hello.nex)
	 */
	public void parse(Path binaryPath) {
		if (binaryPath == null) {
			LOG.warn("No binary path provided for debug info parsing");
			return;
		}

		var dir = binaryPath.getParent();
		var baseName = stripExtension(binaryPath.getFileName().toString());

		/* Parse .map file FIRST — we need symbol addresses */
		var mapFile = dir.resolve(baseName + ".map");
		if (Files.exists(mapFile)) {
			LOG.info("Parsing map file: " + mapFile);
			parseMapFile(mapFile);
		} else {
			LOG.warn("No .map file found at: " + mapFile);
		}

		/* Find and parse .c.asm files.
		 * The debug info generator tool places them in the build directory
		 * (e.g. Debug/main.c.asm) or next to the source (../main.c.asm). */
		findAndParseCasmFiles(dir);
		var projectDir = dir.getParent();
		if (projectDir != null && !projectDir.equals(dir)) {
			findAndParseCasmFiles(projectDir);
		}

		LOG.info("Debug info parsed: " + lineToAddress.size() + " line mappings, "
				+ symbols.size() + " symbols");
	}

	// ---- .c.asm file parsing ----

	/*
	 * C_LINE directive, e.g.:
	 *   C_LINE	9,"/path/to/main.c::main::0::1"
	 */
	private static final Pattern C_LINE_DIRECTIVE = Pattern.compile(
			"^\\s*C_LINE\\s+(\\d+)\\s*,\\s*\"(.+?)\"\\s*$");

	/*
	 * Label definition, e.g.:
	 *   ._main
	 *   .i_2
	 */
	private static final Pattern LABEL_DEF = Pattern.compile(
			"^\\.(\\w+)\\s*$");

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

	/*
	 * Matches a Z80 assembly instruction line (not a directive, label, or comment).
	 * Used to count instructions for byte-offset estimation.
	 */
	private static final Pattern ASM_INSTRUCTION = Pattern.compile(
			"^\\s+(\\w+)\\b.*$");

	/* Instruction prefixes that indicate 2-extra-byte (IX/IY) instructions */
	private static final java.util.Set<String> IX_IY_MNEMONICS = java.util.Set.of(
			"dd", "fd");

	private void parseCasmFile(Path casmFile) {
		LOG.info("Parsing .c.asm file: " + casmFile);

		/*
		 * Two-pass approach:
		 * Pass 1: collect all C_LINE positions, label positions, and count
		 *         assembly instructions between them for byte-offset estimation.
		 * Pass 2: resolve each C_LINE to an absolute address using:
		 *         preceding label address + estimated byte offset.
		 */

		var clines = new ArrayList<int[]>(); // [fileNameIdx, srcLine, asmLineNum]
		var labels = new TreeMap<Integer, String>(); // asmLineNum → labelName
		var fileNames = new ArrayList<String>();
		var fileNameIndex = new HashMap<String, Integer>();
		/* Track instruction count at each asm line number for offset estimation */
		var instrCountAtLine = new TreeMap<Integer, Integer>(); // asmLineNum → cumulative instruction count since last label
		var labelForLine = new TreeMap<Integer, String>(); // tracks which label is "active" at each line

		int asmLineNum = 0;
		int instrCountSinceLabel = 0;
		String currentLabel = null;

		try (BufferedReader reader = Files.newBufferedReader(casmFile)) {
			String line;
			while ((line = reader.readLine()) != null) {
				asmLineNum++;

				var clMatch = C_LINE_DIRECTIVE.matcher(line);
				if (clMatch.matches()) {
					int srcLine = Integer.parseInt(clMatch.group(1));
					var rawPath = clMatch.group(2);
					/* Strip scope info: main.c::main::0::1 → main.c */
					var colonColon = rawPath.indexOf("::");
					var filePath = colonColon >= 0 ? rawPath.substring(0, colonColon) : rawPath;
					var normName = normaliseFileName(filePath);

					if (normName.endsWith(".c") && srcLine > 0) {
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
					/* Only reset instruction count if this label is in .map */
					var addr = symbols.get(labelName);
					if (addr == null) addr = symbols.get("_" + labelName);
					if (addr != null) {
						currentLabel = labelName;
						instrCountSinceLabel = 0;
					}
					continue;
				}

				/* Count assembly instructions for offset estimation */
				var instrMatch = ASM_INSTRUCTION.matcher(line);
				if (instrMatch.matches()) {
					var mnemonic = instrMatch.group(1).toLowerCase();
					/* Skip assembler directives */
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

		/*
		 * Resolve: for each C_LINE, use the preceding label that exists in .map
		 * and add an estimated byte offset (instruction count * ~2 bytes average).
		 */
		int mapped = 0;
		for (var cl : clines) {
			var fileName = fileNames.get(cl[0]);
			int srcLine = cl[1];
			int clAsmLine = cl[2];

			/* Find the active label for this C_LINE */
			String resolvedLabel = labelForLine.get(clAsmLine);
			if (resolvedLabel == null) {
				/* Fallback: walk backwards through labels to find one in .map */
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

			/* Estimate byte offset: ~2 bytes per Z80 instruction on average */
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

	// ---- .map file parsing ----

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
						/* Skip */
					}
				}
			}
		} catch (IOException e) {
			LOG.error("Failed to parse .map file: " + mapFile, e);
		}
	}

	// ---- Public API ----

	public int getAddress(String fileName, int line) {
		var loc = new SourceLocation(normaliseFileName(fileName), line);
		var addr = lineToAddress.get(loc);
		return addr != null ? addr : -1;
	}

	public SourceLocation getSourceLocation(int address) {
		var entry = addressToLine.floorEntry(address);
		return entry != null ? entry.getValue() : null;
	}

	public int getSymbolAddress(String symbolName) {
		var addr = symbols.get(symbolName);
		return addr != null ? addr : -1;
	}

	public Map<SourceLocation, Integer> getLineToAddressMap() {
		return Collections.unmodifiableMap(lineToAddress);
	}

	public NavigableMap<Integer, SourceLocation> getAddressToLineMap() {
		return Collections.unmodifiableNavigableMap(addressToLine);
	}

	public boolean hasDebugInfo() {
		return !lineToAddress.isEmpty();
	}

	// ---- Helpers ----

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
