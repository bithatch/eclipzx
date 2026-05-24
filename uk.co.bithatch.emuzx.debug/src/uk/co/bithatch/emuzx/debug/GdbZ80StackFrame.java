package uk.co.bithatch.emuzx.debug;

import java.io.IOException;

import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

/**
 * Stack frame for GDB RSP-based Z80 debugging.
 * Reads registers via individual {@code p<n>} packets from MAME's GDB stub,
 * and resolves source location using {@link Z88dkDebugInfoParser}.
 */
public final class GdbZ80StackFrame extends DelegatingDebugElement implements IStackFrame {

	private static final ILog LOG = ILog.of(GdbZ80StackFrame.class);

	private final Z80RegisterGroup z80registers;
	private final Z88dkDebugInfoParser debugInfo;
	private int pc = -1;
	private String sourceName;
	private int sourceLine = -1;

	/**
	 * MAME Z80 GDB stub register numbering.
	 * From MAME source (gdbstub.cpp), the Z80 register order for 'g' and 'p' is:
	 * 0=AF, 1=BC, 2=DE, 3=HL, 4=SP, 5=PC, 6=IX, 7=IY,
	 * 8=AF', 9=BC', 10=DE', 11=HL', 12=I, 13=R
	 */
	private static final int REG_AF = 0;
	private static final int REG_BC = 1;
	private static final int REG_DE = 2;
	private static final int REG_HL = 3;
	private static final int REG_SP = 4;
	private static final int REG_PC = 5;
	private static final int REG_IX = 6;
	private static final int REG_IY = 7;

	GdbZ80StackFrame(GdbZ80Thread thread, GdbRspClient rsp, Z88dkDebugInfoParser debugInfo) {
		super(thread);
		this.debugInfo = debugInfo;
		z80registers = new Z80RegisterGroup(this);

		/* Read registers from the GDB stub */
		readRegisters(rsp);

		/* Resolve source location from PC */
		if (debugInfo != null && pc >= 0) {
			var loc = debugInfo.getSourceLocation(pc);
			if (loc != null) {
				sourceName = loc.fileName();
				sourceLine = loc.line();
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
	 * concatenated in order: AF, BC, DE, HL, SP, PC, IX, IY, AF', BC', DE', HL', I(8), R(8)
	 */
	private void parseAllRegisters(String hex) {
		try {
			/* Each 16-bit register = 4 hex chars (little-endian) */
			int af = readLE16(hex, 0);
			int bc = readLE16(hex, 4);
			int de = readLE16(hex, 8);
			int hl = readLE16(hex, 12);
			int sp = readLE16(hex, 16);
			pc = readLE16(hex, 20);
			int ix = readLE16(hex, 24);
			int iy = readLE16(hex, 28);

			addReg("PC", "Program Counter", pc);
			addReg("SP", "Stack Pointer", sp);
			addReg("AF", "Pair", af);
			addReg("BC", "Pair", bc);
			addReg("DE", "Pair", de);
			addReg("HL", "Pair", hl);
			addReg("IX", "Index", ix);
			addReg("IY", "Index", iy);

			/* Shadow registers if available */
			if (hex.length() >= 48) {
				addReg("AF'", "Shadow", readLE16(hex, 32));
				addReg("BC'", "Shadow", readLE16(hex, 36));
				addReg("DE'", "Shadow", readLE16(hex, 40));
				addReg("HL'", "Shadow", readLE16(hex, 44));
			}
			if (hex.length() >= 52) {
				addReg("I", "Interrupt", readLE8(hex, 48));
				addReg("R", "Refresh", readLE8(hex, 50));
			}
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
					int value = readLE16(val, 0);
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
					return String.format("%s:%d [0x%04X]", loc.fileName(), loc.line(), pc);
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
	public String getSourceName() {
		return sourceName;
	}

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
