package uk.co.bithatch.zxbasic.tools;

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
                out.write(0x0E); // marker for 16-bit int
                out.write(intVal & 0xFF);
                out.write((intVal >> 8) & 0xFF);
            } else if (value instanceof Double doubleVal) {
                out.write(0x0F); // marker for 5-byte float
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
