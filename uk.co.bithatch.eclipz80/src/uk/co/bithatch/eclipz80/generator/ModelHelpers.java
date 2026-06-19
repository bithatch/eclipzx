package uk.co.bithatch.eclipz80.generator;

import uk.co.bithatch.eclipz80.asm.IntegralLiteral;

public class ModelHelpers {

	/**
	 * Parse an IntegralLiteral to an int, handling decimal, hex ($, 0x, h suffix),
	 * and binary (%, 0b, b suffix) formats.
	 */
	public static int resolveIntegralLiteral(IntegralLiteral lit) {
		String litStr = lit.getLitvalue();
		if (litStr != null && !litStr.isEmpty()) {
			return parseLitvalue(litStr);
		}
		// Decimal — the parser already converted to int
		return lit.getValue();
	}

	public static int parseLitvalue(String s) {
		s = s.trim();
		// Hex: $XXXX or 0xXXXX
		if (s.startsWith("$")) {
			return (int) Long.parseLong(s.substring(1), 16);
		}
		if (s.toLowerCase().startsWith("0x")) {
			return (int) Long.parseLong(s.substring(2), 16);
		}
		// Hex: XXXXh or XXXXH
		if (s.endsWith("h") || s.endsWith("H")) {
			return (int) Long.parseLong(s.substring(0, s.length() - 1), 16);
		}
		// Binary: %XXXX or 0bXXXX
		if (s.startsWith("%")) {
			return (int) Long.parseLong(s.substring(1), 2);
		}
		if (s.toLowerCase().startsWith("0b")) {
			return (int) Long.parseLong(s.substring(2), 2);
		}
		// Binary: XXXXb or XXXXB
		if (s.endsWith("b") || s.endsWith("B")) {
			return (int) Long.parseLong(s.substring(0, s.length() - 1), 2);
		}
		// Fallback decimal
		return (int) Long.parseLong(s);
	}
}
