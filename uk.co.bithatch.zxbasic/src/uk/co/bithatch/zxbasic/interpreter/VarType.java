package uk.co.bithatch.zxbasic.interpreter;

import java.lang.reflect.Array;


/**
 * Maps a ZX Basic Type to the best native Java type. All variables are stored with their type in a {@link Var}. 
 */
public enum VarType {
	/* Integrals */
	BYTE(1, true, -128, 127, Byte.class), 
	UBYTE(1, false, 0, 255, Short.class), 
	INTEGER(2, true, -32768.0, 32767, Short.class), 
	UINTEGER(2, false, 0, 65535, Integer.class), 
	LONG(4, true, -2147483648,2147483647, Integer.class), 
	ULONG(4, false, 0, 4294967295l, Long.class),
	/* Decimals */
	FIXED(4, false, -32767.9999847, 32767.9999847, Float.class),
	FLOAT(5, false, Float.MIN_VALUE, Float.MAX_VALUE, Double.class),
	/* Strings */
	STRING(0, false, null, null, String.class),
	/* Strings */
	ARRAY(0, false, null, null, Object[].class);
	
	private int size;
	private boolean signed;
	Class<?> nativeType;
	private Number minVal;
	private Number maxVal;
	
	private VarType(int size, boolean signed, Number minVal, Number maxVal, Class<?> nativeType) {
		this.signed = signed;
		this.size = size;
		this.nativeType = nativeType;
		this.minVal = minVal;
		this.maxVal = minVal;
	}
	
	public boolean integral() {
		return this == BYTE || this == UBYTE || this == INTEGER || this == UINTEGER || this == LONG || this == ULONG;
	}
	
	public boolean floatingPoint() {
		return this == FIXED || this == FLOAT;
	}
	
	public int size() {
		return size;
	}
	
	public boolean signed() {
		return signed;
	}
	
	public Number minVal() {
		return minVal;
	}

	public Number maxVal() {
		return maxVal;
	}

	public Var defaultValue() {
		switch(this) {
		case STRING:
			return new Var(STRING, "");
		case BYTE:
			return new Var(this, (byte)0);
		case UBYTE:
			return new Var(this, (byte)0);
		case FIXED:
			return new Var(this, (float)0);
		case FLOAT:
			return new Var(this, (double)0);
		case INTEGER:
			return new Var(this, (short)0);
		case UINTEGER:
			return new Var(this, (int)0);
		case LONG:
			return new Var(this, (int)0);
		case ULONG:
			return new Var(this, (long)0);
		default:
			throw new IllegalStateException("No default value for arrays");
		}
	}
	
	@SuppressWarnings("unchecked") <T> T[] newArray(int size) {
		return (T[]) Array.newInstance(nativeType, size);
	}
	
	static VarType floatType(Number val) {
		var fval = val.doubleValue();
		if(fval < -32767.9999847 || fval > 32767.9999847) {
			return VarType.FIXED;
		}
		else {
			return VarType.FLOAT;
		}
	}

	static VarType fromType(Object val) {
		if(val instanceof String)
			return VarType.STRING;
		else if(val instanceof Float) {
			return VarType.FIXED;
		}
		else if(val instanceof Double) {
			return VarType.FLOAT;
		}
		else if(val instanceof Byte) {
			return VarType.UBYTE;
		}
		else if(val instanceof Short) {
			return VarType.INTEGER;
		}
		else if(val instanceof Integer || val instanceof Long) {
			return VarType.LONG;
		}
		else 
			return VarType.FLOAT;
	}

	static VarType best(VarType... types) {
		var max = -1;
		for(var t : types) {
			if(t.ordinal() > max) {
				max = t.ordinal();
			}
		}
		if(max == -1)
			throw new IllegalArgumentException("No types.");
		else
			return VarType.values()[max];
	}

	boolean number() {
		return this != STRING && this != ARRAY;
	}
}