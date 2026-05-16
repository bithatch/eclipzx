package uk.co.bithatch.bitzx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ZxNumberEncoder {

    /**
     * Encodes a number as a ZX BASIC numeric constant.
     * @param value Either a float (Double) or an integer (Integer)
     * @return byte[] representing the encoded constant
     */
    public static byte[] encode(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            if (value instanceof Integer intVal && intVal >= 0 && intVal <= 0xFFFF) {
                // ASCII representation first
                out.write(String.valueOf(intVal).getBytes());
                // 0x0E marker followed by 5-byte integer encoding: 00 00 LOW HIGH 00
                out.write(0x0E);
                out.write(0x00);
                out.write(0x00);
                out.write(intVal & 0xFF);
                out.write((intVal >> 8) & 0xFF);
                out.write(0x00);
            } else if (value instanceof Double doubleVal) {
                // ASCII representation first
                out.write(String.valueOf(doubleVal).getBytes());
                // 0x0E marker followed by 5-byte float encoding
                out.write(0x0E);
                out.write(SinclairFloat.encode(doubleVal));
            } else if (value instanceof String str) {
                if (str.matches("\\d+")) {
                    int intVal = Integer.parseInt(str);
                    return encode(intVal);
                } else {
                    double dblVal = Double.parseDouble(str);
                    return encode(dblVal);
                }
            } else {
                throw new IllegalArgumentException("Unsupported number type: " + value);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unexpected I/O error in in-memory buffer", e);
        }

        return out.toByteArray();
    }
}
