package uk.co.bithatch.fatexplorer.vfs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Arrays;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class FileStoreTransfer extends ByteArrayTransfer {
    public static final String TYPE_NAME = "file-store-transfer-format";
    public static final int TYPEID = registerType(TYPE_NAME);
    private static final FileStoreTransfer INSTANCE = new FileStoreTransfer();

    public static FileStoreTransfer getInstance() {
        return INSTANCE;
    }

    @Override
    protected int[] getTypeIds() {
        return new int[] { TYPEID };
    }

    @Override
    protected String[] getTypeNames() {
        return new String[] { TYPE_NAME };
    }

    @Override
    protected void javaToNative(Object data, TransferData transferData) {
        // serialize your VirtualFile to byte[]
        if (data instanceof IFileStore[] stores) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(out)) {
                oos.writeObject(Arrays.asList(stores).stream().map(IFileStore::toURI).toList().toArray(new URI[0]));
                super.javaToNative(out.toByteArray(), transferData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected Object nativeToJava(TransferData transferData) {
        byte[] bytes = (byte[]) super.nativeToJava(transferData);
        if (bytes == null) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            URI[] uris = (URI[])ois.readObject();
            return Arrays.asList(uris).stream().map(u -> {
				try {
					return EFS.getStore(u);
				} catch (CoreException e) {
					throw new IllegalStateException(e);
				}
			}).toList().toArray(new IFileStore[0]);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
