package uk.co.bithatch.zxbasic.interpreter.util;

import uk.co.bithatch.zxbasic.basic.BasicPackage;
import uk.co.bithatch.zxbasic.basic.DimDeclaration;
//import uk.co.bithatch.zxbasic.basic.VarRef;

public class ArrayUtils {

//    public static boolean hasIndex(VarRef line) {
//        return line.eIsSet(BasicPackage.Literals.VAR_REF__INDEX);
//    	return false;
//    }

    public static boolean isArrayDeclaration(DimDeclaration line) {
        return line.eIsSet(BasicPackage.Literals.DIM_DECLARATION__ARRAY_INDEX);
    }
}
	