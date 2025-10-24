package uk.co.bithatch.zxbasic.tools;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SinclairFloat {

    public static byte[] encode(double value) {
        if (value == 0.0) {
            return new byte[5]; // all zeros
        }

        byte[] out = new byte[5];
        long bits = Double.doubleToRawLongBits(value);
        boolean negative = (bits >>> 63) != 0;

        int exponent = 0;
        double abs = Math.abs(value);
        double norm = abs;

        while (norm >= 1.0) {
            norm /= 2.0;
            exponent++;
        }
        while (norm < 0.5) {
            norm *= 2.0;
            exponent--;
        }

        // ZX uses base-2 exponent with +128 bias
        int expByte = exponent + 129;
        out[0] = (byte) expByte;

        // Multiply by 2^32 to convert mantissa to 32-bit value
        long mantissa = (long) (norm * (1L << 32));
        if (negative) mantissa |= 0x80000000L;

        out[1] = (byte) ((mantissa >> 24) & 0xFF);
        out[2] = (byte) ((mantissa >> 16) & 0xFF);
        out[3] = (byte) ((mantissa >> 8) & 0xFF);
        out[4] = (byte) (mantissa & 0xFF);

        return out;
    }

    // Optional: for testing round-trips
    public static double decode(byte[] in) {
        if (in.length != 5 || (in[0] == 0 && Arrays.equals(Arrays.copyOfRange(in, 1, 5), new byte[4]))) {
            return 0.0;
        }

        int exp = (in[0] & 0xFF) - 129;

        long mantissa = ((long) (in[1] & 0xFF) << 24)
                      | ((long) (in[2] & 0xFF) << 16)
                      | ((long) (in[3] & 0xFF) << 8)
                      | ((long) (in[4] & 0xFF));

        boolean negative = (mantissa & 0x80000000L) != 0;
        mantissa &= 0x7FFFFFFFL;

        double norm = (double) mantissa / (1L << 32);
        double result = norm * Math.pow(2.0, exp);
        return negative ? -result : result;
    }
}
