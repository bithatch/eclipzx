package uk.co.bithatch.zxbasic.interpreter;

import java.util.Arrays;
import java.util.List;

import uk.co.bithatch.zxbasic.IReferenceIndex;
import uk.co.bithatch.zxbasic.scoping.ScopingUtils;

public class ZXStdlib implements IReferenceIndex {
	
	private final List<String> FUNCTIONS = Arrays.asList(
			"ABS", "ASN", "ATN", "CAST", "CHR", "CODE", "COS", "EXP", "INTEGER",
			"LBOUND", "UBOUND", "LEN", "LN", "SGN", "SIN", "SQR", "STR",
			"TAN", "USR", "VAL", "INKEY", "INKEY$");
	
	private final static class Defaults {
		private final static ZXStdlib DEFAULT = new ZXStdlib();
	}
	
	@Override
	public List<String> definitions() {
		return FUNCTIONS;
	}

	@Override
	public boolean isDefined(String function) {
		return FUNCTIONS.contains(ScopingUtils.normalize(function));
	}

	public static ZXStdlib get() {
		return Defaults.DEFAULT;
	}
	
	public  Var abs(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var asn(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var atn(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var cast(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var chr(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var code(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var cos(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var exp(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var integer(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}


	public  Var lbound(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var ubound(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var len(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var ln(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var sgn(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var sin(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var sqr(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var str(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var tan(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var usr(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public  Var val(Var fromObject) {
		// TODO Auto-generated method stub
		return null;
	}

//  if (name.equals("LBOUND")) {
//      ArrayValue array = (ArrayValue) variables.get(evaluateExpr(func.getArgs().get(0))).value();
//      return array.lowerBounds().get(func.getArgs().size() > 1 ? (Integer)enforceType(func.getArgs().get(1), VarType.LONG) : 0);
//  } else if (name.equals("UBOUND")) {
//      ArrayValue array = (ArrayValue) variables.get(evaluateExpr(func.getArgs().get(0))).value();
//      return array.upperBounds().get(func.getArgs().size() > 1 ? (Integer)enforceType(func.getArgs().get(1), VarType.LONG) : 0);
//  }else if (name.equals("INPUT")) {
//  	try(BufferedReader reader = new BufferedReader(new InputStreamReader(host.input()))) {
//          return reader.readLine();
//  	}
//  	catch(Exception e) {
//  		return "";
//  	}
//  }
}
