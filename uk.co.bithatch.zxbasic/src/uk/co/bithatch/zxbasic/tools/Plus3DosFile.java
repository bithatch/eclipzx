package uk.co.bithatch.zxbasic.tools;

import java.io.ByteArrayOutputStream;

public class Plus3DosFile {
    private final String name;
    private final int type;          // 0 for BASIC, 1 for CODE
    private final int startAddress;
    private final int length;
    private final int param1;        // varies: line number (BASIC) or offset (CODE)
    private final int param2;        // varies: unused or exec address
    private final byte[] data;

    public Plus3DosFile(String name, int type, int startAddress, int length, int param1, int param2, byte[] data) {
        this.name = name;
        this.type = type;
        this.startAddress = startAddress;
        this.length = length;
        this.param1 = param1;
        this.param2 = param2;
        this.data = data;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] header = new byte[128];

        // Bytes 0-7: Signature "PLUS3DOS"
        byte[] sig = "PLUS3DOS".getBytes();
        System.arraycopy(sig, 0, header, 0, 8);

        // Byte 8: Soft EOF (0x1A)
        header[8] = 0x1A;

        // Byte 9: Issue number
        header[9] = 1;

        // Byte 10: Version number
        header[10] = 0;

        // Bytes 11-14: File length (header 128 + data length), 32-bit LE
        int fileLength = 128 + data.length;
        header[11] = (byte) (fileLength & 0xFF);
        header[12] = (byte) ((fileLength >> 8) & 0xFF);
        header[13] = (byte) ((fileLength >> 16) & 0xFF);
        header[14] = (byte) ((fileLength >> 24) & 0xFF);

        // Bytes 15-22: +3 BASIC header
        // Byte 15: File type (0=BASIC, 3=CODE)
        header[15] = (byte) type;

        // Bytes 16-17: Data length (LE)
        writeLE(header, 16, data.length);

        // Bytes 18-19: Param 1 (autostart line for BASIC, load addr for CODE) (LE)
        writeLE(header, 18, param1);

        // Bytes 20-21: Param 2 (start of variable area offset for BASIC, 0 for CODE) (LE)
        writeLE(header, 20, param2);

        // Bytes 22-127: Reserved (already zero)

        // Byte 127: Checksum (sum of bytes 0-126, mod 256)
        int checksum = 0;
        for (int i = 0; i < 127; i++) {
            checksum = (checksum + (header[i] & 0xFF)) & 0xFF;
        }
        header[127] = (byte) checksum;

        out.writeBytes(header);
        out.writeBytes(data);

        return out.toByteArray();
    }

    private static void writeLE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    public static Plus3DosFile fromBasic(String name, byte[] basicData) {
        return fromBasic(name, basicData, 0x8000);
    }

    public static Plus3DosFile fromBasic(String name, byte[] basicData, int autoStartLine) {
        return new Plus3DosFile(name, 0, 0, basicData.length, autoStartLine, basicData.length, basicData);
    }

    public static Plus3DosFile fromBinary(String name, byte[] binData, int loadAddr, int execAddr) {
        return new Plus3DosFile(name, 1, loadAddr, binData.length, 0, execAddr, binData);
    }
}
