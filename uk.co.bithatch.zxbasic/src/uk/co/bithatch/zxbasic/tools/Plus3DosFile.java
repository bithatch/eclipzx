package uk.co.bithatch.zxbasic.tools;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

        // File name (1–10 chars, space padded)
        byte[] nameBytes = name.getBytes();
        int nameLen = Math.min(nameBytes.length, 10);
        System.arraycopy(nameBytes, 0, header, 0, nameLen);
        for (int i = nameLen; i < 10; i++) {
            header[i] = 0x20;
        }

        header[10] = (byte) type;
        writeLE(header, 11, startAddress);
        writeLE(header, 13, length);
        writeLE(header, 15, param1);
        writeLE(header, 17, param2);

        // The 128-byte header is followed by the actual data block and then a checksum
        out.writeBytes(header);
        out.writeBytes(data);

        // Calculate checksum (XOR of all bytes including header + data)
        int checksum = 0;
        for (byte b : header) checksum ^= b;
        for (byte b : data) checksum ^= b;

        out.write(checksum);
        return out.toByteArray();
    }

    private static void writeLE(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    public static Plus3DosFile fromBasic(String name, byte[] basicData, int loadAddr, int autoStartLine) {
        return new Plus3DosFile(name, 0, loadAddr, basicData.length, autoStartLine, 0, basicData);
    }

    public static Plus3DosFile fromBinary(String name, byte[] binData, int loadAddr, int execAddr) {
        return new Plus3DosFile(name, 1, loadAddr, binData.length, 0, execAddr, binData);
    }
}
