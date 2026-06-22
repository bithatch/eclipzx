package uk.co.bithatch.emuzx.debug;

import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import uk.co.bithatch.bitzx.ISourceAdressMap;

/**
 * Stack frame for GDB RSP-based Z80 debugging.
 * Reads registers via individual {@code p<n>} packets from MAME's GDB stub,
 * and resolves source location using {@link Z88dkDebugInfoParser}.
 */
public final class GdbZ80StackFrame extends DelegatingDebugElement implements ILocatableStackFrame {

	private static final ILog LOG = ILog.of(GdbZ80StackFrame.class);

	private final Z80RegisterGroup z80registers;
	private final ISourceAdressMap debugInfo;
	private int pc = -1;
	private String sourceName;
	private int sourceLine = -1;

	/**
	 * MAME Z80 GDB stub register numbering.
	 * Based on MAME output (SP=10, PC=11).
	 * Order: AF, BC, DE, HL, AF', BC', DE', HL', IX, IY, SP, PC
	 */
	private static final int REG_AF = 0;
	private static final int REG_BC = 1;
	private static final int REG_DE = 2;
	private static final int REG_HL = 3;
	// 4=AF', 5=BC', 6=DE', 7=HL'
	private static final int REG_IX = 8;
	private static final int REG_IY = 9;
	private static final int REG_SP = 10;
	private static final int REG_PC = 11;

	GdbZ80StackFrame(GdbZ80Thread thread, GdbRspClient rsp, ISourceAdressMap debugInfo) {
		super(thread);
		this.debugInfo = debugInfo;
		z80registers = new Z80RegisterGroup(this);
		update(rsp);
	}

	public void update() {
		update(((GdbDebugTarget) getThread().getDebugTarget()).getRspClient());
	}

	private void update(GdbRspClient rsp) {
		/* Read registers from the GDB stub */
		readRegisters(rsp);

		/* Resolve source location from PC */
		if (debugInfo != null && pc >= 0) {
			var loc = debugInfo.getSourceLocation(pc);
			if (loc != null) {
				sourceName = loc.fileName();
				sourceLine = loc.line() + 1;
			} else {
				sourceName = null;
				sourceLine = -1;
			}
		}
	}

	private void readRegisters(GdbRspClient rsp) {
		/* Try reading all registers with 'g' first */
		try {
			var allRegs = rsp.readRegisters();
			if (allRegs != null && !allRegs.startsWith("E")) {
				parseAllRegisters(allRegs);
				return;
			}
		} catch (IOException e) {
			LOG.warn("Failed to read all registers via 'g', falling back to individual reads", e);
		}

		/* Fall back to individual 'p' register reads */
		readIndividualRegisters(rsp);
	}

	/**
	 * Parse the 'g' response. MAME returns registers as little-endian 16-bit values
	 * concatenated in order: AF, BC, DE, HL, AF', BC', DE', HL', IX, IY, SP, PC
	 */
	private void parseAllRegisters(String hex) {
		try {
			/* Each 16-bit register = 4 hex chars (little-endian) */
			int af = readLE16(hex, 0);
			int bc = readLE16(hex, 4);
			int de = readLE16(hex, 8);
			int hl = readLE16(hex, 12);
			
			/* Shadow registers */
			int af_ = readLE16(hex, 16);
			int bc_ = readLE16(hex, 20);
			int de_ = readLE16(hex, 24);
			int hl_ = readLE16(hex, 28);
			
			int ix = readLE16(hex, 32);
			int iy = readLE16(hex, 36);
			int sp = readLE16(hex, 40);
			pc = readLE16(hex, 44);

			addReg("PC", "Program Counter", pc);
			addReg("SP", "Stack Pointer", sp);
			addReg("AF", "Pair", af);
			addReg("BC", "Pair", bc);
			addReg("DE", "Pair", de);
			addReg("HL", "Pair", hl);
			addReg("IX", "Index", ix);
			addReg("IY", "Index", iy);
			
			addReg("AF'", "Shadow", af_);
			addReg("BC'", "Shadow", bc_);
			addReg("DE'", "Shadow", de_);
			addReg("HL'", "Shadow", hl_);
			
		} catch (Exception e) {
			LOG.error("Failed to parse register data: " + hex, e);
		}
	}

	private void readIndividualRegisters(GdbRspClient rsp) {
		String[][] regDefs = {
			{"PC", "Program Counter", String.valueOf(REG_PC)},
			{"SP", "Stack Pointer", String.valueOf(REG_SP)},
			{"AF", "Pair", String.valueOf(REG_AF)},
			{"BC", "Pair", String.valueOf(REG_BC)},
			{"DE", "Pair", String.valueOf(REG_DE)},
			{"HL", "Pair", String.valueOf(REG_HL)},
			{"IX", "Index", String.valueOf(REG_IX)},
			{"IY", "Index", String.valueOf(REG_IY)},
		};

		for (var def : regDefs) {
			try {
				var val = rsp.readRegister(Integer.parseInt(def[2]));
				if (val != null && !val.startsWith("E")) {
					/* Note: MAME might return PC padded to 32 bits (8 chars) e.g. "00000000".
					 * If value length is > 4, readLE16 will only read the first 4 chars.
					 * For little endian "34120000", that correctly reads "1234".
					 * If it's big endian padded like "00001234", it needs to read from the end.
					 * But RSP standard for numerical registers is natively little-endian or zero-padded LE.
					 * We use our robust readLE function assuming LE packing.
					 */
					int value = readLE(val, 0, val.length() / 2);
					addReg(def[0], def[1], value);
					if ("PC".equals(def[0])) {
						pc = value;
					}
				} else {
					LOG.warn("Failed to read register " + def[0] + ": " + val);
					addReg(def[0], def[1], -1);
				}
			} catch (IOException e) {
				LOG.warn("IOException reading register " + def[0], e);
				addReg(def[0], def[1], -1);
			}
		}
	}

	private void addReg(String name, String type, int value) {
		var reg = z80registers.addRegister(name, type);
		if (value >= 0) {
			reg.setValue(value);
		}
	}

	/** Read a little-endian 16-bit value from hex string at char offset */
	private static int readLE16(String hex, int charOffset) {
		if (charOffset + 4 > hex.length()) return -1;
		int lo = Integer.parseInt(hex.substring(charOffset, charOffset + 2), 16);
		int hi = Integer.parseInt(hex.substring(charOffset + 2, charOffset + 4), 16);
		return (hi << 8) | lo;
	}

	/** Read a little-endian value of N bytes */
	private static int readLE(String hex, int charOffset, int bytes) {
		if (charOffset + (bytes * 2) > hex.length()) return -1;
		int value = 0;
		for (int i = 0; i < bytes; i++) {
			int b = Integer.parseInt(hex.substring(charOffset + (i * 2), charOffset + (i * 2) + 2), 16);
			value |= (b << (i * 8));
		}
		return value;
	}

	/** Read an 8-bit value from hex string at char offset */
	private static int readLE8(String hex, int charOffset) {
		if (charOffset + 2 > hex.length()) return -1;
		return Integer.parseInt(hex.substring(charOffset, charOffset + 2), 16);
	}

	// ---- IStackFrame ----

	@Override
	protected IThread delegate() {
		return (IThread) super.delegate();
	}

	@Override
	public IThread getThread() {
		return delegate();
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return new IVariable[0];
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return false;
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return true;
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return new IRegisterGroup[] { z80registers };
	}

	@Override
	public String getName() throws DebugException {
		if (sourceName != null && sourceLine > 0) {
			return String.format("%s:%d [0x%04X]", sourceName, sourceLine, pc);
		}
		if (pc >= 0) {
			/* Try to find a symbol name */
			if (debugInfo != null) {
				var loc = debugInfo.getSourceLocation(pc);
				if (loc != null) {
					return String.format("%s:%d [0x%04X]", loc.fileName(), loc.line() + 1, pc);
				}
			}
			return String.format("0x%04X", pc);
		}
		return "Z80 (GDB)";
	}

	@Override
	public int getLineNumber() throws DebugException {
		return sourceLine > 0 ? sourceLine : -1;
	}

	@Override
	public int getCharStart() throws DebugException {
		return -1;
	}

	@Override
	public int getCharEnd() throws DebugException {
		return -1;
	}

	/**
	 * @return the PC value, or -1 if unknown
	 */
	public int getPC() {
		return pc;
	}

	/**
	 * @return the source file name, or null if unknown
	 */
	@Override
	public String getSourceName() {
		return sourceName;
	}

	/* No equals/hashCode override — we reuse a single frame instance per thread,
	 * so default Object identity is correct.  This lets Eclipse's Debug View
	 * keep its tree selection and expansion state across suspend/resume cycles. */

	// ---- Delegation to thread ----

	@Override
	public void terminate() throws DebugException { delegate().terminate(); }

	@Override
	public boolean isTerminated() { return delegate().isTerminated(); }

	@Override
	public boolean canTerminate() { return delegate().canTerminate(); }

	@Override
	public void suspend() throws DebugException { delegate().suspend(); }

	@Override
	public void resume() throws DebugException { delegate().resume(); }

	@Override
	public boolean isSuspended() { return delegate().isSuspended(); }

	@Override
	public boolean canSuspend() { return delegate().canSuspend(); }

	@Override
	public boolean canResume() { return delegate().canResume(); }

	@Override
	public void stepReturn() throws DebugException { delegate().stepReturn(); }

	@Override
	public void stepOver() throws DebugException { delegate().stepOver(); }

	@Override
	public void stepInto() throws DebugException { delegate().stepInto(); }

	@Override
	public boolean isStepping() { return delegate().isStepping(); }

	@Override
	public boolean canStepReturn() { return delegate().canStepReturn(); }

	@Override
	public boolean canStepOver() { return delegate().canStepOver(); }

	@Override
	public boolean canStepInto() { return delegate().canStepInto(); }
}
