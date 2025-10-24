package uk.co.bithatch.zxbasic.interpreter;

/**
 * Encapsulate a variable value and its type. The value will either be a {@link String}, some
 * {@link Number} or an {@link ArrayValue}
 */
public record Var(VarType type, Object value) {
	public static final Var FALSE = new Var(VarType.UBYTE, 0);
	public static final Var TRUE = new Var(VarType.UBYTE, 1);
	public static final Var ZERO = new Var(VarType.UBYTE, 0);
	public static final Var PI = new Var(VarType.FLOAT, Math.PI);

	final static Var forObject(Object value) {
		if(value instanceof Var var)
			return var;
		else
			return new Var(VarType.fromType(value), value);
	}

	public static Var forUByte(int value) {
		return new Var(VarType.UBYTE, value);
	}
	
	public static Var forByte(int value) {
		return new Var(VarType.BYTE, value);
	}
	
	public static Var forInteger(int value) {
		return new Var(VarType.INTEGER, value);
	}
	
	public static Var forLong(int value) {
		return new Var(VarType.LONG, value);
	}
	
	public static Var forULong(long value) {
		return new Var(VarType.ULONG, value);
	}
	
	public static Var forFixed(float value) {
		return new Var(VarType.FIXED, value);
	}
	
	public static Var forFloat(double value) {
		return new Var(VarType.FLOAT, value);
	}
	
	public static Var forUInteger(int value) {
		return new Var(VarType.INTEGER, value);
	}

	public static Var forString(Object value) {
		return new Var(VarType.STRING, value == null ? "" : (value instanceof String ? (String)value : String.valueOf(value)));
	}

	public static Var forFixedOrFloat(Number nbr) {
		var type = VarType.floatType(nbr);
		if(type == VarType.FLOAT)
			return new Var(type, nbr.doubleValue());
		else
			return new Var(type, nbr.floatValue());
	}

	public byte byteValue() {
		if(value instanceof Number nbr) {
			return nbr.byteValue();
		}
		return 0;
	}

	public float floatValue() {
		if(value instanceof Number nbr) {
			return nbr.floatValue();
		}
		return 0;
	}

	public double doubleValue() {
		if(value instanceof Number nbr) {
			return nbr.doubleValue();
		}
		return 0;
	}

	public int intValue() {
		if(value instanceof Number nbr) {
			return nbr.intValue();
		}
		return 0;
	}

	public long longValue() {
		if(value instanceof Number nbr) {
			return nbr.longValue();
		}
		return 0;
	}
	
	public boolean booleanValue() {
		if(value instanceof Number nval) {
    		return nval.doubleValue() != 0;
    	}
    	else if(value instanceof String sval) {
    		return sval.length() > 0;
    	}
    	else 
    		return false;
	}

	public String stringValue() {
		if(value instanceof String sval) {
    		return sval;
    	}
    	else 
    		return String.valueOf(value);
	}

	public static Var forIntegral(long value) {
		if(value < 0) {
			if(value < 32768) {
				return new Var(VarType.LONG, value);
			}
			else if(value < 128) {
				return new Var(VarType.INTEGER, value);
			}
			else {
				return new Var(VarType.BYTE, value);
			}
		}
		else {
			if(value  > 65535) {
				return new Var(VarType.ULONG, value);
			}
			else if(value  > 255) {
				return new Var(VarType.UINTEGER, value);
			}
			else {
				return new Var(VarType.UBYTE, value);
			}
		}
	}

	public static Var forBoolean(boolean b) {
		return b ? Var.TRUE : Var.FALSE;
	}

	public Var toType(VarType type) {
		if(type == type())
    		return this;
    	
    	switch(type) {
    	case ARRAY:
    		throw new UnsupportedOperationException("Cannot enforce array type");
    	case STRING:
    		return Var.forString(value);
    	default: {
    		/* Some kind of number */
    		var number = (Number)value;
    		switch(type) {
    		case UBYTE:
    			return forUByte(number.intValue());
    		case BYTE:
    			return forByte(number.intValue()); 
    		case INTEGER:
    			return forInteger(number.intValue());
    		case UINTEGER:
    			return forUInteger(number.intValue());
    		case LONG:
    			return forLong(number.intValue());
    		case ULONG:
    			return forULong(number.longValue());
    		case FLOAT:
    			return forFloat(number.doubleValue());
    		case FIXED:
    			return forFixed(number.floatValue());
    		default:
    			throw new UnsupportedOperationException("Cannot convert type to " + type.name());
    		}
    		}
    	}
	}

	public boolean zero() {
		if (type().integral())
			return longValue() == 0;
		else if (type().floatingPoint())
			return doubleValue() == 0;
		else
			return true;
	}

	public boolean gt(Var other) {
		if (type.integral() && other.type().number()) {
			return (((Number) value).longValue() > ((Number) other.value).longValue());
		} else if (type.floatingPoint()) {
			return (((Number) value).doubleValue() > ((Number) other.value).doubleValue());
		} else {
			throw new IllegalArgumentException();
		}
	}

	public boolean lt(Var other) {
		if (type.integral() && other.type().number()) {
			return (((Number) value).longValue() < ((Number) other.value).longValue());
		} else if (type.floatingPoint()) {
			return (((Number) value).doubleValue() < ((Number) other.value).doubleValue());
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public Var add(Var step) {
		if(type.integral()) {
			var newVal = (((Number)value).longValue() + ((Number)step.value).longValue());
			if(newVal < type.minVal().longValue()) {
				newVal = newVal % type.minVal().longValue(); 
			}
			else if(newVal > type.maxVal().longValue()) {
				newVal = newVal % type.maxVal().longValue(); 
			}
			return new Var(type, newVal); 
		}
		else if(type.floatingPoint()) {
			var newVal = (((Number)value).doubleValue() + ((Number)step.value).doubleValue());
			if(newVal < type.minVal().doubleValue()) {
				newVal = newVal % type.minVal().doubleValue(); 
			}
			else if(newVal > type.maxVal().longValue()) {
				newVal = newVal % type.maxVal().doubleValue(); 
			}
			return new Var(type, newVal); 
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public VarType arrayType() {
		if(type == VarType.ARRAY)
			return ((ArrayValue)value).contentType();
		else
			throw new IllegalStateException("Not an array.");
	}

	public Var element(int dimension, int... index) {
		var dimc = 0;
		return getElement(dimc, dimension, index);
	}
	private Var getElement(int thisDim, int dimension, int... index) {

		if(type == VarType.ARRAY) {
			var elVar = (Var)((ArrayValue)value).data()[index[thisDim]];
			if(thisDim == dimension) {
				return elVar;		
			}
			else {
				return elVar.getElement(thisDim + 1, dimension, index);
			}
		}
		else {
			throw new IllegalArgumentException("Dim " + dimension + " is not an array.");
		}
	}
}