package uk.co.bithatch.emuzx.debug;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.MemoryByte;

/**
 * {@link IMemoryBlockExtension} implementation for Z80 memory via GDB RSP.
 * <p>
 * Provides live, refreshable access to the Z80's 64 KB address space
 * (0x0000–0xFFFF). Each call to {@link #getBytesFromOffset(BigInteger, long)}
 * or {@link #getBytesFromAddress(BigInteger, long)} reads directly from the
 * emulator via the {@link GdbRspClient}.
 */
public final class Z80MemoryBlock implements IMemoryBlockExtension {

	private static final ILog LOG = ILog.of(Z80MemoryBlock.class);

	/** Z80 address space is 64 KB */
	private static final BigInteger ADDRESS_SPACE_SIZE = BigInteger.valueOf(0x10000);

	private final GdbDebugTarget debugTarget;
	private final GdbRspClient rsp;
	private final String expression;
	private final BigInteger baseAddress;
	private final List<Object> connections = new ArrayList<>();

	Z80MemoryBlock(GdbDebugTarget debugTarget, GdbRspClient rsp,
			String expression, BigInteger baseAddress) {
		this.debugTarget = debugTarget;
		this.rsp = rsp;
		this.expression = expression;
		this.baseAddress = clampAddress(baseAddress);
	}

	// ---- IMemoryBlockExtension ----

	@Override
	public String getExpression() {
		return expression;
	}

	@Override
	public BigInteger getBigBaseAddress() throws DebugException {
		return baseAddress;
	}

	@Override
	public BigInteger getMemoryBlockStartAddress() throws DebugException {
		return BigInteger.ZERO;
	}

	@Override
	public BigInteger getMemoryBlockEndAddress() throws DebugException {
		return ADDRESS_SPACE_SIZE.subtract(BigInteger.ONE);
	}

	@Override
	public BigInteger getBigLength() throws DebugException {
		return ADDRESS_SPACE_SIZE;
	}

	@Override
	public int getAddressSize() throws DebugException {
		/* Z80 addresses are 16-bit → 2 bytes */
		return 2;
	}

	@Override
	public boolean supportBaseAddressModification() throws DebugException {
		return true;
	}

	@Override
	public boolean supportsChangeManagement() {
		return false;
	}

	@Override
	public void setBaseAddress(BigInteger address) throws DebugException {
		/* Nothing to do — the memory view will re-query with the new base */
	}

	@Override
	public MemoryByte[] getBytesFromOffset(BigInteger unitOffset, long addressableUnits) throws DebugException {
		return getBytesFromAddress(baseAddress.add(unitOffset), addressableUnits);
	}

	@Override
	public MemoryByte[] getBytesFromAddress(BigInteger address, long units) throws DebugException {
		int requestedLength = (int) units;
		int addr = clampAddress(address).intValue();
		/* Clamp to the Z80 address space */
		int readable = (int) Math.min(requestedLength, 0x10000 - addr);
		if (readable < 0) readable = 0;

		LOG.info("getBytesFromAddress: addr=0x" + Integer.toHexString(addr)
				+ " units=" + units + " readable=" + readable);

		var result = new MemoryByte[requestedLength];

		if (readable > 0) {
			try {
				var hexData = rsp.readMemory(addr, readable);
				LOG.info("readMemory returned " + (hexData != null ? hexData.length() / 2 : 0) + " bytes");
				var readBytes = hexToMemoryBytes(hexData, readable);
				System.arraycopy(readBytes, 0, result, 0, readBytes.length);
			} catch (IOException e) {
				LOG.error("Failed to read memory at 0x" + Integer.toHexString(addr), e);
				/* Fill readable range with error bytes */
				for (int i = 0; i < readable; i++) {
					result[i] = new MemoryByte((byte) 0, (byte) 0);
				}
			}
		}

		/* Fill any bytes beyond the readable range as unavailable */
		for (int i = readable; i < requestedLength; i++) {
			result[i] = new MemoryByte((byte) 0, (byte) 0);
		}

		return result;
	}

	@Override
	public void setValue(BigInteger offset, byte[] bytes) throws DebugException {
		/* Read-only for now */
		throw new DebugException(Status.error("Memory modification not supported"));
	}

	@Override
	public void connect(Object client) {
		if (!connections.contains(client)) {
			connections.add(client);
		}
	}

	@Override
	public void disconnect(Object client) {
		connections.remove(client);
	}

	@Override
	public Object[] getConnections() {
		return connections.toArray();
	}

	@Override
	public void dispose() throws DebugException {
		connections.clear();
	}

	@Override
	public IMemoryBlockRetrieval getMemoryBlockRetrieval() {
		return debugTarget;
	}

	@Override
	public int getAddressableSize() throws DebugException {
		/* Z80 is byte-addressable */
		return 1;
	}

	// ---- IMemoryBlock (base interface, still required) ----

	@Override
	public long getStartAddress() {
		return baseAddress.longValue();
	}

	@Override
	public long getLength() {
		return ADDRESS_SPACE_SIZE.longValue();
	}

	@Override
	public byte[] getBytes() throws DebugException {
		try {
			var hex = rsp.readMemory(baseAddress.intValue(), 256);
			return hexToRawBytes(hex);
		} catch (IOException e) {
			throw new DebugException(Status.error("Failed to read memory", e));
		}
	}

	@Override
	public boolean supportsValueModification() {
		return false;
	}

	@Override
	public void setValue(long offset, byte[] bytes) throws DebugException {
		/* Read-only */
	}

	// ---- IDebugElement ----

	@Override
	public String getModelIdentifier() {
		return debugTarget.getModelIdentifier();
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return debugTarget;
	}

	@Override
	public ILaunch getLaunch() {
		return debugTarget.getLaunch();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	// ---- Helpers ----

	/**
	 * Notify connected renderings that memory content may have changed.
	 * Call this after the target suspends so hex views auto-refresh.
	 */
	void fireContentChange() {
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
			new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.CONTENT)
		});
	}

	private static BigInteger clampAddress(BigInteger address) {
		if (address.signum() < 0) return BigInteger.ZERO;
		if (address.compareTo(ADDRESS_SPACE_SIZE) >= 0) {
			return address.mod(ADDRESS_SPACE_SIZE);
		}
		return address;
	}

	private static MemoryByte[] hexToMemoryBytes(String hex, int expectedLength) {
		if (hex == null || hex.isEmpty()) {
			/* Return "unavailable" bytes */
			var result = new MemoryByte[expectedLength];
			for (int i = 0; i < expectedLength; i++) {
				result[i] = new MemoryByte((byte) 0, (byte) 0);
			}
			return result;
		}
		int count = Math.min(hex.length() / 2, expectedLength);
		var result = new MemoryByte[expectedLength];
		/* READABLE (0x08) | HISTORY_KNOWN (0x01) — minimum flags for renderings to display a byte */
		byte flags = (byte) (MemoryByte.READABLE | MemoryByte.HISTORY_KNOWN | MemoryByte.ENDIANESS_KNOWN);
		for (int i = 0; i < count; i++) {
			byte b = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
			result[i] = new MemoryByte(b, flags);
		}
		/* Pad with unavailable bytes if we got fewer than expected */
		for (int i = count; i < expectedLength; i++) {
			result[i] = new MemoryByte((byte) 0, (byte) 0);
		}
		return result;
	}

	private static byte[] hexToRawBytes(String hex) {
		if (hex == null) return new byte[0];
		int len = hex.length() / 2;
		var bytes = new byte[len];
		for (int i = 0; i < len; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}
		return bytes;
	}
}
