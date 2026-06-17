package uk.co.bithatch.eclipz80.generator;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
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
import java.util.Stack;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import uk.co.bithatch.eclipz80.asm.*;
import uk.co.bithatch.eclipzpp.SourceMap;

/**
 * A simple Z80 assembler that walks an Xtext-parsed {@link AsmProgram} AST and
 * emits raw Z80 machine code. Uses a two-pass approach: pass&nbsp;1 collects
 * label addresses (emitting into a discarded buffer to get accurate instruction
 * sizes), then pass&nbsp;2 re-emits the final machine code with all label
 * references resolved.
 * <p>
 * Supports:
 * <ul>
 *   <li>Labels defined on their own line, on statement lines, via EQU, and via DEFC</li>
 *   <li>Forward and backward label references in operands (JP, CALL, LD, data directives, etc.)</li>
 *   <li>Relative offset calculation for JR and DJNZ with range checking</li>
 *   <li>MODULE / SECTION tracking with Z88DK-style module-qualified symbol names</li>
 *   <li>PUBLIC / GLOBAL / EXTERN symbol visibility directives</li>
 *   <li>ALIGN directive with configurable fill byte</li>
 * </ul>
 * <p>
 * Can be used standalone (outside the Xtext generator infrastructure) by
 * calling {@link #assemble(AsmProgram)} which returns a {@code byte[]}, or
 * {@link #assemble(AsmProgram, OutputStream)} to write directly to a stream.
 * <p>
 * Use the {@link #builder()} method to configure options before assembling:
 * <pre>
 * Z80Assembler asm = Z80Assembler.builder()
 *     .withMap()                // write .zmap alongside .bin output
 *     .withFarAddresses()      // use 32-bit far addresses (Z88DK)
 *     .build();
 * </pre>
 *
 * <h2>Outstanding TODOs</h2>
 * <ul>
 *   <li>TODO: Explicit module.label syntax in expressions (needs grammar change for dotted AsmLabel - DONE)</li>
 *   <li>TODO: Section ordering in output (CODE, DATA, BSS reordering for linker phase)</li>
 *   <li>TODO: Proper linker-phase extern resolution (currently fails with address 0)</li>
 *   <li>TODO: Copper directives — CU.WAIT, CU.MOVE, CU.STOP, CU.NOP  (Grammar exists, needs wiring up here)</li>
 *   <li>TODO: DMA directives — DMA.WR0 through DMA.WR6/DMA.CMD  (Grammar exists, needs wiring up here)</li>
 *   <li>TODO: Z88DK directives — CALL_OZ, CALL_PKG, FPP, .ASSUME ADL, C_LINE  (Grammar exists, needs wiring up here)</li>
 *   <li>TODO: PROC / LOCAL scoping (Grammar exists, needs wiring up here)</li>
 *   <li>TODO: Numeric label support (AsmNumericLabelLine)</li>
 *   <li>TODO: Allow command line ORG setting that overrides the default and what assembly specifies</li>
 *   <li>TODO: Add all same command line options z80asm has</li>
 * </ul>
 * 
 * 
 * 
 * 
 * 
 */
public class Z80Assembler {
	
	public interface Results {
		Path mapFile();
	}
	
	public final static class Symbol {
		
		public final static class Builder {

			private final String name;
			private int address;
			private boolean isGlobal = false;
			private boolean isExternal = false;
			private boolean isPublic = false;
			private String section;
			private String module;
			
			public Builder(String name) {
				this.name = name;
			}
			
			public Builder withSection(String section) {
				this.section = section;
				return  this;
			}
			
			public Builder withModule(String module) {
				this.module = module;
				return  this;
			}

			public Builder asGlobal(boolean isGlobal) {
				this.isGlobal = isGlobal;
				return this;
			}

			public Builder asPublic(boolean isPublic) {
				this.isPublic = isPublic;
				return this;
			}

			public Builder asExternal(boolean isExternal) {
				this.isExternal = isExternal;
				return this;
			}
			
			public Builder withAddress(int address) {
				this.address = address;
				return this;
			}
			
			public Symbol build() {
				return new Symbol(this);
			}
		}
		
		private final String name;
		private int address;
		private boolean isGlobal;
		private boolean isExternal;
		private boolean isPublic;
		private String section;
		private String module;
		
		private Symbol(String name) {
			this.name = name;
		}	
		
		private Symbol(Builder builder) {
			this.name = builder.name;
			this.address = builder.address;
			this.isExternal = builder.isExternal;
			this.isPublic = builder.isPublic;
			this.isGlobal = builder.isGlobal;
			this.section = builder.section;
			this.module = builder.module;
		}

		public int address() {
			return address;
		}
		
		public boolean isPublic() {
			return isPublic;
		}
		
		public boolean isExternal() {
			return isExternal;
		}
		
		public boolean isGlobal() {
			return isGlobal;
		}
		
		public String name() {
			return name;
		}
		
		public String section() {
			return section;
		}
		
		public String module() {
			return module;
		}
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
	public static class AssemblyException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final String filename;
		private final int line;

		public AssemblyException(String filename, int line, String message) {
			super(filename + ":" + line + ": " + message);
			this.filename = filename;
			this.line = line;
		}

		public String getFilename() { return filename; }
		public int getLine() { return line; }
	}

	private final List<String> warnings = new ArrayList<>();
	private final Map<String, Symbol> symbols = new LinkedHashMap<>();
	private int currentAddress = 0;
	private String effectiveSource;
	private Path sourceDir;
	private int currentLine = -1;
	private boolean pass1;
	private WarningCallback warningCallback;
	private String currentSection = "";
	private String currentModule = "";
	private boolean listing = false;
	private boolean forcedORG;

	private final Optional<Path> mapFile;
	private final Optional<Path> outputDir;
	private final boolean mapEnabled;
	private final boolean farAddresses;
	private final boolean z80n;
	private final List<MapEntry> mapEntries = new ArrayList<>();
	private Optional<Integer> forceORG;
	private Optional<Integer> defaultORG;
	private final int defaultFill;
	private final List<Path> libPaths;
	private final Optional<SourceMap> sourceMap;
	private final Stack<String> namespace = new Stack<>();
	private FloatType floatType = FloatType.ZX;

	/**
	 * An entry in the line-to-address map.
	 */
	public static class MapEntry {
		private final String fileName;
		private final int lineNumber;
		private final long address;

		MapEntry(String fileName, int lineNumber, long address) {
			this.fileName = fileName;
			this.lineNumber = lineNumber;
			this.address = address;
		}

		public String getFileName() { return fileName; }
		public int getLineNumber() { return lineNumber; }
		public long getAddress() { return address; }
	}

	/**
	 * Builder for configuring a {@link Z80Assembler}.
	 */
	public static class Builder {
		private Optional<Path> mapFile = Optional.empty();
		private Optional<Path> outputDir = Optional.empty();
		private Optional<Integer> forceORG = Optional.empty();
		private boolean mapEnabled;
		private boolean farAddresses;
		private boolean z80n;
		private WarningCallback warningCallback;
		private Optional<Integer> defaultORG = Optional.empty();
		private Optional<Integer> defaultFill = Optional.empty();
		private List<Path> libPaths = new ArrayList<>();
		private Optional<SourceMap> sourceMap = Optional.empty();
		private Map<String, Symbol> symbols = new LinkedHashMap<>();

		private Builder() {}
		
		public Builder withSymbols(Symbol... symbols) {
			return withSymbols(Arrays.asList(symbols));
		}
		
		public Builder withSymbols(Collection<Symbol> symbols) {
			for(var symbol : symbols) {
				this.symbols.put(symbol.name, symbol);
			}
			return this;
		}
		
		/**
		 * If the input has been preprocessed, the resulting Source Map 
		 * should be passed to the assembler so it's reported errors and warnings and address
		 * map refer to the original source lines.
		 * 
		 * @param sourceMap map
		 * @return this for chaining
		 */
		public Builder withSourceMap(SourceMap sourceMap) {
			this.sourceMap = Optional.of(sourceMap);
			return this;
		}

		
		/**
		 * Add an array of paths to the list of those searched when locating
		 * library files
		 * 
		 * @param libPaths library paths
		 * @return this for chaining
		 */
		public Builder addLibPaths(Path... libPaths) {
			return addLibPaths(List.of(libPaths));
		}
		
		/**
		 * Add a list of paths to the list of those searched when locating
		 * library files
		 * 
		 * @param libPaths library paths
		 * @return this for chaining
		 */
		public Builder addLibPaths(Collection<Path> libPaths) {
			this.libPaths.addAll(libPaths);
			return this;
		}

		/**
		 * Set the paths searched when locating
		 * library files.
		 * 
		 * @param libPaths library paths
		 * @return this for chaining
		 */
		public Builder withLibPaths(Path... libPaths) {
			return withLibPaths(List.of(libPaths));
		}

		/**
		 * Set the paths searched when locating
		 * library files.
		 * 
		 * @param libPaths library paths
		 * @return this for chaining
		 */
		public Builder withLibPaths(Collection<Path> libPaths) {
			this.libPaths.clear();
			return addLibPaths(libPaths);
		}

		/**
		 * Enable .zmap output. The map file path will be derived from the
		 * binary output path (replacing the extension with {@code .zmap}).
		 * This acts as a flag — the actual path is resolved at write time
		 * if no explicit path is given via {@link #withMap(Path)}.
		 * 
		 * @return this for chaining
		 */
		public Builder withMap() {
			return withMap(true);
		}
		
		/**
		 * Enable .zmap output. The map file path will be derived from the
		 * binary output path (replacing the extension with {@code .zmap}).
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
		 * Set the default ORG. The ORG directive in source may override
		 * this as may {@link #withORG(int)}.
		 * 
		 * @param defaultORG default ORG
		 * @return this for chaining
		 */
		public Builder withDefaultORG(int defaultORG) {
			this.defaultORG  = Optional.of(defaultORG);
			return this;
		}
		
		/**
		 * Set the default fill byte (DEFS). When not set, will be zero.
		 * 
		 * @param defaultFill default fill byte for DEEFS
		 * @return this for chaining
		 */
		public Builder withDefaultFill(int defaultFill) {
			this.defaultFill = Optional.of(defaultFill);
			return this;
		}
		
		/**
		 * Force an ORG for the first section. The ORG directive in source may override
		 * this.
		 * 
		 * @param forceORG force ORG of first section
		 * @return this for chaining
		 */
		public Builder withORG(int forceORG) {
			this.forceORG  = Optional.of(forceORG);
			return this;
		}

		/**
		 * Enable .zmap output and specify an explicit file path.
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
		 * Use 32-bit far addresses (Z88DK style) in map output.
		 * By default, standard 16-bit addresses are used.
		 * 
		 * @return this for chaining
		 */
		public Builder withFarAddresses() {
			this.farAddresses = true;
			return this;
		}

		/**
		 * Enable Z80N mode (next-generation Z80 instructions and addressing).
		 * 
		 * @return this for chaining
		 */
		public Builder withZ80N() {
			this.z80n = true;
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

		public Z80Assembler build() {
			return new Z80Assembler(this);
		}
	}

	/**
	 * Create a new builder for configuring a {@link Z80Assembler}.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Default constructor for backwards compatibility.
	 */
	public Z80Assembler() {
		this.mapFile = Optional.empty();
		this.mapEnabled = false;
		this.farAddresses = false;
		this.z80n = false;
		this.outputDir = Optional.empty();
		this.forceORG = Optional.empty();
		this.defaultORG = Optional.empty();
		this.defaultFill = 0;
		this.libPaths = Collections.emptyList();
		this.sourceMap = Optional.empty();
	}

	private Z80Assembler(Builder builder) {
		this.sourceMap = builder.sourceMap;
		this.outputDir = builder.outputDir;
		this.mapFile = builder.mapFile;
		this.mapEnabled = builder.mapEnabled;
		this.farAddresses = builder.farAddresses;
		this.z80n = builder.z80n;
		this.warningCallback = builder.warningCallback;
		this.forceORG = builder.forceORG;
		this.defaultORG = builder.defaultORG;
		this.defaultFill = builder.defaultFill.orElse(0);
		this.libPaths = Collections.unmodifiableList(new ArrayList<>(builder.libPaths));
		this.symbols.putAll(builder.symbols);
	}

	/**
	 * Returns whether map generation was requested (via builder).
	 */
	boolean isMapEnabled() {
		return mapEnabled;
	}

	/**
	 * Assemble the program, writing bytes to the given output stream.
	 * Uses a two-pass approach: pass 1 collects label addresses (output is
	 * discarded), pass 2 emits the final machine code with all symbols resolved.
	 * If a map file was configured, the .zmap file is written after assembly.
	 */
	public Results assemble(String sourceFileName, AsmProgram program, OutputStream out) {
		warnings.clear();
		forcedORG = false;
		currentAddress = forceORG.or(() -> defaultORG).orElse(32768);
		symbols.clear();
		mapEntries.clear();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// Derive source file name from the resource URI if not explicitly set
		this.effectiveSource = calcEffectiveSource(sourceFileName, program);

		// Derive the source directory for resolving relative paths (e.g. INCBIN)
		if (program.eResource() != null && program.eResource().getURI().isFile()) {
			this.sourceDir = Path.of(program.eResource().getURI().toFileString()).getParent();
		} else {
			this.sourceDir = Path.of("").toAbsolutePath();
		}

		// Default module name derived from source file (Z88DK convention)
		this.currentModule = deriveModuleName(this.effectiveSource);

		// ── Pass 1: collect label addresses (output discarded) ──
		pass1 = true;
		currentSection = "";
		currentModule = deriveModuleName(this.effectiveSource);
		assembleLines(program, new ByteArrayOutputStream());

		// ── Pass 2: emit final machine code with symbols resolved ──
		pass1 = false;
		forcedORG = false;
		currentAddress = forceORG.or(() -> defaultORG).orElse(32768);
		currentSection = "";
		currentModule = deriveModuleName(this.effectiveSource);
		assembleLines(program, baos);

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
				return outputDir.orElse(this.sourceDir).resolve((dot >= 0 ? effectiveSource.substring(0, dot) : effectiveSource) + ".zmap");
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

	protected String calcEffectiveSource(String sourceFileName, AsmProgram program) {
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
	 * Derive the default module name from a source filename.
	 * Strips the file extension (e.g. "main.asm" → "main").
	 * This follows Z88DK convention where the module name defaults
	 * to the source file name without extension.
	 */
	private String deriveModuleName(String sourceFileName) {
		if (sourceFileName == null) return "";
		int dot = sourceFileName.lastIndexOf('.');
		String base = dot >= 0 ? sourceFileName.substring(0, dot) : sourceFileName;
		// Strip any directory separators
		int sep = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
		if (sep >= 0) base = base.substring(sep + 1);
		return base;
	}

	/**
	 * Returns the collected line-to-address map entries from the last assembly.
	 */
	public List<MapEntry> getMapEntries() {
		return mapEntries;
	}

	/**
	 * Returns any warnings collected during the last assembly.
	 */
	public List<String> getWarnings() {
		return warnings;
	}

	/**
	 * Returns an unmodifiable view of the symbol table from the last assembly.
	 */
	public Map<String, Symbol> getSymbols() {
		return Collections.unmodifiableMap(symbols);
	}

	// ─────────────── Line / source tracking helpers ───────────────

	private int getLineNumber(AsmLine line) {
		INode node = NodeModelUtils.getNode(line);
		if (node != null) {
			return node.getStartLine();
		}
		return -1;
	}

	private String getSourceFile(AsmLine line, String defaultSource) {
		// For now, all lines come from the main source file.
		// When preprocessing / includes are supported, this can be
		// extended to return the included file path relative to the
		// original source.
		return defaultSource;
	}

	private long addressMask() {
		return farAddresses ? 0xFFFFFFFFL : 0xFFFFL;
	}

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
				String addrStr;
				if (farAddresses) {
					addrStr = String.format("%08X", entry.address);
				} else {
					addrStr = String.format("%04X", entry.address);
				}
				pw.println(fileCol + "|" + entry.lineNumber + "|" + addrStr);
			}
		} catch (IOException e) {
			warn("Failed to write map file: " + e.getMessage());
		}
	}
	
	private int translateToOriginalSourceLine(int line, String uri) {
		return sourceMap.map(sm -> sm.translatePreprocessedToOriginalLine(line, uri)).orElse(line);
	}

	// ─────────────── Program / line assembly ───────────────

	/**
	 * Walk the lines of an {@link AsmProgram} and assemble each statement.
	 * Used both for the top-level program and for conditional branch bodies.
	 */
	private void assembleLines(AsmProgram program, ByteArrayOutputStream out) {
		for (AsmLine line : program.getLines()) {

			// ── Label-only line (e.g. "start:" or ".loop:") ──
			// ── EQU line (e.g. "SCREEN_ADDR EQU $4000") ──
			if (line instanceof LabelledLine lol) {
				if(lol.getValue() != null) {
					// Resolve on both passes — pass 2 re-resolves with all symbols defined
					putSymbol(lol.getName().getName()).address = resolveImmediate(lol.getValue());
				}
				if (pass1 && lol.getName() != null) {
					putSymbol(lol.getName().getName()).address = currentAddress;
				}
				
				// Record line-to-address mapping before emitting
				int lineNumber = getLineNumber(lol);
				String lineFile = getSourceFile(lol, effectiveSource);
				this.currentLine = lineNumber;

				if (!pass1 && lineNumber > 0 && listing) {
					mapEntries.add(new MapEntry(lineFile, translateToOriginalSourceLine(lineNumber, effectiveSource), currentAddress & addressMask()));
				}

				for (AsmStatement stmt : lol.getStatements()) {
					assembleStatement(stmt, out);
				}
				
				continue;
			}

			// Other line types (NUMERIC_LABEL, LOCAL, etc.) are ignored for now
			// TODO: Numeric label support (AsmNumericLabelLine)
			// TODO: PROC / LOCAL scoping
		}
	}

	// ─────────────── Statement dispatch ───────────────

	private void assembleStatement(AsmStatement stmt, ByteArrayOutputStream out) {
		// ── Directives ──
		if (stmt instanceof Org) {
			if(forceORG.isPresent()) {
				if(forcedORG) {
					return;
				}
				else
					forcedORG = true;
			}
			currentAddress = resolveIntegralLiteral(((Org) stmt).getValue());
			return;
		}

		// ── Module / Section directives ──
		if (stmt instanceof uk.co.bithatch.eclipz80.asm.Module) {
			currentModule = ((uk.co.bithatch.eclipz80.asm.Module) stmt).getName();
			return;
		}
		if (stmt instanceof Section) {
			currentSection = ((Section) stmt).getName();
			return;
		}

		// ── Symbol visibility directives ──
		if (stmt instanceof Public) {
			Public pub = (Public) stmt;
			if (pass1) {
				for (String name : pub.getName()) {
					putSymbol(name).isPublic = true;
				}
			}
			return;
		}
		if (stmt instanceof Global) {
			Global glob = (Global) stmt;
			if (pass1) {
				for (AsmLabelDef name : glob.getName()) {
					putSymbol(name.getName()).isGlobal = true;
				}
			}
			return;
		}
		if (stmt instanceof Local lcl) {
			if (pass1) {
				for (AsmLabelDef name : lcl.getName()) {
					Symbol symbl = putSymbol(name.getName());
					symbl.isGlobal = false;
					symbl.isExternal = false;
					symbl.isPublic = false;
				}
			}
			return;
		}
		if (stmt instanceof Extern) {
			Extern ext = (Extern) stmt;
			if (pass1) {
				for (AsmLabelDef name : ext.getName()) {
					Symbol sym = putSymbol(name.getName());
					sym.isExternal = true;
					sym.address = 0;
				}
			} else {
				// TODO: Proper linker-phase extern resolution (currently fails with address 0)
				for (AsmLabelDef name : ext.getName()) {
					String qualifiedName = currentModule + "." + name.getName();
					Symbol sym = symbols.get(qualifiedName);
					if (sym == null) sym = symbols.get(name.getName());
					if (sym != null && sym.isExternal && sym.address == 0) {
						throw new AssemblyException(effectiveSource, translateToOriginalSourceLine(currentLine, effectiveSource),
								"Unresolved external symbol: " + name.getName());
					}
				}
			}
			return;
		}

		// ── ALIGN directive ──
		if (stmt instanceof AlignDirective) {
			AlignDirective align = (AlignDirective) stmt;
			int boundary = resolveImmediate(align.getExpression());
			if (boundary > 0) {
				int padding = (boundary - (currentAddress % boundary)) % boundary;
				int fill = 0x00;
				if (align.getFiller() != null) {
					fill = resolveImmediate(align.getFiller()) & 0xFF;
				}
				for (int i = 0; i < padding; i++) {
					emit8(out, fill);
				}
			}
			return;
		}

		// ── ALIGN directive ──
		if (stmt instanceof SetFloat setFloat) {
			floatType  = setFloat.getType();
			return;
		}

		// ── DEFC line (e.g. "DEFC name = expr") ──
		if (stmt instanceof DefC defc) {
			if (defc.getName() != null) {
				// Resolve on both passes — pass 2 re-resolves with all symbols defined
				putSymbol(defc.getName().getName()).address = resolveImmediate(defc.getValue());
			}
			return;
		}

		// ── LSTS directives ──
		if(stmt instanceof LSTON) {
			listing = true;
			return;
		}

		if(stmt instanceof LSTOFF) {
			listing = false;
			return;
		}

		// ── Data directives ──
		if (stmt instanceof DefByte) {
			assembleDefByte((DefByte) stmt, out);
			return;
		}
		if (stmt instanceof DefWord) {
			assembleDefWord((DefWord) stmt, out);
			return;
		}
		if (stmt instanceof DefWordBE) {
			assembleDefWordBE((DefWordBE) stmt, out);
			return;
		}
		if (stmt instanceof DefPointer) {
			assembleDefPointer((DefPointer) stmt, out);
			return;
		}
		if (stmt instanceof DefDWord) {
			assembleDefDWord((DefDWord) stmt, out);
			return;
		}
		if (stmt instanceof DefTermString) {
			assembleDefTermString((DefTermString) stmt, out);
			return;
		}
		if (stmt instanceof DefSpace) {
			assembleDefSpace((DefSpace) stmt, out);
			return;
		}
		if (stmt instanceof DataDefineGroup) {
			assembleDefineGroup((DataDefineGroup) stmt, out);
			return;
		}

		// ── Zero-operand instructions ──
		if (stmt instanceof Nop)  { emit8(out, 0x00); return; }
		if (stmt instanceof Halt) { emit8(out, 0x76); return; }
		if (stmt instanceof DI)   { emit8(out, 0xF3); return; }
		if (stmt instanceof EI)   { emit8(out, 0xFB); return; }
		if (stmt instanceof Ret) {
			Ret ret = (Ret) stmt;
			if (ret.getCondition() == null) {
				emit8(out, 0xC9);
			} else {
				int cc = resolveCondition(ret.getCondition());
				emit8(out, 0xC0 + cc * 8);
			}
			return;
		}
		if (stmt instanceof Exx)  { emit8(out, 0xD9); return; }
		if (stmt instanceof Rla)  { emit8(out, 0x17); return; }
		if (stmt instanceof Rra)  { emit8(out, 0x1F); return; }
		if (stmt instanceof Rlca) { emit8(out, 0x07); return; }
		if (stmt instanceof Rrca) { emit8(out, 0x0F); return; }
		if (stmt instanceof Cpl)  { emit8(out, 0x2F); return; }
		if (stmt instanceof Ccf)  { emit8(out, 0x3F); return; }
		if (stmt instanceof Scf)  { emit8(out, 0x37); return; }
		if (stmt instanceof Daa)  { emit8(out, 0x27); return; }
		if (stmt instanceof Neg)  { emit8(out, 0xED); emit8(out, 0x44); return; }
		if (stmt instanceof Rld)  { emit8(out, 0xED); emit8(out, 0x6F); return; }
		if (stmt instanceof Rrd)  { emit8(out, 0xED); emit8(out, 0x67); return; }
		if (stmt instanceof Reti) { emit8(out, 0xED); emit8(out, 0x4D); return; }
		if (stmt instanceof Retn) { emit8(out, 0xED); emit8(out, 0x45); return; }
		if (stmt instanceof Cpd)  { emit8(out, 0xED); emit8(out, 0xA9); return; }
		if (stmt instanceof Cpdr) { emit8(out, 0xED); emit8(out, 0xB9); return; }
		if (stmt instanceof Cpi)  { emit8(out, 0xED); emit8(out, 0xA1); return; }
		if (stmt instanceof Cpir) { emit8(out, 0xED); emit8(out, 0xB1); return; }
		if (stmt instanceof Ldd)  { emit8(out, 0xED); emit8(out, 0xA8); return; }
		if (stmt instanceof Lddr) { emit8(out, 0xED); emit8(out, 0xB8); return; }
		if (stmt instanceof Ldi)  { emit8(out, 0xED); emit8(out, 0xA0); return; }
		if (stmt instanceof Ldir) { emit8(out, 0xED); emit8(out, 0xB0); return; }
		if (stmt instanceof Ind)  { emit8(out, 0xED); emit8(out, 0xAA); return; }
		if (stmt instanceof Indr) { emit8(out, 0xED); emit8(out, 0xBA); return; }
		if (stmt instanceof Ini)  { emit8(out, 0xED); emit8(out, 0xA2); return; }
		if (stmt instanceof Inir) { emit8(out, 0xED); emit8(out, 0xB2); return; }
		if (stmt instanceof Otdr) { emit8(out, 0xED); emit8(out, 0xBB); return; }
		if (stmt instanceof Otir) { emit8(out, 0xED); emit8(out, 0xB3); return; }
		if (stmt instanceof Outd) { emit8(out, 0xED); emit8(out, 0xAB); return; }
		if (stmt instanceof Outi) { emit8(out, 0xED); emit8(out, 0xA3); return; }

		// ── LD ──
		if (stmt instanceof Ld) {
			assembleLd((Ld) stmt, out);
			return;
		}

		// ── INC / DEC ──
		if (stmt instanceof Inc) {
			assembleIncDec(((Inc) stmt).getName(), true, out);
			return;
		}
		if (stmt instanceof Dec) {
			assembleIncDec(((Dec) stmt).getName(), false, out);
			return;
		}

		// ── ADD ──
		if (stmt instanceof Add) {
			assembleAdd((Add) stmt, out);
			return;
		}

		// ── SUB ──
		if (stmt instanceof Sub) {
			Sub sub = (Sub) stmt;
			if (sub.getValue() != null) {
				// SUB A, operand — treat name as A, value as operand
				assembleAluOp(sub.getValue(), 0x90, 0xD6, out);
			} else {
				assembleAluOp(sub.getName(), 0x90, 0xD6, out);
			}
			return;
		}

		// ── AND / OR / XOR / CP ──
		if (stmt instanceof And)    { assembleAluOp(((And) stmt).getName(),    0xA0, 0xE6, out); return; }
		if (stmt instanceof OrInst) { assembleAluOp(((OrInst) stmt).getName(), 0xB0, 0xF6, out); return; }
		if (stmt instanceof Xor)    { assembleAluOp(((Xor) stmt).getName(),    0xA8, 0xEE, out); return; }
		if (stmt instanceof Cp)     { assembleAluOp(((Cp) stmt).getName(),     0xB8, 0xFE, out); return; }

		// ── PUSH / POP ──
		if (stmt instanceof Push push) {
			if(push.getNamespace() != null) {
				// TODO: Handle PUSH symbol namespace 
				namespace.push(push.getNamespace().getImportedNamespace());
			}
			else {
				AsmExpression reg = push.getRegister();
				int rr = resolveRegister16Push(reg);
				if (rr >= 0) {
					emit8(out, 0xC5 + rr * 16);
				} else {
					// Check IX/IY
					String regName = getRegisterName(reg);
					int prefix = getIXIYPrefix(regName);
					if (prefix > 0 && regName != null && ("IX".equalsIgnoreCase(regName) || "IY".equalsIgnoreCase(regName))) {
						emit8(out, prefix);
						emit8(out, 0xE5);
					} else {
						// PUSH nn (Z80N) — handle immediate
						requireZ80N("PUSH nn");
						int val = resolveImmediate(reg);
						emit8(out, 0xED); emit8(out, 0x8A);
						// Z80N PUSH nn is big-endian
						emit8(out, (val >> 8) & 0xFF);
						emit8(out, val & 0xFF);
					}
				}
			}
			return;
		}
		if (stmt instanceof Pop pop) {
			AsmExpression reg = pop.getRegister();
			if(reg == null) {
				if(namespace.isEmpty()) {
					warn("Namespace is empty, cannot POP");
				}
				else {
					namespace.pop();
				}
			}
			else {
				int rr = resolveRegister16Push(reg);
				if (rr >= 0) {
					emit8(out, 0xC1 + rr * 16);
				} else {
					String regName = getRegisterName(reg);
					int prefix = getIXIYPrefix(regName);
					if (prefix > 0 && regName != null && ("IX".equalsIgnoreCase(regName) || "IY".equalsIgnoreCase(regName))) {
						emit8(out, prefix);
						emit8(out, 0xE1);
					} else {
						warn("POP requires a 16-bit register pair");
					}
				}
			}
			return;
		}

		// ── JP ──
		if (stmt instanceof Jp) {
			Jp jp = (Jp) stmt;
			AsmExpression target = jp.getValue();
			if (jp.getCondition() == null) {
				// Check for JP (HL) / JP (IX) / JP (IY)
				if (target instanceof AsmIndirectExpr) {
					AsmExpression inner = ((AsmIndirectExpr) target).getRight();
					String regName = getRegisterName(inner);
					if (regName != null) {
						String rn = regName.toUpperCase();
						if ("HL".equals(rn)) {
							emit8(out, 0xE9); return;
						} else if ("IX".equals(rn)) {
							emit8(out, 0xDD); emit8(out, 0xE9); return;
						} else if ("IY".equals(rn)) {
							emit8(out, 0xFD); emit8(out, 0xE9); return;
						}
					}
				}
				// JP nn
				int addr = resolveImmediate(target);
				emit8(out, 0xC3);
				emit16LE(out, addr);
			} else {
				int cc = resolveCondition(jp.getCondition());
				int addr = resolveImmediate(target);
				emit8(out, 0xC2 + cc * 8);
				emit16LE(out, addr);
			}
			return;
		}

		// ── SBC ──
		if (stmt instanceof Sbc) {
			Sbc sbc = (Sbc) stmt;
			if (sbc.getValue() != null) {
				// SBC A, operand or SBC HL, rr
				String firstReg = getRegisterName(sbc.getName());
				if (firstReg != null && "HL".equalsIgnoreCase(firstReg)) {
					// SBC HL, rr
					int rr = resolveRegister16(sbc.getValue());
					if (rr >= 0) {
						emit8(out, 0xED);
						emit8(out, 0x42 + rr * 16);
					}
				} else {
					assembleAluOp(sbc.getValue(), 0x98, 0xDE, out);
				}
			} else {
				assembleAluOp(sbc.getName(), 0x98, 0xDE, out);
			}
			return;
		}

		// ── CB-prefix single-operand instructions ──
		if (stmt instanceof Rl)  { assembleCBOp(((Rl) stmt).getName(),  0x10, out); return; }
		if (stmt instanceof Rlc) { assembleCBOp(((Rlc) stmt).getName(), 0x00, out); return; }
		if (stmt instanceof Rr)  { assembleCBOp(((Rr) stmt).getName(),  0x18, out); return; }
		if (stmt instanceof Rrc) { assembleCBOp(((Rrc) stmt).getName(), 0x08, out); return; }
		if (stmt instanceof Sla) { assembleCBOp(((Sla) stmt).getName(), 0x20, out); return; }
		if (stmt instanceof Sra) { assembleCBOp(((Sra) stmt).getName(), 0x28, out); return; }
		if (stmt instanceof Srl) { assembleCBOp(((Srl) stmt).getName(), 0x38, out); return; }

		// ── BIT / SET / RES ──
		if (stmt instanceof Bit) {
			assembleBitOp(((Bit) stmt).getName(), ((Bit) stmt).getValue(), 0x40, out);
			return;
		}
		if (stmt instanceof SetInst) {
			assembleBitOp(((SetInst) stmt).getName(), ((SetInst) stmt).getValue(), 0xC0, out);
			return;
		}
		if (stmt instanceof Res) {
			assembleBitOp(((Res) stmt).getName(), ((Res) stmt).getValue(), 0x80, out);
			return;
		}

		// ── RST ──
		if (stmt instanceof Rst) {
			int val = resolveIntegralLiteral(((Rst) stmt).getValue());
			emit8(out, 0xC7 + val);
			return;
		}

		// ── IM ──
		if (stmt instanceof Im) {
			int mode = resolveIntegralLiteral(((Im) stmt).getValue());
			emit8(out, 0xED);
			switch (mode) {
				case 0: emit8(out, 0x46); break;
				case 1: emit8(out, 0x56); break;
				case 2: emit8(out, 0x5E); break;
				default: warn("Invalid IM mode: " + mode);
			}
			return;
		}

		// ── CALL ──
		if (stmt instanceof AsmCall) {
			AsmCall call = (AsmCall) stmt;
			if (call.getCondition() == null) {
				int addr = resolveImmediate(call.getName());
				emit8(out, 0xCD);
				emit16LE(out, addr);
			} else {
				int cc = resolveCondition(call.getCondition());
				int addr = resolveImmediate(call.getName());
				emit8(out, 0xC4 + cc * 8);
				emit16LE(out, addr);
			}
			return;
		}

		// ── DJNZ ──
		if (stmt instanceof Djnz) {
			int target = resolveImmediate(((Djnz) stmt).getValue());
			emit8(out, 0x10);
			int offset = target - (currentAddress + 1); // +1 because we've already emitted the opcode
			if (!pass1 && (offset < -128 || offset > 127)) {
				warn("DJNZ offset out of range: " + offset);
			}
			emit8(out, offset & 0xFF);
			return;
		}

		// ── JR ──
		if (stmt instanceof Jr) {
			Jr jr = (Jr) stmt;
			int target = resolveImmediate(jr.getValue());
			if (jr.getCondition() == null) {
				emit8(out, 0x18);
			} else {
				int cc = resolveConditionJr(jr.getCondition());
				emit8(out, 0x20 + cc * 8);
			}
			int offset = target - (currentAddress + 1); // +1 because we've already emitted the opcode
			if (!pass1 && (offset < -128 || offset > 127)) {
				warn("JR offset out of range: " + offset);
			}
			emit8(out, offset & 0xFF);
			return;
		}

		// ── EX ──
		if (stmt instanceof Ex) {
			// We handle common cases: EX DE,HL / EX AF,AF' / EX (SP),HL
			Ex ex = (Ex) stmt;
			String first = getRegisterName(ex.getName());
			String second = getRegisterName(ex.getValue());
			if (first != null && second != null) {
				if ("DE".equalsIgnoreCase(first) && "HL".equalsIgnoreCase(second)) {
					emit8(out, 0xEB); return;
				}
				if ("AF".equalsIgnoreCase(first) && "AF'".equalsIgnoreCase(second)) {
					emit8(out, 0x08); return;
				}
			}
			// EX (SP), HL
			if (ex.getName() instanceof AsmIndirectExpr) {
				String inner = getRegisterName(((AsmIndirectExpr) ex.getName()).getRight());
				if (inner != null && "SP".equalsIgnoreCase(inner) && second != null && "HL".equalsIgnoreCase(second)) {
					emit8(out, 0xE3); return;
				}
			}
			warn("Unsupported EX variant");
			return;
		}

		// ── IN ──
		if (stmt instanceof InInst) {
			assembleIn((InInst) stmt, out);
			return;
		}

		// ── OUT ──
		if (stmt instanceof OutInst) {
			assembleOut((OutInst) stmt, out);
			return;
		}

		// ── Z80N zero-operand instructions ──
		if (stmt instanceof Swapnib) { requireZ80N("SWAPNIB"); emit8(out, 0xED); emit8(out, 0x23); return; }
		if (stmt instanceof Mirror)  { requireZ80N("MIRROR");  emit8(out, 0xED); emit8(out, 0x24); return; }
		if (stmt instanceof Mul)     { requireZ80N("MUL");     emit8(out, 0xED); emit8(out, 0x30); return; }
		if (stmt instanceof Pixeldn) { requireZ80N("PIXELDN"); emit8(out, 0xED); emit8(out, 0x93); return; }
		if (stmt instanceof Pixelad) { requireZ80N("PIXELAD"); emit8(out, 0xED); emit8(out, 0x94); return; }
		if (stmt instanceof Setae)   { requireZ80N("SETAE");   emit8(out, 0xED); emit8(out, 0x95); return; }
		if (stmt instanceof Outinb)  { requireZ80N("OUTINB");  emit8(out, 0xED); emit8(out, 0x90); return; }
		if (stmt instanceof Brk)     { requireZ80N("BRK");     emit8(out, 0xDD); emit8(out, 0x01); return; }

		// ── Z80N block operations ──
		if (stmt instanceof Ldix)   { requireZ80N("LDIX");   emit8(out, 0xED); emit8(out, 0xA4); return; }
		if (stmt instanceof Lddx)   { requireZ80N("LDDX");   emit8(out, 0xED); emit8(out, 0xAC); return; }
		if (stmt instanceof Ldirx)  { requireZ80N("LDIRX");  emit8(out, 0xED); emit8(out, 0xB4); return; }
		if (stmt instanceof Lddrx)  { requireZ80N("LDDRX");  emit8(out, 0xED); emit8(out, 0xBC); return; }
		if (stmt instanceof Ldpirx) { requireZ80N("LDPIRX"); emit8(out, 0xED); emit8(out, 0xB7); return; }
		if (stmt instanceof Ldws)   { requireZ80N("LDWS");   emit8(out, 0xED); emit8(out, 0xA5); return; }

		// ── Z80N barrel shifts (all take DE, B) ──
		if (stmt instanceof Bsla) { requireZ80N("BSLA"); emit8(out, 0xED); emit8(out, 0x28); return; }
		if (stmt instanceof Bsra) { requireZ80N("BSRA"); emit8(out, 0xED); emit8(out, 0x29); return; }
		if (stmt instanceof Bsrl) { requireZ80N("BSRL"); emit8(out, 0xED); emit8(out, 0x2A); return; }
		if (stmt instanceof Bsrf) { requireZ80N("BSRF"); emit8(out, 0xED); emit8(out, 0x2B); return; }
		if (stmt instanceof Brlc) { requireZ80N("BRLC"); emit8(out, 0xED); emit8(out, 0x2C); return; }

		// ── Z80N TEST nn ──
		if (stmt instanceof Test) {
			requireZ80N("TEST");
			int val = resolveImmediate(((Test) stmt).getValue());
			emit8(out, 0xED); emit8(out, 0x27);
			emit8(out, val & 0xFF);
			return;
		}

		// ── Z80N NEXTREG ──
		if (stmt instanceof NextReg) {
			requireZ80N("NEXTREG");
			NextReg nr = (NextReg) stmt;
			int reg = resolveImmediate(nr.getName());
			String valReg = getRegisterName(nr.getValue());
			if (valReg != null && "A".equalsIgnoreCase(valReg)) {
				// NEXTREG reg, A → ED 92 reg
				emit8(out, 0xED); emit8(out, 0x92);
				emit8(out, reg & 0xFF);
			} else {
				// NEXTREG reg, val → ED 91 reg val
				int val = resolveImmediate(nr.getValue());
				emit8(out, 0xED); emit8(out, 0x91);
				emit8(out, reg & 0xFF);
				emit8(out, val & 0xFF);
			}
			return;
		}

		// ── Z80N MMU ──
		if (stmt instanceof Mmu) {
			requireZ80N("MMU");
			Mmu mmu = (Mmu) stmt;
			int slot;
			if (mmu.getName() != null) {
				// MMU slot, page form
				slot = resolveImmediate(mmu.getName());
			} else {
				// MMU0..MMU7 form — extract slot number from the keyword in the source text
				INode node = NodeModelUtils.getNode(mmu);
				String text = node != null ? node.getText().trim() : "";
				// Extract digit from MMU0-MMU7
				slot = -1;
				for (int i = 0; i < text.length(); i++) {
					char ch = text.charAt(i);
					if (ch >= '0' && ch <= '7') {
						slot = ch - '0';
						break;
					}
				}
				if (slot < 0) {
					warn("Cannot determine MMU slot from: " + text);
					slot = 0;
				}
			}
			int page = resolveImmediate(mmu.getValue());
			// MMU is assembled as NEXTREG $50+slot, page
			emit8(out, 0xED); emit8(out, 0x91);
			emit8(out, (0x50 + slot) & 0xFF);
			emit8(out, page & 0xFF);
			return;
		}
		
		/* Dealt with by preprocessor */
		if(stmt instanceof AsmInclude) {
			return;
		}

		// If we get here, the instruction/directive is not yet supported — hard fail
		// TODO: Copper directives — CU.WAIT, CU.MOVE, CU.STOP, CU.NOP
		// TODO: DMA directives — DMA.WR0 through DMA.WR6/DMA.CMD
		// TODO: Z88DK directives — CALL_OZ, CALL_PKG, FPP, .ASSUME ADL, C_LINE
		throw new AssemblyException(effectiveSource, translateToOriginalSourceLine(currentLine, effectiveSource),
				"Unsupported instruction/directive: " + stmt.eClass().getName());
	}

	// ─────────────── LD encoding ───────────────

	private void assembleLd(Ld ld, ByteArrayOutputStream out) {
		AsmExpression dest = ld.getName();
		AsmExpression src = ld.getValue();

		boolean destIndirect = dest instanceof AsmIndirectExpr;
		boolean srcIndirect = src instanceof AsmIndirectExpr;

		// ── IX/IY indexed source: LD r, (IX+d) / (IY+d) ──
		if (srcIndirect) {
			IndexedInfo idx = resolveIndexed((AsmIndirectExpr) src);
			if (idx != null) {
				int dr = resolveRegister8(dest);
				if (dr >= 0) {
					emit8(out, idx.prefix);
					emit8(out, 0x46 + dr * 8);
					emit8(out, idx.displacement);
					return;
				}
			}
		}

		// ── IX/IY indexed dest: LD (IX+d), r / LD (IX+d), n ──
		if (destIndirect) {
			IndexedInfo idx = resolveIndexed((AsmIndirectExpr) dest);
			if (idx != null) {
				int sr = srcIndirect ? -1 : resolveRegister8(src);
				if (sr >= 0) {
					emit8(out, idx.prefix);
					emit8(out, 0x70 + sr);
					emit8(out, idx.displacement);
					return;
				}
				// LD (IX+d), n
				if (!srcIndirect) {
					int n = resolveImmediate(src);
					emit8(out, idx.prefix);
					emit8(out, 0x36);
					emit8(out, idx.displacement);
					emit8(out, n & 0xFF);
					return;
				}
			}
		}

		// ── LD IX, nn / LD IY, nn ──
		String destReg = getRegisterName(dest);
		String srcReg = getRegisterName(src);
		int ixiyPrefix = getIXIYPrefix(destReg);
		if (ixiyPrefix > 0 && !srcIndirect && destReg != null) {
			String du = destReg.toUpperCase();
			if ("IX".equals(du) || "IY".equals(du)) {
				// LD IX, nn / LD IY, nn
				if (!srcIndirect) {
					int nn = resolveImmediate(src);
					emit8(out, ixiyPrefix);
					emit8(out, 0x21);
					emit16LE(out, nn);
					return;
				}
			}
		}

		// ── LD SP, IX / LD SP, IY ──
		if ("SP".equalsIgnoreCase(destReg) && srcReg != null) {
			int srcPrefix = getIXIYPrefix(srcReg);
			if (srcPrefix > 0 && ("IX".equalsIgnoreCase(srcReg) || "IY".equalsIgnoreCase(srcReg))) {
				emit8(out, srcPrefix);
				emit8(out, 0xF9);
				return;
			}
		}

		// ── LD (nn), IX / LD (nn), IY ──
		if (destIndirect && !srcIndirect && srcReg != null) {
			int srcPrefix = getIXIYPrefix(srcReg);
			if (srcPrefix > 0 && ("IX".equalsIgnoreCase(srcReg) || "IY".equalsIgnoreCase(srcReg))) {
				int nn = resolveImmediate(((AsmIndirectExpr) dest).getRight());
				emit8(out, srcPrefix);
				emit8(out, 0x22);
				emit16LE(out, nn);
				return;
			}
		}

		// ── LD IX, (nn) / LD IY, (nn) ──
		if (srcIndirect && !destIndirect && destReg != null) {
			int destPrefix = getIXIYPrefix(destReg);
			if (destPrefix > 0 && ("IX".equalsIgnoreCase(destReg) || "IY".equalsIgnoreCase(destReg))) {
				int nn = resolveImmediate(((AsmIndirectExpr) src).getRight());
				emit8(out, destPrefix);
				emit8(out, 0x2A);
				emit16LE(out, nn);
				return;
			}
		}

		// LD r, r' — register to register
		int dr = destIndirect ? -1 : resolveRegister8(dest);
		int sr = srcIndirect ? -1 : resolveRegister8(src);

		if (dr >= 0 && sr >= 0) {
			emit8(out, 0x40 + dr * 8 + sr);
			return;
		}

		// ── LD I, A / LD R, A / LD A, I / LD A, R ──
		// Must be checked before LD r, n to avoid treating I/R as immediates
		if (dr == 7 && srcReg != null) {
			if ("I".equalsIgnoreCase(srcReg)) { emit8(out, 0xED); emit8(out, 0x57); return; }
			if ("R".equalsIgnoreCase(srcReg)) { emit8(out, 0xED); emit8(out, 0x5F); return; }
		}
		if (destReg != null && sr == 7) {
			if ("I".equalsIgnoreCase(destReg)) { emit8(out, 0xED); emit8(out, 0x47); return; }
			if ("R".equalsIgnoreCase(destReg)) { emit8(out, 0xED); emit8(out, 0x4F); return; }
		}

		// LD r, (HL) — load from indirect
		if (dr >= 0 && srcIndirect) {
			String innerReg = getRegisterName(((AsmIndirectExpr) src).getRight());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, 0x40 + dr * 8 + 6);
				return;
			}
		}

		// LD (HL), r — store to indirect
		if (destIndirect && sr >= 0) {
			String innerReg = getRegisterName(((AsmIndirectExpr) dest).getRight());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, 0x70 + sr);
				return;
			}
		}

		// LD r, n — register, immediate 8-bit
		if (dr >= 0 && !srcIndirect) {
			int n = resolveImmediate(src);
			emit8(out, 0x06 + dr * 8);
			emit8(out, n & 0xFF);
			return;
		}

		// LD (HL), n — immediate to indirect
		if (destIndirect && !srcIndirect) {
			String innerReg = getRegisterName(((AsmIndirectExpr) dest).getRight());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				int n = resolveImmediate(src);
				emit8(out, 0x36);
				emit8(out, n & 0xFF);
				return;
			}
		}

		// LD rr, nn — 16-bit register, immediate 16-bit
		int drr = resolveRegister16(dest);
		if (drr >= 0 && !srcIndirect) {
			int nn = resolveImmediate(src);
			emit8(out, 0x01 + drr * 16);
			emit16LE(out, nn);
			return;
		}

		// LD A, (BC) / LD A, (DE)
		if (dr == 7 && srcIndirect) {
			String innerReg = getRegisterName(((AsmIndirectExpr) src).getRight());
			if (innerReg != null) {
				if ("BC".equalsIgnoreCase(innerReg)) { emit8(out, 0x0A); return; }
				if ("DE".equalsIgnoreCase(innerReg)) { emit8(out, 0x1A); return; }
			}
			// LD A, (nn)
			int nn = resolveImmediate(((AsmIndirectExpr) src).getRight());
			emit8(out, 0x3A);
			emit16LE(out, nn);
			return;
		}

		// LD (BC), A / LD (DE), A
		if (destIndirect && sr == 7) {
			String innerReg = getRegisterName(((AsmIndirectExpr) dest).getRight());
			if (innerReg != null) {
				if ("BC".equalsIgnoreCase(innerReg)) { emit8(out, 0x02); return; }
				if ("DE".equalsIgnoreCase(innerReg)) { emit8(out, 0x12); return; }
			}
			// LD (nn), A
			int nn = resolveImmediate(((AsmIndirectExpr) dest).getRight());
			emit8(out, 0x32);
			emit16LE(out, nn);
			return;
		}

		// LD (nn), rr / LD rr, (nn)
		if (destIndirect && !srcIndirect) {
			int srr = resolveRegister16(src);
			if (srr >= 0) {
				int nn = resolveImmediate(((AsmIndirectExpr) dest).getRight());
				if (srr == 2) { // HL
					emit8(out, 0x22);
				} else {
					emit8(out, 0xED);
					emit8(out, 0x43 + srr * 16);
				}
				emit16LE(out, nn);
				return;
			}
		}
		if (srcIndirect && !destIndirect) {
			drr = resolveRegister16(dest);
			if (drr >= 0) {
				int nn = resolveImmediate(((AsmIndirectExpr) src).getRight());
				if (drr == 2) { // HL
					emit8(out, 0x2A);
				} else {
					emit8(out, 0xED);
					emit8(out, 0x4B + drr * 16);
				}
				emit16LE(out, nn);
				return;
			}
		}

		// LD SP, HL
		if ("SP".equalsIgnoreCase(destReg) && "HL".equalsIgnoreCase(srcReg)) {
			emit8(out, 0xF9);
			return;
		}

		warn("Unsupported LD variant");
	}

	// ─────────────── ALU operations (ADD A,x / SUB x etc.) ───────────────

	private void assembleAdd(Add add, ByteArrayOutputStream out) {
		AsmExpression first = add.getName();
		AsmExpression second = add.getValue();

		String firstReg = getRegisterName(first);

		if (second != null && firstReg != null && "HL".equalsIgnoreCase(firstReg)) {
			int rr = resolveRegister16(second);
			if (rr >= 0) {
				emit8(out, 0x09 + rr * 16);
				return;
			}
		}

		// ADD IX, rr / ADD IY, rr
		if (second != null && firstReg != null) {
			int prefix = getIXIYPrefix(firstReg);
			if (prefix > 0 && ("IX".equalsIgnoreCase(firstReg) || "IY".equalsIgnoreCase(firstReg))) {
				String secondReg = getRegisterName(second);
				int rr = -1;
				if (secondReg != null) {
					switch (secondReg.toUpperCase()) {
						case "BC": rr = 0; break;
						case "DE": rr = 1; break;
						case "IX": case "IY": rr = 2; break;
						case "SP": rr = 3; break;
					}
				}
				if (rr >= 0) {
					emit8(out, prefix);
					emit8(out, 0x09 + rr * 16);
					return;
				}
			}
		}

		// ADD A, operand (two-operand form)
		if (second != null) {
			assembleAluOp(second, 0x80, 0xC6, out);
			return;
		}

		// ADD operand (single-operand form: implicit A)
		assembleAluOp(first, 0x80, 0xC6, out);
	}

	private void assembleAluOp(AsmExpression operand, int regBase, int immOpcode, ByteArrayOutputStream out) {
		// Check for indexed (IX+d)/(IY+d)
		if (operand instanceof AsmIndirectExpr) {
			IndexedInfo idx = resolveIndexed((AsmIndirectExpr) operand);
			if (idx != null) {
				emit8(out, idx.prefix);
				emit8(out, regBase + 6);
				emit8(out, idx.displacement);
				return;
			}
			// Check for indirect (HL)
			String innerReg = getRegisterName(((AsmIndirectExpr) operand).getRight());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, regBase + 6);
				return;
			}
		}

		int r = resolveRegister8(operand);
		if (r >= 0) {
			emit8(out, regBase + r);
		} else {
			int n = resolveImmediate(operand);
			emit8(out, immOpcode);
			emit8(out, n & 0xFF);
		}
	}

	// ─────────────── INC / DEC ───────────────

	private void assembleIncDec(AsmExpression operand, boolean isInc, ByteArrayOutputStream out) {
		// Check for (IX+d)/(IY+d) indexed
		if (operand instanceof AsmIndirectExpr) {
			IndexedInfo idx = resolveIndexed((AsmIndirectExpr) operand);
			if (idx != null) {
				emit8(out, idx.prefix);
				emit8(out, isInc ? 0x34 : 0x35);
				emit8(out, idx.displacement);
				return;
			}
			// Check for (HL) indirect
			String innerReg = getRegisterName(((AsmIndirectExpr) operand).getRight());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, isInc ? 0x34 : 0x35);
				return;
			}
		}

		// 8-bit register
		int r = resolveRegister8(operand);
		if (r >= 0) {
			emit8(out, (isInc ? 0x04 : 0x05) + r * 8);
			return;
		}

		// 16-bit register pair
		int rr = resolveRegister16(operand);
		if (rr >= 0) {
			emit8(out, (isInc ? 0x03 : 0x0B) + rr * 16);
			return;
		}

		// IX/IY as 16-bit
		String regName = getRegisterName(operand);
		int prefix = getIXIYPrefix(regName);
		if (prefix > 0 && regName != null && ("IX".equalsIgnoreCase(regName) || "IY".equalsIgnoreCase(regName))) {
			emit8(out, prefix);
			emit8(out, isInc ? 0x23 : 0x2B);
			return;
		}

		warn("Unsupported " + (isInc ? "INC" : "DEC") + " operand");
	}

	// ─────────────── CB-prefix operations (RL, RLC, RR, RRC, SLA, SRA, SRL) ───

	private void assembleCBOp(AsmExpression operand, int baseOpcode, ByteArrayOutputStream out) {
		if (operand instanceof AsmIndirectExpr) {
			IndexedInfo idx = resolveIndexed((AsmIndirectExpr) operand);
			if (idx != null) {
				emit8(out, idx.prefix);
				emit8(out, 0xCB);
				emit8(out, idx.displacement);
				emit8(out, baseOpcode + 6);
				return;
			}
			String innerReg = getRegisterName(((AsmIndirectExpr) operand).getRight());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, 0xCB);
				emit8(out, baseOpcode + 6);
				return;
			}
		}
		int r = resolveRegister8(operand);
		if (r >= 0) {
			emit8(out, 0xCB);
			emit8(out, baseOpcode + r);
		} else {
			warn("CB-prefix instruction requires a register operand");
		}
	}

	// ─────────────── BIT / SET / RES ───────────────

	private void assembleBitOp(AsmExpression bitNum, AsmExpression target, int baseOpcode, ByteArrayOutputStream out) {
		int bit = resolveImmediate(bitNum);
		if (target instanceof AsmIndirectExpr) {
			IndexedInfo idx = resolveIndexed((AsmIndirectExpr) target);
			if (idx != null) {
				emit8(out, idx.prefix);
				emit8(out, 0xCB);
				emit8(out, idx.displacement);
				emit8(out, baseOpcode + bit * 8 + 6);
				return;
			}
			String innerReg = getRegisterName(((AsmIndirectExpr) target).getRight());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, 0xCB);
				emit8(out, baseOpcode + bit * 8 + 6);
				return;
			}
			warn("BIT/SET/RES indirect requires (HL), (IX+d), or (IY+d)");
			return;
		}
		int r = resolveRegister8(target);
		if (r < 0) {
			warn("BIT/SET/RES requires a register operand");
			return;
		}
		emit8(out, 0xCB);
		emit8(out, baseOpcode + bit * 8 + r);
	}

	// ─────────────── Data directive assembly ───────────────

	/**
	 * DEFB / DB / DEFM / DM / BYTE — emit 1 byte per numeric expression,
	 * or each character of a string as a separate byte.
	 */
	private void assembleDefByte(DefByte directive, ByteArrayOutputStream out) {
		for (AsmExpression expr : directive.getData()) {
			String str = resolveString(expr);
			if (str != null) {
				for (int i = 0; i < str.length(); i++) {
					emit8(out, str.charAt(i) & 0xFF);
				}
			} else {
				emit8(out, resolveImmediate(expr) & 0xFF);
			}
		}
	}

	/**
	 * DEFW / DW / WORD — emit 2 bytes little-endian per expression.
	 */
	private void assembleDefWord(DefWord directive, ByteArrayOutputStream out) {
		for (AsmExpression expr : directive.getData()) {
			emit16LE(out, resolveImmediate(expr));
		}
	}

	/**
	 * DEFW_BE / DW_BE / DEFDB / DDB — emit 2 bytes big-endian per expression.
	 */
	private void assembleDefWordBE(DefWordBE directive, ByteArrayOutputStream out) {
		for (AsmExpression expr : directive.getData()) {
			int val = resolveImmediate(expr);
			emit8(out, (val >> 8) & 0xFF);
			emit8(out, val & 0xFF);
		}
	}

	/**
	 * DEFP / DP / PTR — emit 3 bytes little-endian per expression.
	 */
	private void assembleDefPointer(DefPointer directive, ByteArrayOutputStream out) {
		for (AsmExpression expr : directive.getData()) {
			int val = resolveImmediate(expr);
			emit8(out, val & 0xFF);
			emit8(out, (val >> 8) & 0xFF);
			emit8(out, (val >> 16) & 0xFF);
		}
	}

	/**
	 * DEFQ / DQ / DWORD — emit 4 bytes little-endian per expression.
	 */
	private void assembleDefDWord(DefDWord directive, ByteArrayOutputStream out) {
		for (AsmExpression expr : directive.getData()) {
			int val = resolveImmediate(expr);
			emit8(out, val & 0xFF);
			emit8(out, (val >> 8) & 0xFF);
			emit8(out, (val >> 16) & 0xFF);
			emit8(out, (val >> 24) & 0xFF);
		}
	}

	/**
	 * DC — emit string characters as bytes, with bit 7 set on the last byte.
	 */
	private void assembleDefTermString(DefTermString directive, ByteArrayOutputStream out) {
		// Collect all bytes first so we know which is last
		List<Integer> bytes = new ArrayList<>();
		for (AsmExpression expr : directive.getData()) {
			String str = resolveString(expr);
			if (str != null) {
				for (int i = 0; i < str.length(); i++) {
					bytes.add(str.charAt(i) & 0xFF);
				}
			} else {
				bytes.add(resolveImmediate(expr) & 0xFF);
			}
		}
		for (int i = 0; i < bytes.size(); i++) {
			int b = bytes.get(i);
			if (i == bytes.size() - 1) {
				b |= 0x80; // Set bit 7 on last byte
			}
			emit8(out, b);
		}
	}

	/**
	 * DEFS / DS — emit 'count' bytes, each filled with 'fill' (default 0x00).
	 */
	private void assembleDefSpace(DefSpace directive, ByteArrayOutputStream out) {
		int count = resolveImmediate(directive.getCount());
		int fill = defaultFill;
		if (directive.getFill() != null) {
			fill = resolveImmediate(directive.getFill()) & 0xFF;
		}
		for (int i = 0; i < count; i++) {
			emit8(out, fill);
		}
	}

	/**
	 * DEFGROUP { name[=expr] ... } — define a group of symbols.
	 * Only processes {@link AsmGroupedDefine} entries (ignores AsmVarDefine from DEFVARS).
	 */
	private void assembleDefineGroup(DataDefineGroup directive, ByteArrayOutputStream out) {
		for (EObject entry : directive.getDefines()) {
			if (entry instanceof AsmGroupedDefine def) {
				var name = def.getName();
				putSymbol(name).address = resolveImmediate(def.getData());
			}
		}
	}

	// ─────────────── IX/IY indexed addressing helpers ───────────────

	/**
	 * Result of resolving an (IX+d) or (IY+d) indirect operand.
	 */
	private static class IndexedInfo {
		final int prefix; // 0xDD for IX, 0xFD for IY
		final int displacement;
		IndexedInfo(int prefix, int displacement) {
			this.prefix = prefix;
			this.displacement = displacement;
		}
	}

	/**
	 * If the given AsmIndirectExpr contains an IX+d / IY+d / IX / IY expression,
	 * return an IndexedInfo with the prefix byte and displacement.
	 * Returns null if this is not an IX/IY indexed operand.
	 */
	private IndexedInfo resolveIndexed(AsmIndirectExpr indirect) {
		AsmExpression inner = indirect.getRight();

		// (IX) or (IY) — zero displacement
		String regName = getRegisterName(inner);
		if (regName != null) {
			if ("IX".equalsIgnoreCase(regName)) return new IndexedInfo(0xDD, 0);
			if ("IY".equalsIgnoreCase(regName)) return new IndexedInfo(0xFD, 0);
			return null;
		}

		// (IX+d), (IX-d), (IY+d), (IY-d)
		if (inner instanceof AsmBinaryExpr bin) {
			String op = bin.getOp();
			if ("+".equals(op) || "-".equals(op)) {
				String leftReg = getRegisterName(bin.getLeft());
				if (leftReg != null) {
					int prefix;
					if ("IX".equalsIgnoreCase(leftReg)) prefix = 0xDD;
					else if ("IY".equalsIgnoreCase(leftReg)) prefix = 0xFD;
					else return null;
					int d = resolveImmediate(bin.getRight());
					if ("-".equals(op)) d = -d;
					return new IndexedInfo(prefix, d & 0xFF);
				}
			}
		}

		return null;
	}

	/**
	 * Get the IX/IY prefix byte for a register name, or -1 if not IX/IY.
	 */
	private int getIXIYPrefix(String regName) {
		if (regName == null) return -1;
		switch (regName.toUpperCase()) {
			case "IX": case "IXH": case "IXL": return 0xDD;
			case "IY": case "IYH": case "IYL": return 0xFD;
			default: return -1;
		}
	}

	// ─────────────── IN / OUT ───────────────

	/**
	 * IN r, (C)  → ED 40+r*8
	 * IN A, (n)  → DB n
	 */
	private void assembleIn(InInst stmt, ByteArrayOutputStream out) {
		AsmExpression dest = stmt.getName();
		AsmExpression src = stmt.getValue();

		int dr = resolveRegister8(dest);

		if (src instanceof AsmIndirectExpr) {
			AsmExpression inner = ((AsmIndirectExpr) src).getRight();
			String innerReg = getRegisterName(inner);
			if (innerReg != null && "C".equalsIgnoreCase(innerReg)) {
				// IN r, (C)
				if (dr >= 0) {
					emit8(out, 0xED);
					emit8(out, 0x40 + dr * 8);
				} else {
					// IN F, (C) — undocumented, reads flags
					emit8(out, 0xED);
					emit8(out, 0x70);
				}
				return;
			}
			// IN A, (n)
			if (dr == 7) { // A
				int n = resolveImmediate(inner);
				emit8(out, 0xDB);
				emit8(out, n & 0xFF);
				return;
			}
		}

		warn("Unsupported IN variant");
	}

	/**
	 * OUT (C), r  → ED 41+r*8
	 * OUT (n), A  → D3 n
	 */
	private void assembleOut(OutInst stmt, ByteArrayOutputStream out) {
		AsmExpression dest = stmt.getName();
		AsmExpression src = stmt.getValue();

		if (dest instanceof AsmIndirectExpr) {
			AsmExpression inner = ((AsmIndirectExpr) dest).getRight();
			String innerReg = getRegisterName(inner);

			if (innerReg != null && "C".equalsIgnoreCase(innerReg)) {
				// OUT (C), r
				int sr = resolveRegister8(src);
				if (sr >= 0) {
					emit8(out, 0xED);
					emit8(out, 0x41 + sr * 8);
				} else {
					warn("OUT (C) requires a register operand");
				}
				return;
			}

			// OUT (n), A
			int sr = resolveRegister8(src);
			if (sr == 7) { // A
				int n = resolveImmediate(inner);
				emit8(out, 0xD3);
				emit8(out, n & 0xFF);
				return;
			}
		}

		warn("Unsupported OUT variant");
	}

	// ─────────────── Operand resolution helpers ───────────────

	/**
	 * Resolve an 8-bit register name to its 3-bit encoding.
	 * Returns -1 if the operand is not an 8-bit register.
	 * B=0, C=1, D=2, E=3, H=4, L=5, (HL)=6, A=7
	 */
	private int resolveRegister8(AsmExpression operand) {
		String name = getRegisterName(operand);
		if (name == null) return -1;
		switch (name.toUpperCase()) {
			case "B": return 0;
			case "C": return 1;
			case "D": return 2;
			case "E": return 3;
			case "H": return 4;
			case "L": return 5;
			case "A": return 7;
			default:  return -1;
		}
	}

	/**
	 * Resolve a 16-bit register pair to its 2-bit encoding.
	 * BC=0, DE=1, HL=2, SP=3. Returns -1 if not a 16-bit pair.
	 */
	private int resolveRegister16(AsmExpression operand) {
		String name = getRegisterName(operand);
		if (name == null) return -1;
		switch (name.toUpperCase()) {
			case "BC": return 0;
			case "DE": return 1;
			case "HL": return 2;
			case "SP": return 3;
			default:   return -1;
		}
	}

	/**
	 * Resolve a 16-bit register pair for PUSH/POP encoding.
	 * BC=0, DE=1, HL=2, AF=3. Returns -1 if not a pushable pair.
	 */
	private int resolveRegister16Push(AsmExpression operand) {
		String name = getRegisterName(operand);
		if (name == null) return -1;
		switch (name.toUpperCase()) {
			case "BC": return 0;
			case "DE": return 1;
			case "HL": return 2;
			case "AF": return 3;
			default:   return -1;
		}
	}

	/**
	 * Get the register name from an operand, or null if it's not a register.
	 */
	private String getRegisterName(AsmExpression operand) {
		if (operand instanceof AsmRegisterName) {
			return ((AsmRegisterName) operand).getName();
		}
		return null;
	}

	/**
	 * Resolve a condition code to its 3-bit encoding.
	 * NZ=0, Z=1, NC=2, C=3, PO=4, PE=5, P=6, M=7
	 */
	private int resolveCondition(AsmCondition cond) {
		if (cond == null) return -1;
		switch (cond.getName().toUpperCase()) {
			case "NZ": return 0;
			case "Z":  return 1;
			case "NC": return 2;
			case "C":  return 3;
			case "PO": return 4;
			case "PE": return 5;
			case "P":  return 6;
			case "M":  return 7;
			default:   return -1;
		}
	}

	/**
	 * Resolve a condition for JR (only NZ, Z, NC, C are valid).
	 */
	private int resolveConditionJr(AsmCondition cond) {
		switch (cond.getName().toUpperCase()) {
			case "NZ": return 0;
			case "Z":  return 1;
			case "NC": return 2;
			case "C":  return 3;
			default:
				warn("Invalid JR condition: " + cond.getName());
				return 0;
		}
	}

	/**
	 * Resolve an operand to an integer value. Recursively evaluates
	 * expressions including binary operators, unary sign/not, literals,
	 * strings (first char ordinal), and symbols (looked up from the symbol
	 * table populated during pass&nbsp;1).
	 */
	private int resolveImmediate(AsmExpression operand) {
		if (operand instanceof IntegralLiteral) {
			return resolveIntegralLiteral((IntegralLiteral) operand);
		}

		if (operand instanceof AsmPowerExpr pwr) {
			int left = resolveImmediate(pwr.getLeft());
			int right = resolveImmediate(pwr.getRight());
			return (int)Math.pow(left, right);
		}
		
		if (operand instanceof AsmBinaryExpr bin) {
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

		if (operand instanceof AsmEqualityExpr eql) {
			int left = resolveImmediate(eql.getLeft());
			int right = resolveImmediate(eql.getRight());
			switch (eql.getOp()) {
				case "==":  return evaluate(left == right);
				case "=":  return evaluate(left == right);
				case "!=":  return evaluate(left != right);
				case "<>":  return evaluate(left != right);
				case "<=":  return evaluate(left <= right);
				case ">=":  return evaluate(left >= right);
				case "<":  return evaluate(left < right);
				case ">":  return evaluate(left > right);
				default:
					warn("Unknown operator: " + eql.getOp());
					return 0;
			}
		}
		
		if (operand instanceof AsmBitwiseExpr bwise) {
			int left = resolveImmediate(bwise.getLeft());
			int right = resolveImmediate(bwise.getRight());
			switch (bwise.getOp()) {
				case "|":  return left | right;
				case "&":  return left & right;
				case "^":  return left ^ right;
				default:
					warn("Unknown operator: " + bwise.getOp());
					return 0;
			}
		}
		
		
		if (operand instanceof AsmUnaryExpr unary) {
			int val = resolveImmediate(unary.getRight());
			// Determine the sign from the source text node
			INode node = NodeModelUtils.getNode(unary.getRight());
			if (node != null && node.getText().trim().startsWith("-")) {
				return -val;
			}
			return val;
		}
		
		if (operand instanceof AsmNotExpr lnot) {
			int val = resolveImmediate(lnot.getRight());
			switch (lnot.getOp()) {
				case "!":  return val == 0 ? 1 : 0;
				case "~":  return ~val;
				default:
					warn("Unknown operator: " + lnot.getOp());
					return 0;
			}
		}
		
		if (operand instanceof AsmLogicExpr logic) {
			boolean left = resolveImmediate(logic.getLeft()) != 0;
			boolean right = resolveImmediate(logic.getRight()) != 0;
			switch (logic.getOp()) {
				case "&&": return evaluate(left && right);
				case "||":  return evaluate(left || right);
				default:
					warn("Unknown operator: " + logic.getOp());
					return 0;
			}
		}
		
		if (operand instanceof AsmTernaryExpr ternary) {
			return resolveImmediate(ternary.getLeft()) != 0 
					? resolveImmediate(ternary.getRight()) 
					: resolveImmediate(ternary.getElse());
		}
		
		if (operand instanceof StringLiteral) {
			String s = resolveString(operand);
			if (s != null && !s.isEmpty()) {
				return s.charAt(0) & 0xFF;
			}
			return 0;
		}
		if (operand instanceof AsmLabel label) {
			AsmLabelDef def = label.getRef();
			String labelName = (def != null && !def.eIsProxy()) ? def.getName() : null;
			// When the cross-reference is unresolved (e.g. EXTERN symbols have no
			// AsmLabelDef in the grammar), fall back to extracting the text from the
			// node model so we can still look the name up in our symbol table.
			if (labelName == null) {
				INode node = NodeModelUtils.getNode(label);
				if (node != null) {
					labelName = node.getText().trim();
				}
			}
			if (labelName != null) {
				// Strip leading dot — z88dk convention
				if (labelName.startsWith(".")) {
					labelName = labelName.substring(1);
				}
				// Try module-qualified lookup first, then bare name
				// TODO: Support explicit module.label syntax in expressions (needs grammar change)
				String qualifiedName = currentModule + "." + labelName;
				if (symbols.containsKey(qualifiedName)) {
					return symbols.get(qualifiedName).address;
				}
				if (symbols.containsKey(labelName)) {
					return symbols.get(labelName).address;
				}
			}
			// During pass 1, symbols may not yet be defined (forward references) — return 0 silently
			if (pass1) {
				return 0;
			}
			warn("Undefined label: " + (labelName != null ? labelName : "?"));
			return 0;
		}
		if (operand instanceof AsmIndirectExpr) {
			// In immediate contexts (e.g. LD A,(nn)), resolve the inner expression
			return resolveImmediate(((AsmIndirectExpr) operand).getRight());
		}
		

		if (operand instanceof AsmIndexExpr idx) {
			var left = resolveImmediate(idx.getLeft());
			var index = resolveImmediate(idx.getRight());
			return left+index;
		}
		
		warn("Cannot resolve operand to immediate value: " + operand.eClass().getName());
		return 0;
	}

	private int evaluate(boolean bool) {
		return bool ? 1 : 0;
	}
	
	/**
	 * Resolve a constant expression
	 * Returns null if the operand is not a constant.
	 */
	private String resolveConstExpressionAsString(AsmExpression operand) {
		if (operand instanceof StringLiteral) {
			return ((StringLiteral) operand).getValue();
		}
		else if (operand instanceof IntegralLiteral lv) {
			return String.valueOf(resolveIntegralLiteral(lv));
		}
		return null;
	}

	/**
	 * Resolve a StringLiteral to its raw string value (quotes stripped).
	 * Returns null if the operand is not a StringLiteral.
	 */
	private String resolveString(AsmExpression operand) {
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

	// ─────────────── Byte emission helpers ───────────────

	private void emit8(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		currentAddress++;
	}

	private void emit16LE(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);        // low byte
		out.write((value >> 8) & 0xFF); // high byte
		currentAddress += 2;
	}

	private void warn(String message) {
		if (pass1) {
			return; // Suppress warnings during pass 1 — symbols aren't resolved yet
		}
		warnings.add(message);
		if (warningCallback != null && currentLine > 0) {
			warningCallback.warn(effectiveSource, translateToOriginalSourceLine(currentLine, effectiveSource), message);
		}
	}

	/**
	 * Guard that throws if Z80N mode is not enabled.
	 */
	private void requireZ80N(String mnemonic) {
		if (!z80n) {
			throw new AssemblyException(effectiveSource, translateToOriginalSourceLine(currentLine, effectiveSource),
					mnemonic + " requires Z80N mode (use --z80n or withZ80N())");
		}
	}

	private Symbol putSymbol(String symbol) {
		// Strip leading dot — z88dk uses .label to define labels, but
		// the dot is not part of the name (it is referenced without it)
		String name = symbol.startsWith(".") ? symbol.substring(1) : symbol;
		// Store with module-qualified name
		String qualifiedName = namespace.empty() ? name : String.join(".", namespace) + "." + name;
		String moduleQualifiedName = currentModule.isEmpty() ? qualifiedName : currentModule + "." + qualifiedName;
		Symbol sym = symbols.computeIfAbsent(moduleQualifiedName, s -> new Symbol(name));
		sym.section = currentSection;
		sym.module = currentModule;
		// Also store unqualified for cross-module access (PUBLIC/GLOBAL symbols)
		symbols.putIfAbsent(name, sym);
		return sym;
	}
}
