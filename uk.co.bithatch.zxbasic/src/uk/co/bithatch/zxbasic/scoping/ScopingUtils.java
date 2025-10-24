package uk.co.bithatch.zxbasic.scoping;

import org.eclipse.emf.ecore.EObject;

import uk.co.bithatch.zxbasic.ILanguageSettings;
import uk.co.bithatch.zxbasic.basic.Group;

public class ScopingUtils {
	
	public static String normalizeIdentifier(String id, ILanguageSettings settings, EObject context) {
		return settings.isNormalizeCase(context, id) ? normalize(id) : id;
	}
	
	public static String normalize(String id) {
		return id.toUpperCase();
	}
	
	public static boolean hasNumberOrLabel(Group group) {
		return group.getName() != null;
	}
	
	public static boolean hasNumber(Group group) {
		return isNumber(group.getName());
	}
	
	public static boolean hasLabel(Group group) {
		return group.getName() != null && !isNumber(group.getName());
	}

	public static String stripLabelSuffix(String labelname) {
		if(labelname != null) {
			while(labelname.endsWith(":"))
				labelname = labelname.substring(0, labelname.length() - 1);
		}
		return labelname;
	}

	public static boolean isLabel(String txt) {
		return txt != null && !isNumber(txt);
	}

	public static boolean isNumber(String txt) {
		try {
			Integer.parseInt(txt);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String numberOrLabel(Group line) {
		if(line.getName() != null) {
			return stripLabelSuffix(line.getName());
		}
		else {
			throw new IllegalArgumentException("No line or number.");
		}
	}

	public static String addLabelSuffix(String value) {
		if(!value.endsWith(":"))
			return value + ":";
		else
			return value;
	}
}
