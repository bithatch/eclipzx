package uk.co.bithatch.zxbasic;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;

public interface ILanguageSettings {

	boolean isNormalizeCase(EObject context, String id);
	
	Map<String, String> defines();
}
