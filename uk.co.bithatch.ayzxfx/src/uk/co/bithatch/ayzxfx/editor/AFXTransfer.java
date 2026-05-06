package uk.co.bithatch.ayzxfx.editor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import uk.co.bithatch.ayzxfx.ay.AFX;
import uk.co.bithatch.ayzxfx.ay.NamedAFX;

/**
 * SWT Transfer type for AFX effect data, allowing copy/paste via the system clipboard.
 */
public class AFXTransfer extends ByteArrayTransfer {

	private static final String TYPE_NAME = "uk.co.bithatch.ayzxfx.AFXTransfer";
	private static final int TYPE_ID = registerType(TYPE_NAME);
	private static final AFXTransfer INSTANCE = new AFXTransfer();

	public static AFXTransfer getInstance() {
		return INSTANCE;
	}

	private AFXTransfer() {
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { TYPE_ID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	@Override
	protected void javaToNative(Object object, TransferData transferData) {
		if (object instanceof AFX afx) {
			var buf = ByteBuffer.allocate(65536);
			buf.order(ByteOrder.LITTLE_ENDIAN);

			// Write name if present
			String name = null;
			if (afx instanceof NamedAFX nafx) {
				name = nafx.name();
			}
			if (name != null) {
				var nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
				buf.putInt(nameBytes.length);
				buf.put(nameBytes);
			} else {
				buf.putInt(-1);
			}

			// Write AFX frame data
			afx.write(buf);
			buf.flip();

			var bytes = new byte[buf.remaining()];
			buf.get(bytes);
			super.javaToNative(bytes, transferData);
		}
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
		var bytes = (byte[]) super.nativeToJava(transferData);
		if (bytes == null)
			return null;

		var buf = ByteBuffer.wrap(bytes);
		buf.order(ByteOrder.LITTLE_ENDIAN);

		// Read name
		var nameLen = buf.getInt();
		String name = null;
		if (nameLen >= 0) {
			var nameBytes = new byte[nameLen];
			buf.get(nameBytes);
			name = new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8);
		}

		// Read AFX data from remaining bytes using AFX.load
		var remaining = new byte[buf.remaining()];
		buf.get(remaining);
		try {
			var in = Channels.newChannel(new java.io.ByteArrayInputStream(remaining));
			var afx = AFX.load(in);
			if (name != null) {
				var named = afx.named();
				named.name(name);
				return named;
			}
			return afx;
		} catch (Exception e) {
			return null;
		}
	}
}
