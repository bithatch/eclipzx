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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import uk.co.bithatch.eclipz80.asm.Add;
import uk.co.bithatch.eclipz80.asm.And;
import uk.co.bithatch.eclipz80.asm.AsmCall;
import uk.co.bithatch.eclipz80.asm.AsmCondition;
import uk.co.bithatch.eclipz80.asm.AsmDefcLine;
import uk.co.bithatch.eclipz80.asm.AsmExpression;
import uk.co.bithatch.eclipz80.asm.AsmGroupedDefine;
import uk.co.bithatch.eclipz80.asm.AsmIf;
import uk.co.bithatch.eclipz80.asm.AsmIfDef;
import uk.co.bithatch.eclipz80.asm.AsmIfNDef;
import uk.co.bithatch.eclipz80.asm.AsmInclude;
import uk.co.bithatch.eclipz80.asm.AsmIndirect;
import uk.co.bithatch.eclipz80.asm.AsmLabel;
import uk.co.bithatch.eclipz80.asm.AsmLabelDef;
import uk.co.bithatch.eclipz80.asm.AsmLine;
import uk.co.bithatch.eclipz80.asm.AsmNotExpr;
import uk.co.bithatch.eclipz80.asm.AsmProgram;
import uk.co.bithatch.eclipz80.asm.AsmRegisterName;
import uk.co.bithatch.eclipz80.asm.AsmSignedExpr;
import uk.co.bithatch.eclipz80.asm.AsmStatement;
import uk.co.bithatch.eclipz80.asm.AsmStatementLine;
import uk.co.bithatch.eclipz80.asm.BinaryExpr;
import uk.co.bithatch.eclipz80.asm.Bit;
import uk.co.bithatch.eclipz80.asm.Ccf;
import uk.co.bithatch.eclipz80.asm.Cp;
import uk.co.bithatch.eclipz80.asm.Cpd;
import uk.co.bithatch.eclipz80.asm.Cpdr;
import uk.co.bithatch.eclipz80.asm.Cpi;
import uk.co.bithatch.eclipz80.asm.Cpir;
import uk.co.bithatch.eclipz80.asm.Cpl;
import uk.co.bithatch.eclipz80.asm.DI;
import uk.co.bithatch.eclipz80.asm.Daa;
import uk.co.bithatch.eclipz80.asm.DataDefineGroup;
import uk.co.bithatch.eclipz80.asm.Dec;
import uk.co.bithatch.eclipz80.asm.DefByte;
import uk.co.bithatch.eclipz80.asm.DefDWord;
import uk.co.bithatch.eclipz80.asm.DefPointer;
import uk.co.bithatch.eclipz80.asm.DefSpace;
import uk.co.bithatch.eclipz80.asm.DefTermString;
import uk.co.bithatch.eclipz80.asm.DefWord;
import uk.co.bithatch.eclipz80.asm.DefWordBE;
import uk.co.bithatch.eclipz80.asm.Define;
import uk.co.bithatch.eclipz80.asm.Djnz;
import uk.co.bithatch.eclipz80.asm.EI;
import uk.co.bithatch.eclipz80.asm.Ex;
import uk.co.bithatch.eclipz80.asm.Exx;
import uk.co.bithatch.eclipz80.asm.Halt;
import uk.co.bithatch.eclipz80.asm.Im;
import uk.co.bithatch.eclipz80.asm.Inc;
import uk.co.bithatch.eclipz80.asm.IncBin;
import uk.co.bithatch.eclipz80.asm.Ind;
import uk.co.bithatch.eclipz80.asm.Indr;
import uk.co.bithatch.eclipz80.asm.Ini;
import uk.co.bithatch.eclipz80.asm.Inir;
import uk.co.bithatch.eclipz80.asm.IntegralLiteral;
import uk.co.bithatch.eclipz80.asm.Jp;
import uk.co.bithatch.eclipz80.asm.Jr;
import uk.co.bithatch.eclipz80.asm.LabelEQULine;
import uk.co.bithatch.eclipz80.asm.LabelOnlyLine;
import uk.co.bithatch.eclipz80.asm.Ld;
import uk.co.bithatch.eclipz80.asm.Ldd;
import uk.co.bithatch.eclipz80.asm.Lddr;
import uk.co.bithatch.eclipz80.asm.Ldi;
import uk.co.bithatch.eclipz80.asm.Ldir;
import uk.co.bithatch.eclipz80.asm.Neg;
import uk.co.bithatch.eclipz80.asm.Nop;
import uk.co.bithatch.eclipz80.asm.OrInst;
import uk.co.bithatch.eclipz80.asm.Org;
import uk.co.bithatch.eclipz80.asm.Otdr;
import uk.co.bithatch.eclipz80.asm.Otir;
import uk.co.bithatch.eclipz80.asm.Outd;
import uk.co.bithatch.eclipz80.asm.Outi;
import uk.co.bithatch.eclipz80.asm.Pop;
import uk.co.bithatch.eclipz80.asm.Push;
import uk.co.bithatch.eclipz80.asm.Res;
import uk.co.bithatch.eclipz80.asm.Ret;
import uk.co.bithatch.eclipz80.asm.Reti;
import uk.co.bithatch.eclipz80.asm.Retn;
import uk.co.bithatch.eclipz80.asm.Rl;
import uk.co.bithatch.eclipz80.asm.Rla;
import uk.co.bithatch.eclipz80.asm.Rlc;
import uk.co.bithatch.eclipz80.asm.Rlca;
import uk.co.bithatch.eclipz80.asm.Rld;
import uk.co.bithatch.eclipz80.asm.Rr;
import uk.co.bithatch.eclipz80.asm.Rra;
import uk.co.bithatch.eclipz80.asm.Rrc;
import uk.co.bithatch.eclipz80.asm.Rrca;
import uk.co.bithatch.eclipz80.asm.Rrd;
import uk.co.bithatch.eclipz80.asm.Rst;
import uk.co.bithatch.eclipz80.asm.Sbc;
import uk.co.bithatch.eclipz80.asm.Scf;
import uk.co.bithatch.eclipz80.asm.SetInst;
import uk.co.bithatch.eclipz80.asm.Sla;
import uk.co.bithatch.eclipz80.asm.Sra;
import uk.co.bithatch.eclipz80.asm.Srl;
import uk.co.bithatch.eclipz80.asm.StringLiteral;
import uk.co.bithatch.eclipz80.asm.Sub;
import uk.co.bithatch.eclipz80.asm.Xor;

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
 */
public class Z80Assembler {
	
	public interface Results {
		Path mapFile();
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
	private final Map<String, String> defines;
	private final Map<String, Integer> labels = new LinkedHashMap<>();
	private int currentAddress = 0;
	private String effectiveSource;
	private Path sourceDir;
	private int currentLine = -1;
	private boolean pass1;
	private WarningCallback warningCallback;

	private final Optional<Path> mapFile;
	private final Optional<Path> outputDir;
	private final boolean mapEnabled;
	private final boolean farAddresses;
	private final List<MapEntry> mapEntries = new ArrayList<>();

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
		private boolean mapEnabled;
		private boolean farAddresses;
		private final Map<String, String> defines = new LinkedHashMap<>();
		private WarningCallback warningCallback;

		private Builder() {}

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
		 * Set a single defines given its name and value. Value
		 * may be indicating it will evaluated to true but won't
		 * expand to anything.
		 * 
		 * @param name name
		 * @param value value
		 */
		public Builder withDefine(String name, String value) {
			defines.put(name, value);
			return this;
		}
		

		/**
		 * Set the defines. Each string can be either just the 
		 * key, or key=value format. The former will result in a 
		 * an empty define (i.e. still evaluates to true).
		 * 
		 * @param defineSpecs define specs
		 */
		public Builder withDefines(String... defines) {
			return withDefines(Arrays.asList(defines));
		}

		/**
		 * Set the defines. Each string can be either just the 
		 * key, or key=value format. The former will result in a 
		 * an empty define (i.e. still evaluates to true).
		 * 
		 * @param defineSpecs define specs
		 */
		public Builder withDefines(Collection<String> defineSpecs) {
			defineSpecs.forEach(d -> {
				var idx = d.indexOf('=');
				defines.put(
					idx == -1 ? d : d.substring(0, idx), 
					idx == -1 ? null : d.substring(idx + 1)
				);
			});
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
		this.defines = new LinkedHashMap<>();
		this.outputDir = Optional.empty();
	}

	private Z80Assembler(Builder builder) {
		this.outputDir = builder.outputDir;
		this.mapFile = builder.mapFile;
		this.mapEnabled = builder.mapEnabled;
		this.farAddresses = builder.farAddresses;
		this.defines = new LinkedHashMap<>(builder.defines);
		this.warningCallback = builder.warningCallback;
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
	 * discarded), pass 2 emits the final machine code with all labels resolved.
	 * If a map file was configured, the .zmap file is written after assembly.
	 */
	public Results assemble(String sourceFileName, AsmProgram program, OutputStream out) {
		warnings.clear();
		currentAddress = 0;
		labels.clear();
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

		// ── Pass 1: collect label addresses (output discarded) ──
		pass1 = true;
		assembleLines(program, new ByteArrayOutputStream());

		// ── Pass 2: emit final machine code with labels resolved ──
		pass1 = false;
		currentAddress = 0;
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

	// ─────────────── Program / line assembly ───────────────

	/**
	 * Walk the lines of an {@link AsmProgram} and assemble each statement.
	 * Used both for the top-level program and for conditional branch bodies.
	 */
	private void assembleLines(AsmProgram program, ByteArrayOutputStream out) {
		for (AsmLine line : program.getLines()) {

			// ── Label-only line (e.g. "start:" or ".loop:") ──
			if (line instanceof LabelOnlyLine) {
				LabelOnlyLine lol = (LabelOnlyLine) line;
				if (pass1 && lol.getLabelDef() != null) {
					labels.put(lol.getLabelDef().getName(), currentAddress);
				}
				continue;
			}

			// ── EQU line (e.g. "SCREEN_ADDR EQU $4000") ──
			if (line instanceof LabelEQULine) {
				LabelEQULine equ = (LabelEQULine) line;
				if (pass1 && equ.getLabelDef() != null) {
					// TODO: forward references in EQU expressions are not yet supported
					labels.put(equ.getLabelDef().getName(), resolveImmediate(equ.getValue()));
				}
				continue;
			}

			// ── DEFC line (e.g. "DEFC name = expr") ──
			if (line instanceof AsmDefcLine) {
				AsmDefcLine defc = (AsmDefcLine) line;
				if (pass1 && defc.getLabelDef() != null) {
					// TODO: forward references in DEFC expressions are not yet supported
					labels.put(defc.getLabelDef().getName(), resolveImmediate(defc.getValue()));
				}
				continue;
			}

			// ── Statement line (may have an optional label prefix) ──
			if (line instanceof AsmStatementLine) {
				AsmStatementLine stmtLine = (AsmStatementLine) line;

				// Record label on this statement line (e.g. "message: db ...")
				if (pass1 && stmtLine.getLabelDef() != null) {
					labels.put(stmtLine.getLabelDef().getName(), currentAddress);
				}

				// Record line-to-address mapping before emitting
				int lineNumber = getLineNumber(stmtLine);
				String lineFile = getSourceFile(stmtLine, effectiveSource);
				this.currentLine = lineNumber;

				if (!pass1 && lineNumber > 0) {
					mapEntries.add(new MapEntry(lineFile, lineNumber, currentAddress & addressMask()));
				}

				for (AsmStatement stmt : stmtLine.getStatements()) {
					assembleStatement(stmt, out);
				}
			}
			// Other line types (NUMERIC_LABEL, LOCAL, etc.) are ignored for now
		}
	}

	// ─────────────── Statement dispatch ───────────────

	private void assembleStatement(AsmStatement stmt, ByteArrayOutputStream out) {
		// ── Directives ──
		if (stmt instanceof Org) {
			currentAddress = resolveIntegralLiteral(((Org) stmt).getValue());
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
		if(stmt instanceof Define) {
			assembleDefine((Define) stmt, out);
			return;
		}
		if (stmt instanceof DataDefineGroup) {
			assembleDefineGroup((DataDefineGroup) stmt, out);
			return;
		}
		if (stmt instanceof IncBin) {
			assembleIncBin((IncBin) stmt, out);
			return;
		}
		if (stmt instanceof AsmInclude) {
			assembleInclude((AsmInclude) stmt, out);
			return;
		}

		// ── Conditional compilation ──
		if (stmt instanceof AsmIf) {
			assembleIf((AsmIf) stmt, out);
			return;
		}
		if (stmt instanceof AsmIfDef) {
			assembleIfDef((AsmIfDef) stmt, out);
			return;
		}
		if (stmt instanceof AsmIfNDef) {
			assembleIfNDef((AsmIfNDef) stmt, out);
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
		if (stmt instanceof Push) {
			AsmExpression reg = ((Push) stmt).getRegister();
			int rr = resolveRegister16Push(reg);
			if (rr >= 0) {
				emit8(out, 0xC5 + rr * 16);
			} else {
				// PUSH nn (Z80N) — handle immediate
				int val = resolveImmediate(reg);
				emit8(out, 0xED); emit8(out, 0x8A);
				// Z80N PUSH nn is big-endian
				emit8(out, (val >> 8) & 0xFF);
				emit8(out, val & 0xFF);
			}
			return;
		}
		if (stmt instanceof Pop) {
			int rr = resolveRegister16Push(((Pop) stmt).getRegister());
			if (rr >= 0) {
				emit8(out, 0xC1 + rr * 16);
			} else {
				warn("POP requires a 16-bit register pair");
			}
			return;
		}

		// ── JP ──
		if (stmt instanceof Jp) {
			Jp jp = (Jp) stmt;
			AsmExpression target = jp.getValue();
			if (jp.getCondition() == null) {
				// Check for JP (HL) / JP (IX) / JP (IY)
				if (target instanceof AsmIndirect) {
					AsmExpression inner = ((AsmIndirect) target).getExpr();
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
			if (ex.getName() instanceof AsmIndirect) {
				String inner = getRegisterName(((AsmIndirect) ex.getName()).getExpr());
				if (inner != null && "SP".equalsIgnoreCase(inner) && second != null && "HL".equalsIgnoreCase(second)) {
					emit8(out, 0xE3); return;
				}
			}
			warn("Unsupported EX variant");
			return;
		}

		// If we get here, the instruction/directive is not yet supported — hard fail
		throw new AssemblyException(effectiveSource, currentLine,
				"Unsupported instruction/directive: " + stmt.eClass().getName());
	}

	// ─────────────── LD encoding ───────────────

	private void assembleLd(Ld ld, ByteArrayOutputStream out) {
		AsmExpression dest = ld.getName();
		AsmExpression src = ld.getValue();

		boolean destIndirect = dest instanceof AsmIndirect;
		boolean srcIndirect = src instanceof AsmIndirect;

		// LD r, r' — register to register
		int dr = destIndirect ? -1 : resolveRegister8(dest);
		int sr = srcIndirect ? -1 : resolveRegister8(src);

		if (dr >= 0 && sr >= 0) {
			// LD r, r' (but LD (HL),(HL) is HALT=0x76, should not happen)
			emit8(out, 0x40 + dr * 8 + sr);
			return;
		}

		// LD r, (HL) — load from indirect
		if (dr >= 0 && srcIndirect) {
			String innerReg = getRegisterName(((AsmIndirect) src).getExpr());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, 0x40 + dr * 8 + 6); // 6 = (HL) position
				return;
			}
		}

		// LD (HL), r — store to indirect
		if (destIndirect && sr >= 0) {
			String innerReg = getRegisterName(((AsmIndirect) dest).getExpr());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, 0x70 + sr); // 0x70 = 0x40 + 6*8
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
			String innerReg = getRegisterName(((AsmIndirect) dest).getExpr());
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
		if (dr == 7 && srcIndirect) { // A = 7
			String innerReg = getRegisterName(((AsmIndirect) src).getExpr());
			if (innerReg != null) {
				if ("BC".equalsIgnoreCase(innerReg)) { emit8(out, 0x0A); return; }
				if ("DE".equalsIgnoreCase(innerReg)) { emit8(out, 0x1A); return; }
			}
			// LD A, (nn)
			int nn = resolveImmediate(((AsmIndirect) src).getExpr());
			emit8(out, 0x3A);
			emit16LE(out, nn);
			return;
		}

		// LD (BC), A / LD (DE), A
		if (destIndirect && sr == 7) { // A = 7
			String innerReg = getRegisterName(((AsmIndirect) dest).getExpr());
			if (innerReg != null) {
				if ("BC".equalsIgnoreCase(innerReg)) { emit8(out, 0x02); return; }
				if ("DE".equalsIgnoreCase(innerReg)) { emit8(out, 0x12); return; }
			}
			// LD (nn), A
			int nn = resolveImmediate(((AsmIndirect) dest).getExpr());
			emit8(out, 0x32);
			emit16LE(out, nn);
			return;
		}

		// LD (nn), rr / LD rr, (nn)
		if (destIndirect && !srcIndirect) {
			int srr = resolveRegister16(src);
			if (srr >= 0) {
				int nn = resolveImmediate(((AsmIndirect) dest).getExpr());
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
				int nn = resolveImmediate(((AsmIndirect) src).getExpr());
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
		String destReg = getRegisterName(dest);
		String srcReg = getRegisterName(src);
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
			// ADD HL, rr
			int rr = resolveRegister16(second);
			if (rr >= 0) {
				emit8(out, 0x09 + rr * 16);
				return;
			}
		}

		// ADD A, operand (two-operand form: ADD A, r/n)
		if (second != null) {
			assembleAluOp(second, 0x80, 0xC6, out);
			return;
		}

		// ADD operand (single-operand form: implicit A)
		assembleAluOp(first, 0x80, 0xC6, out);
	}

	private void assembleAluOp(AsmExpression operand, int regBase, int immOpcode, ByteArrayOutputStream out) {
		// Check for indirect (HL)
		if (operand instanceof AsmIndirect) {
			String innerReg = getRegisterName(((AsmIndirect) operand).getExpr());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				emit8(out, regBase + 6); // (HL) = slot 6
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
		// Check for (HL) indirect
		if (operand instanceof AsmIndirect) {
			String innerReg = getRegisterName(((AsmIndirect) operand).getExpr());
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

		// 16-bit register
		int rr = resolveRegister16(operand);
		if (rr >= 0) {
			emit8(out, (isInc ? 0x03 : 0x0B) + rr * 16);
			return;
		}

		warn("Unsupported " + (isInc ? "INC" : "DEC") + " operand");
	}

	// ─────────────── CB-prefix operations (RL, RLC, RR, RRC, SLA, SRA, SRL) ───

	private void assembleCBOp(AsmExpression operand, int baseOpcode, ByteArrayOutputStream out) {
		if (operand instanceof AsmIndirect) {
			String innerReg = getRegisterName(((AsmIndirect) operand).getExpr());
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
		int r;
		if (target instanceof AsmIndirect) {
			String innerReg = getRegisterName(((AsmIndirect) target).getExpr());
			if (innerReg != null && "HL".equalsIgnoreCase(innerReg)) {
				r = 6;
			} else {
				warn("BIT/SET/RES indirect requires (HL)");
				return;
			}
		} else {
			r = resolveRegister8(target);
			if (r < 0) {
				warn("BIT/SET/RES requires a register operand");
				return;
			}
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
		int fill = 0x00;
		if (directive.getFill() != null) {
			fill = resolveImmediate(directive.getFill()) & 0xFF;
		}
		for (int i = 0; i < count; i++) {
			emit8(out, fill);
		}
	}

	/**
	 * DEFINE name[=constr-expression] - Define set of symbols
	 */
	private void assembleDefine(Define directive, ByteArrayOutputStream out) {
		for(var def : directive.getDefines()) {
			var name = def.getName();
			var val = resolveConstExpressionAsString(def.getData());
			defines.put(name, val);
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
				var val = resolveConstExpressionAsString(def.getData());
				defines.put(name, val);
			}
		}
	}

	/**
	 * BINARY / INCBIN — load a binary file at the current location in the
	 * object file. The file path is resolved relative to the source file
	 * being assembled.
	 */
	private void assembleIncBin(IncBin directive, ByteArrayOutputStream out) {
		String fileName = directive.getFile();
		if (fileName == null || fileName.isEmpty()) {
			warn("INCBIN: no file specified");
			return;
		}
		Path filePath = sourceDir.resolve(fileName);
		try {
			byte[] data = Files.readAllBytes(filePath);
			for (byte b : data) {
				emit8(out, b & 0xFF);
			}
		} catch (IOException e) {
			warn("INCBIN: cannot read file '" + filePath + "': " + e.getMessage());
		}
	}

	/**
	 * INCLUDE "file" — parse and assemble the included source file at the
	 * current position. The included file must already be loaded into the
	 * resource set (Xtext's {@code importURI} mechanism handles this).
	 * Source-to-address map entries use the included file's name.
	 */
	private void assembleInclude(AsmInclude directive, ByteArrayOutputStream out) {
		String importURI = directive.getImportURI();
		if (importURI == null || importURI.isEmpty()) {
			warn("INCLUDE: no file specified");
			return;
		}

		// Strip surrounding quotes if present (grammar's FileSpec uses STRING terminal)
		importURI = stripQuotes(importURI);

		// Resolve the include path relative to the containing resource
		Resource containingResource = directive.eResource();
		if (containingResource == null) {
			warn("INCLUDE: cannot resolve '" + importURI + "' — no containing resource");
			return;
		}

		URI baseURI = containingResource.getURI();
		URI resolvedURI = URI.createFileURI(importURI).resolve(baseURI);

		// Look up or load the resource from the resource set
		ResourceSet resourceSet = containingResource.getResourceSet();
		Resource includedResource = null;
		try {
			includedResource = resourceSet.getResource(resolvedURI, true);
		} catch (Exception e) {
			warn("INCLUDE: cannot load '" + importURI + "': " + e.getMessage());
			return;
		}

		if (includedResource == null || includedResource.getContents().isEmpty()) {
			warn("INCLUDE: empty or unresolvable resource '" + importURI + "'");
			return;
		}

		if (!(includedResource.getContents().get(0) instanceof AsmProgram)) {
			warn("INCLUDE: resource '" + importURI + "' does not contain an AsmProgram");
			return;
		}

		AsmProgram includedProgram = (AsmProgram) includedResource.getContents().get(0);

		// Save and switch source context for map entries
		String previousSource = this.effectiveSource;
		Path previousSourceDir = this.sourceDir;

		this.effectiveSource = resolvedURI.lastSegment();
		if (resolvedURI.isFile()) {
			this.sourceDir = Path.of(resolvedURI.toFileString()).getParent();
		}

		// Assemble the included program inline
		assembleLines(includedProgram, out);

		// Restore source context
		this.effectiveSource = previousSource;
		this.sourceDir = previousSourceDir;
	}

	// ─────────────── Conditional compilation ───────────────

	/**
	 * IF condition ... ELIF ... ELSE ... ENDIF
	 * Evaluates expression conditions; a non-zero result is true.
	 */
	private void assembleIf(AsmIf directive, ByteArrayOutputStream out) {
		// Check the primary IF condition
		if (resolveImmediate(directive.getCondition()) != 0) {
			assembleLines(directive.getProgram(), out);
			return;
		}

		// Check ELIF branches
		EList<AsmExpression> elifConditions = directive.getElifCondition();
		EList<AsmProgram> elifPrograms = directive.getElifProgram();
		for (int i = 0; i < elifConditions.size(); i++) {
			if (resolveImmediate(elifConditions.get(i)) != 0) {
				assembleLines(elifPrograms.get(i), out);
				return;
			}
		}

		// ELSE fallback
		if (directive.getElseProgram() != null) {
			assembleLines(directive.getElseProgram(), out);
		}
	}

	/**
	 * IFDEF name ... ELIFDEF ... ELSE ... ENDIF
	 * Checks whether a symbol is present in the defines map.
	 */
	private void assembleIfDef(AsmIfDef directive, ByteArrayOutputStream out) {
		// Check the primary IFDEF name
		if (defines.containsKey(directive.getName())) {
			assembleLines(directive.getProgram(), out);
			return;
		}

		// Check ELIFDEF branches
		EList<String> elifNames = directive.getElifName();
		EList<AsmProgram> elifPrograms = directive.getElifProgram();
		for (int i = 0; i < elifNames.size(); i++) {
			if (defines.containsKey(elifNames.get(i))) {
				assembleLines(elifPrograms.get(i), out);
				return;
			}
		}

		// ELSE fallback
		if (directive.getElseProgram() != null) {
			assembleLines(directive.getElseProgram(), out);
		}
	}

	/**
	 * IFNDEF name ... ELIFNDEF ... ELSE ... ENDIF
	 * Checks whether a symbol is <em>not</em> present in the defines map.
	 */
	private void assembleIfNDef(AsmIfNDef directive, ByteArrayOutputStream out) {
		// Check the primary IFNDEF name
		if (!defines.containsKey(directive.getName())) {
			assembleLines(directive.getProgram(), out);
			return;
		}

		// Check ELIFNDEF branches
		EList<String> elifNames = directive.getElifName();
		EList<AsmProgram> elifPrograms = directive.getElifProgram();
		for (int i = 0; i < elifNames.size(); i++) {
			if (!defines.containsKey(elifNames.get(i))) {
				assembleLines(elifPrograms.get(i), out);
				return;
			}
		}

		// ELSE fallback
		if (directive.getElseProgram() != null) {
			assembleLines(directive.getElseProgram(), out);
		}
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
	 * strings (first char ordinal), and labels (looked up from the symbol
	 * table populated during pass&nbsp;1).
	 */
	private int resolveImmediate(AsmExpression operand) {
		if (operand instanceof IntegralLiteral) {
			return resolveIntegralLiteral((IntegralLiteral) operand);
		}
		if (operand instanceof BinaryExpr) {
			BinaryExpr bin = (BinaryExpr) operand;
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
		if (operand instanceof AsmSignedExpr) {
			AsmSignedExpr signed = (AsmSignedExpr) operand;
			int val = resolveImmediate(signed.getExpr());
			// Determine the sign from the source text node
			INode node = NodeModelUtils.getNode(signed);
			if (node != null && node.getText().trim().startsWith("-")) {
				return -val;
			}
			return val;
		}
		if (operand instanceof AsmNotExpr) {
			int val = resolveImmediate(((AsmNotExpr) operand).getExpr());
			return val == 0 ? 1 : 0;
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
			if (labelName != null && labels.containsKey(labelName)) {
				return labels.get(labelName);
			}
			// During pass 1, labels may not yet be defined (forward references) — return 0 silently
			if (pass1) {
				return 0;
			}
			warn("Undefined label: " + (labelName != null ? labelName : "?"));
			return 0;
		}
		if (operand instanceof AsmIndirect) {
			// In immediate contexts (e.g. LD A,(nn)), resolve the inner expression
			return resolveImmediate(((AsmIndirect) operand).getExpr());
		}
		warn("Cannot resolve operand to immediate value: " + operand.eClass().getName());
		return 0;
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
			return; // Suppress warnings during pass 1 — labels aren't resolved yet
		}
		warnings.add(message);
		if (warningCallback != null && currentLine > 0) {
			warningCallback.warn(effectiveSource, currentLine, message);
		}
	}

	/**
	 * Strip surrounding quotes from a string, if present.
	 */
	private String stripQuotes(String s) {
		s = s.trim();
		if (s.startsWith("\"") && s.endsWith("\"")) {
			return s.substring(1, s.length() - 1);
		}
		if (s.startsWith("'") && s.endsWith("'")) {
			return s.substring(1, s.length() - 1);
		}
		return s;
	}
}
