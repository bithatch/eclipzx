package uk.co.bithatch.tnfs.eclipse;

import java.net.URI;
import java.util.Objects;

import uk.co.bithatch.tnfs.lib.Protocol;

/**
 * Represents a single TNFS client mount configuration.
 */
public class TNFSClientMount {

	private String name;
	private String host;
	private int port;
	private String remotePath;
	private String username;
	private boolean automount;
	private Protocol protocol;

	public TNFSClientMount() {
		this.port = uk.co.bithatch.tnfs.lib.TNFS.DEFAULT_PORT;
		this.remotePath = "/";
		this.host = "";
		this.name = "";
		this.username = "";
		this.automount = true;
		this.protocol = Protocol.TCP;
	}

	public TNFSClientMount(String name, String host, int port, String remotePath, String username) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.remotePath = remotePath;
		this.username = username;
		this.automount = true;
		this.protocol = Protocol.TCP;
	}

	public TNFSClientMount(String name, String host, int port, String remotePath, String username, boolean automount) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.remotePath = remotePath;
		this.username = username;
		this.automount = automount;
		this.protocol = Protocol.TCP;
	}

	public TNFSClientMount(String name, String host, int port, String remotePath, String username, boolean automount, Protocol protocol) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.remotePath = remotePath;
		this.username = username;
		this.automount = automount;
		this.protocol = protocol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isAutomount() {
		return automount;
	}

	public void setAutomount(boolean automount) {
		this.automount = automount;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	/**
	 * Build a tnfs:// URI for this mount.
	 * Format: tnfs://[user@]host[:port]/path#protocol
	 */
	public URI toURI() {
		try {
			var userInfo = (username != null && !username.isEmpty()) ? username : null;
			var p = (port != uk.co.bithatch.tnfs.lib.TNFS.DEFAULT_PORT) ? port : -1;
			var path = remotePath.startsWith("/") ? remotePath : "/" + remotePath;
			var fragment = protocol != null ? protocol.name().toLowerCase() : "tcp";
			return new URI("tnfs", userInfo, host, p, path, null, fragment);
		} catch (Exception e) {
			throw new IllegalStateException("Invalid mount configuration", e);
		}
	}

	/**
	 * Serialize to a storable string: name|host|port|remotePath|username|automount|protocol
	 */
	public String serialize() {
		return String.join("|", name, host, String.valueOf(port), remotePath, 
				username == null ? "" : username, String.valueOf(automount),
				protocol != null ? protocol.name() : "TCP");
	}

	/**
	 * Deserialize from stored string.
	 */
	public static TNFSClientMount deserialize(String s) {
		var parts = s.split("\\|", -1);
		if (parts.length < 4) return null;
		var m = new TNFSClientMount();
		m.name = parts[0];
		m.host = parts[1];
		m.port = Integer.parseInt(parts[2]);
		m.remotePath = parts[3];
		m.username = parts.length > 4 ? parts[4] : "";
		m.automount = parts.length > 5 ? Boolean.parseBoolean(parts[5]) : true;
		m.protocol = parts.length > 6 ? parseProtocol(parts[6]) : Protocol.TCP;
		return m;
	}

	private static Protocol parseProtocol(String s) {
		try {
			return Protocol.valueOf(s.toUpperCase());
		} catch (Exception e) {
			return Protocol.TCP;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof TNFSClientMount other)) return false;
		return Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return name + " [" + toURI() + "]";
	}
}