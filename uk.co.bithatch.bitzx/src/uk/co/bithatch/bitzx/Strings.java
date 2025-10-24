package uk.co.bithatch.bitzx;

import java.util.Arrays;
import java.util.List;

public class Strings {
	
	public static String normalizeMultilineString(String str) {
		return str.replace("\r\n"," ").replace("\n\r", " ").replace("\n", " ").replace("\r", " ");
	}

	public static List<String> separatedList(String str, String pattern) {
		return Arrays.asList(str == null || str.length() == 0 ? new String[0] : str.split(pattern)); 
	}

	public static String limit(String txt, int len) {
		if(txt.length() > len) {
			return txt.substring(0, len - 2) + "..";
		}
		return txt;
	}

	public static String cutAndLimit(int cut, String txt, int len) {
		if(txt.length() > cut) {
			txt = txt.substring(cut);
		}
		return limit(txt, len);
	}
}
