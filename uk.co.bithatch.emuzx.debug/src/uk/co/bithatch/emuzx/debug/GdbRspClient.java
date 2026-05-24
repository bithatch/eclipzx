package uk.co.bithatch.emuzx.debug;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.ILog;

/**
 * Minimal GDB Remote Serial Protocol client.
 * <p>
 * Connects directly to a GDB stub (e.g., MAME's {@code -debugger gdbstub})
 * and communicates using the GDB RSP packet format:
 * {@code $<data>#<checksum>}
 * <p>
 * This replaces the fragile stdin/stdout wrapper around z88dk-gdb with a
 * clean, direct socket connection.
 */
public class GdbRspClient implements AutoCloseable {

	private static final ILog LOG = ILog.of(GdbRspClient.class);

	private final Socket socket;
	private final InputStream in;
	private final OutputStream out;

	private String targetXml;

	public GdbRspClient(String host, int port) throws IOException {
		LOG.info("GDB RSP: connecting to " + host + ":" + port);
		socket = new Socket(host, port);
		socket.setTcpNoDelay(true);
		in = socket.getInputStream();
		out = socket.getOutputStream();
		LOG.info("GDB RSP: connected");

		/* Perform GDB handshake — required by MAME before register access */
		performHandshake();
	}

	/**
	 * Perform the GDB RSP handshake:
	 * 1. Send qSupported to discover features
	 * 2. Read target.xml via qXfer to unlock register access
	 */
	private void performHandshake() throws IOException {
		/* Query supported features */
		var supported = sendCommand("qSupported:multiprocess+;xmlRegisters=i386");
		LOG.info("qSupported response: " + supported);

		/* Read target XML — MAME requires this before g/p commands work */
		if (supported != null && supported.contains("qXfer:features:read+")) {
			var sb = new StringBuilder();
			int offset = 0;
			while (true) {
				var chunk = sendCommand(
						String.format("qXfer:features:read:target.xml:%x,%x", offset, 0x3fff));
				if (chunk == null || chunk.isEmpty()) break;
				char indicator = chunk.charAt(0); // 'l' = last, 'm' = more
				sb.append(chunk.substring(1));
				if (indicator == 'l') break;
				offset += chunk.length() - 1;
			}
			targetXml = sb.toString();
			LOG.info("Target XML (" + targetXml.length() + " chars): "
					+ (targetXml.length() > 500 ? targetXml.substring(0, 500) + "..." : targetXml));
		}
	}

	/**
	 * @return the target description XML received from the stub, or null
	 */
	public String getTargetXml() {
		return targetXml;
	}

	/**
	 * Send a GDB RSP command and return the response data (without framing).
	 * 
	 * @param command the command string (e.g., "g" for read registers, "c" for continue)
	 * @return the response data string, or null if no response / error
	 */
	public String sendCommand(String command) throws IOException {
		sendPacket(command);
		return receivePacket();
	}

	/**
	 * Send a raw GDB RSP packet: {@code $<data>#<checksum>}
	 */
	private void sendPacket(String data) throws IOException {
		int checksum = 0;
		for (byte b : data.getBytes(StandardCharsets.US_ASCII)) {
			checksum = (checksum + (b & 0xFF)) & 0xFF;
		}
		var packet = String.format("$%s#%02x", data, checksum);
		LOG.info("GDB RSP >>> " + packet);
		out.write(packet.getBytes(StandardCharsets.US_ASCII));
		out.flush();

		/* Wait for ACK (+) */
		int ack = in.read();
		if (ack == '+') {
			LOG.info("GDB RSP: ACK received");
		} else if (ack == '-') {
			LOG.warn("GDB RSP: NACK received, retransmitting");
			sendPacket(data);
		} else {
			LOG.warn("GDB RSP: unexpected ACK byte: " + ack);
		}
	}

	/**
	 * Receive a GDB RSP response packet: {@code $<data>#<checksum>}
	 * 
	 * @return the data portion of the response, or null on error
	 */
	private String receivePacket() throws IOException {
		/* Skip until we find '$' */
		int b;
		while ((b = in.read()) != -1) {
			if (b == '$') break;
		}
		if (b == -1) return null;

		/* Read until '#' */
		var sb = new StringBuilder();
		while ((b = in.read()) != -1) {
			if (b == '#') break;
			sb.append((char) b);
		}
		if (b == -1) return null;

		/* Read 2-char checksum (we don't validate it) */
		int c1 = in.read();
		int c2 = in.read();

		/* Send ACK */
		out.write('+');
		out.flush();

		var data = sb.toString();
		LOG.info("GDB RSP <<< " + (data.length() > 200 ? data.substring(0, 200) + "..." : data));
		return data;
	}

	/**
	 * Query why the target stopped. Sends '?' command.
	 * 
	 * @return stop reply packet (e.g., "T05" for SIGTRAP)
	 */
	public String queryHaltReason() throws IOException {
		return sendCommand("?");
	}

	/**
	 * Read all general registers.
	 * 
	 * @return hex-encoded register data
	 */
	public String readRegisters() throws IOException {
		return sendCommand("g");
	}

	/**
	 * Read a single register by index using the 'p' packet.
	 * 
	 * @param regNum the register number (target-specific)
	 * @return hex-encoded register value, or error string
	 */
	public String readRegister(int regNum) throws IOException {
		return sendCommand(String.format("p%x", regNum));
	}

	/**
	 * Continue execution.
	 */
	public void continueExecution() throws IOException {
		sendPacket("c");
		/* 'c' doesn't get an immediate response — the target runs until it stops */
	}

	/**
	 * Single step.
	 */
	public void step() throws IOException {
		sendPacket("s");
	}

	/**
	 * Set a breakpoint at the given address.
	 * 
	 * @param address the memory address (e.g., 0x8000)
	 * @return true if the breakpoint was set successfully
	 */
	public boolean setBreakpoint(int address) throws IOException {
		var response = sendCommand(String.format("Z0,%04x,1", address));
		return "OK".equals(response);
	}

	/**
	 * Remove a breakpoint at the given address.
	 * 
	 * @param address the memory address
	 * @return true if the breakpoint was removed successfully
	 */
	public boolean removeBreakpoint(int address) throws IOException {
		var response = sendCommand(String.format("z0,%04x,1", address));
		return "OK".equals(response);
	}

	/**
	 * Read memory from the target.
	 * 
	 * @param address start address
	 * @param length  number of bytes to read
	 * @return hex-encoded memory contents
	 */
	public String readMemory(int address, int length) throws IOException {
		return sendCommand(String.format("m%x,%x", address, length));
	}

	/**
	 * Detach from the target (let it continue running).
	 */
	public void detach() throws IOException {
		sendCommand("D");
	}

	/**
	 * Send an interrupt (Ctrl-C / 0x03) to pause a running target.
	 * This is sent as a raw byte, not as a GDB RSP packet.
	 */
	public void interrupt() throws IOException {
		LOG.info("GDB RSP: sending interrupt (0x03)");
		out.write(0x03);
		out.flush();
	}

	/**
	 * Kill the target.
	 */
	public void kill() throws IOException {
		try {
			sendPacket("k");
		} catch (IOException e) {
			/* Target may close connection immediately on kill */
		}
	}

	/**
	 * Wait for the target to stop (after continue/step).
	 * Blocks until a stop reply packet is received.
	 * 
	 * @return the stop reply (e.g., "T05" for breakpoint)
	 */
	public String waitForStop() throws IOException {
		return receivePacket();
	}

	/**
	 * Check if the connection is still open.
	 */
	public boolean isConnected() {
		return socket != null && !socket.isClosed() && socket.isConnected();
	}

	@Override
	public void close() throws IOException {
		LOG.info("GDB RSP: closing connection");
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
	}
}
