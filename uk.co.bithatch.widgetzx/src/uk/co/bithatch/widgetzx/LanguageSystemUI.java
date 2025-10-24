package uk.co.bithatch.widgetzx;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import uk.co.bithatch.bitzx.IDescribed;
import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.LanguageSystem;

public class LanguageSystemUI {

	public static String[][] architectureOptions() {
		return architectureOptions(null);
	}

	public static String[][] architectureOptions(IResource resource) {
		return describedOptions(LanguageSystem.architecturesFor(resource));
	}

	public static String[][] outputFormatOptions() {
		return outputFormatOptions(null);
	}

	public static String[][] outputFormatOptions(IResource resource) {
		return describedOptions(LanguageSystem.outputFormatsFor(resource));
	}

	public static String[][] describedOptions(List<? extends IDescribed> described) {
		var arr = new String[described.size()][2];
		for (int i = 0; i < arr.length; i++) {
			arr[i][0] = described.get(i).fullDescription();
			arr[i][1] = described.get(i).name();
		}
		return arr;
	}


	public static String[] architectureNames() {
		return architectureNames(null);
	}

	public static String[] architectureNames(IResource resource) {
		return describedNames(LanguageSystem.architecturesFor(resource));
	}

	public static String[] describedNames(List<? extends IDescribed> described) {
		var arr = new String[described.size()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = described.get(i).name();
		}
		return arr;
	}

	public static List<String> languageSystemNamesFor(IProject proj) {
		return LanguageSystem.descriptorsFor(proj).stream().map(d -> d.name()).toList();
	}
}
