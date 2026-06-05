package uk.co.bithatch.emuzx.emulator.zoxo;

import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

import uk.co.bithatch.bitzx.ISourceAdressMap;
import uk.co.bithatch.eclipzoxo.views.EmulatorInstance;
import uk.co.bithatch.emuzx.debug.DelegatingDebugElement;
import uk.co.bithatch.emuzx.debug.ILocatableStackFrame;
import uk.co.bithatch.emuzx.debug.Z80RegisterGroup;

/**
 * Stack frame for GDB RSP-based Z80 debugging.
 * Reads registers via individual {@code p<n>} packets from MAME's GDB stub,
 * and resolves source location using {@link Z88dkDebugInfoParser}.
 */
public final class EmulatorZ80StackFrame extends DelegatingDebugElement implements ILocatableStackFrame {

	private static final ILog LOG = ILog.of(EmulatorZ80StackFrame.class);

	private final Z80RegisterGroup z80registers;
	private final ISourceAdressMap debugInfo;
	private int pc = -1;
	private String sourceName;
	private int sourceLine = -1;

	EmulatorZ80StackFrame(EmulatorZ80Thread thread, EmulatorInstance rsp, ISourceAdressMap debugInfo) {
		super(thread);
		this.debugInfo = debugInfo;
		z80registers = new Z80RegisterGroup(this);
		update(rsp);
	}

	public void update() {
		update(((EmulatorDebugTarget) getThread().getDebugTarget()).getEmulator());
	}

	private void update(EmulatorInstance rsp) {
		/* Read registers from the GDB stub */
		readRegisters(rsp);

		/* Resolve source location from PC */
		if (debugInfo != null && pc >= 0) {
			var loc = debugInfo.getSourceLocation(pc);
			if (loc != null) {
				sourceName = loc.fileName();
				sourceLine = loc.line();
			} else {
				sourceName = null;
				sourceLine = -1;
			}
		}
	}

	private void readRegisters(EmulatorInstance emulator) {
		try {
			var cpu = emulator.machine().cpu();

			pc = cpu.getRegPC();
			int sp = cpu.getRegSP();
			int af = cpu.getRegAF();
			int bc = cpu.getRegBC();
			int de = cpu.getRegDE();
			int hl = cpu.getRegHL();
			int ix = cpu.getRegIX();
			int iy = cpu.getRegIY();

			/* Shadow registers */
			int af_ = cpu.getRegAFx();
			int bc_ = cpu.getRegBCx();
			int de_ = cpu.getRegDEx();
			int hl_ = cpu.getRegHLx();

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

			LOG.info("Read registers: PC=0x" + Integer.toHexString(pc)
					+ " SP=0x" + Integer.toHexString(sp));
		} catch (Exception e) {
			LOG.error("Failed to read registers from emulator CPU", e);
		}
	}

	private void addReg(String name, String type, int value) {
		var reg = z80registers.addRegister(name, type);
		if (value >= 0) {
			reg.setValue(value);
		}
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
