package uk.co.bithatch.eclipz80;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import uk.co.bithatch.eclipzpp.IReferenceIndex;

public class AsmStdlib implements IReferenceIndex {
	
	/*
	 * limit $
	 */
	private final static Map<String, Function<double[], Double>> FUNCTIONS = new HashMap<>();  
	private final static Map<String, Double> CONSTANTS = new HashMap<>();
	
	static {
		CONSTANTS.put("PI", Math.PI );
		CONSTANTS.put("E", Math.E );
		
		FUNCTIONS.put("LIMIT", (parms) -> { throw new UnsupportedOperationException("LIMIT not implemented."); } );
		FUNCTIONS.put("$", (parms) -> { throw new UnsupportedOperationException("$ not implemented."); } );
		FUNCTIONS.put("SIN", (parms) -> Math.sin(parms[0]) );
		FUNCTIONS.put("COS", (parms) -> Math.cos(parms[0]) );
		FUNCTIONS.put("TAN", (parms) -> Math.tan(parms[0]) );
		FUNCTIONS.put("ASIN", (parms) -> Math.asin(parms[0]) );
		FUNCTIONS.put("ACOS", (parms) -> Math.acos(parms[0]) );
		FUNCTIONS.put("ATAB", (parms) -> Math.atan(parms[0]) );
		FUNCTIONS.put("COSH", (parms) -> Math.cosh(parms[0]) );
		FUNCTIONS.put("TANH", (parms) -> Math.tanh(parms[0]) );
		FUNCTIONS.put("ASINH", (parms) -> asinh(parms[0]) );
		FUNCTIONS.put("ACOSH", (parms) -> acosh(parms[0]) );
		FUNCTIONS.put("ATANH", (parms) -> atanh(parms[0]) );
		FUNCTIONS.put("LOG", (parms) -> Math.log(parms[0]) );
		FUNCTIONS.put("LOG10", (parms) -> Math.log10(parms[0]) );
		FUNCTIONS.put("LOG2", (parms) -> (double)log2((int)parms[0]) );
		FUNCTIONS.put("EXP", (parms) -> Math.exp(parms[0]) );
		FUNCTIONS.put("EXP2", (parms) -> Math.pow(2.0, parms[0]));
		FUNCTIONS.put("POW", (parms) -> Math.pow(parms[0], parms[1]));
		FUNCTIONS.put("CBRT", (parms) -> Math.cbrt(parms[0]));
		FUNCTIONS.put("CEIL", (parms) -> Math.ceil(parms[0]));
		FUNCTIONS.put("FLOOR", (parms) -> Math.floor(parms[0]));
		FUNCTIONS.put("TRUNC", (parms) -> (double)((long)parms[0]));
		FUNCTIONS.put("ROUND", (parms) -> (double)Math.round(parms[0]));
		FUNCTIONS.put("ABS", (parms) -> Math.abs(parms[0]));
		FUNCTIONS.put("HYPOT", (parms) -> Math.hypot(parms[0],parms[1]));
		FUNCTIONS.put("FMOD", (parms) -> (double)Math.floorMod((long)parms[0],(long)parms[1]));
	}
	
	private final static class Defaults {
		private final static AsmStdlib DEFAULT = new AsmStdlib();
	}
	
	@Override
	public List<String> definitions() {
		return Stream.concat(
				CONSTANTS.keySet().stream(),
				FUNCTIONS.keySet().stream()
			).
			sorted().
			toList();
	}

	@Override
	public boolean isDefined(String function) {
		return definitions().contains(function.toUpperCase());
	}
	
	public double invoke(String name, double... parms) {
		var cnst = CONSTANTS.get(name.toUpperCase());
		if(cnst == null) {
			var func = FUNCTIONS.get(name.toUpperCase());
			if(func == null) {
				throw new IllegalArgumentException("No such function or constant as " + name);
			}
			
			return func.apply(parms);
		}
		else {
			if(parms.length > 0) {
				throw new IllegalArgumentException(name + " is a constant, not a function.");
			}
			return cnst;
		}
	}

	public static AsmStdlib get() {
		return Defaults.DEFAULT;
	}
	
	public static double asinh(double x) {
		return Math.log(x + Math.sqrt(x*x + 1));
	}
	
	public static double acosh(double x) {
		return Math.log(x + Math.sqrt(x - 1) * Math.sqrt(x + 1));
	}
	
	public static double atanh(double x) {
		return 0.5d * Math.log((1d + x) / (1d - x));
	}
	
	public static int log2(int N)
    {

        // calculate log2 N indirectly
        // using log() method
        int result = (int)(Math.log(N) / Math.log(2));

        return result;
    }
	
}
