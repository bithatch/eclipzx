package uk.co.bithatch.eclipz80.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import uk.co.bithatch.eclipz80.asm.*;

/**
 * A simple Z80 assembler that walks an Xtext-parsed {@link AsmProgram} AST and
 * emits raw Z80 machine code. Currently supports the subset of instructions
 * needed for basic programs (no labels, no expression evaluation beyond
 * literals).
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

	private final List<String> warnings = new ArrayList<>();
	private int currentAddress = 0;

	private Path mapFile;
	private final boolean mapEnabled;
	private final boolean farAddresses;
	private final String sourceFileName;
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
		private Path mapFile;
		private boolean mapEnabled;
		private boolean farAddresses;
		private String sourceFileName;

		private Builder() {}

		/**
		 * Enable .zmap output. The map file path will be derived from the
		 * binary output path (replacing the extension with {@code .zmap}).
		 * This acts as a flag — the actual path is resolved at write time
		 * if no explicit path is given via {@link #withMap(Path)}.
		 */
		public Builder withMap() {
			this.mapEnabled = true;
			return this;
		}

		/**
		 * Enable .zmap output and specify an explicit file path.
		 */
		public Builder withMap(Path mapFile) {
			this.mapFile = mapFile;
			this.mapEnabled = true;
			return this;
		}

		/**
		 * Use 32-bit far addresses (Z88DK style) in map output.
		 * By default, standard 16-bit addresses are used.
		 */
		public Builder withFarAddresses() {
			this.farAddresses = true;
			return this;
		}

		/**
		 * Set the source file name used as the default in map entries.
		 * If not set, the assembler will attempt to derive it from the
		 * Xtext resource URI.
		 */
		public Builder withSourceFileName(String sourceFileName) {
			this.sourceFileName = sourceFileName;
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
		this.mapFile = null;
		this.mapEnabled = false;
		this.farAddresses = false;
		this.sourceFileName = null;
	}

	private Z80Assembler(Builder builder) {
		this.mapFile = builder.mapFile;
		this.mapEnabled = builder.mapEnabled;
		this.farAddresses = builder.farAddresses;
		this.sourceFileName = builder.sourceFileName;
	}

	/**
	 * Returns whether map generation was requested (via builder).
	 */
	boolean isMapEnabled() {
		return mapEnabled;
	}

	/**
	 * Assemble the program and return raw bytes.
	 */
	public byte[] assemble(AsmProgram program) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		assemble(program, baos);
		return baos.toByteArray();
	}

	/**
	 * Assemble the program, writing bytes to the given output stream.
	 * If a map file was configured, the .zmap file is written after assembly.
	 */
	public void assemble(AsmProgram program, OutputStream out) {
		warnings.clear();
		currentAddress = 0;
		mapEntries.clear();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// Derive source file name from the resource URI if not explicitly set
		String effectiveSource = sourceFileName;
		if (effectiveSource == null && program.eResource() != null) {
			effectiveSource = program.eResource().getURI().lastSegment();
		}
		if (effectiveSource == null) {
			effectiveSource = "unknown";
		}

		for (AsmLine line : program.getLines()) {
			if (line instanceof AsmStatementLine) {
				AsmStatementLine stmtLine = (AsmStatementLine) line;

				// Record line-to-address mapping before emitting
				int lineNumber = getLineNumber(stmtLine);
				String lineFile = getSourceFile(stmtLine, effectiveSource);
				if (lineNumber > 0) {
					mapEntries.add(new MapEntry(lineFile, lineNumber, currentAddress & addressMask()));
				}

				for (AsmStatement stmt : stmtLine.getStatements()) {
					assembleStatement(stmt, baos);
				}
			}
			// Other line types (labels, EQU, etc.) are ignored for now
		}

		try {
			baos.writeTo(out);
		} catch (IOException e) {
			throw new RuntimeException("Failed to write assembled output", e);
		}

		// Write map file if configured
		if (mapEnabled && mapFile != null) {
			writeMapFile(effectiveSource);
		}
	}

	/**
	 * Assemble the program, writing bytes to the given output stream,
	 * and derive the map file path from the given binary output path
	 * (replacing the extension with {@code .zmap}).
	 * <p>
	 * This is a convenience for callers who used {@link Builder#withMap()}
	 * without an explicit path.
	 */
	public void assemble(AsmProgram program, OutputStream out, Path binaryOutputPath) {
		if (mapEnabled && mapFile == null && binaryOutputPath != null) {
			// Auto-derive map path from binary output path
			String binName = binaryOutputPath.getFileName().toString();
			int dot = binName.lastIndexOf('.');
			String baseName = dot >= 0 ? binName.substring(0, dot) : binName;
			this.mapFile = binaryOutputPath.resolveSibling(baseName + ".zmap");
		}
		assemble(program, out);
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

	private void writeMapFile(String defaultSource) {
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

	// ─────────────── Statement dispatch ───────────────

	private void assembleStatement(AsmStatement stmt, ByteArrayOutputStream out) {
		// ── Directives ──
		if (stmt instanceof Org) {
			currentAddress = resolveIntegralLiteral(((Org) stmt).getValue());
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
			// For now, only support immediate offset (no label resolution)
			int offset = resolveImmediate(((Djnz) stmt).getValue());
			emit8(out, 0x10);
			emit8(out, offset & 0xFF);
			return;
		}

		// ── JR ──
		if (stmt instanceof Jr) {
			Jr jr = (Jr) stmt;
			int offset = resolveImmediate(jr.getValue());
			if (jr.getCondition() == null) {
				emit8(out, 0x18);
			} else {
				int cc = resolveConditionJr(jr.getCondition());
				emit8(out, 0x20 + cc * 8);
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

		// If we get here, the instruction is not yet supported
		warn("Unsupported instruction: " + stmt.eClass().getName());
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
	 * Resolve an operand to an integer value. Handles IntegralLiteral
	 * (decimal via getValue(), hex/bin via getLitvalue()).
	 */
	private int resolveImmediate(AsmExpression operand) {
		if (operand instanceof IntegralLiteral) {
			return resolveIntegralLiteral((IntegralLiteral) operand);
		}
		warn("Cannot resolve operand to immediate value: " + operand.eClass().getName());
		return 0;
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
			return Integer.parseInt(s.substring(1), 16);
		}
		if (s.toLowerCase().startsWith("0x")) {
			return Integer.parseInt(s.substring(2), 16);
		}
		// Hex: XXXXh or XXXXH
		if (s.endsWith("h") || s.endsWith("H")) {
			return Integer.parseInt(s.substring(0, s.length() - 1), 16);
		}
		// Binary: %XXXX or 0bXXXX
		if (s.startsWith("%")) {
			return Integer.parseInt(s.substring(1), 2);
		}
		if (s.toLowerCase().startsWith("0b")) {
			return Integer.parseInt(s.substring(2), 2);
		}
		// Binary: XXXXb or XXXXB
		if (s.endsWith("b") || s.endsWith("B")) {
			return Integer.parseInt(s.substring(0, s.length() - 1), 2);
		}
		// Fallback decimal
		return Integer.parseInt(s);
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
		warnings.add(message);
	}
}
