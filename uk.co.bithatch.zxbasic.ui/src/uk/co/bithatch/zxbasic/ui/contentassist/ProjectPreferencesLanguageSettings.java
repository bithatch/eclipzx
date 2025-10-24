package uk.co.bithatch.zxbasic.ui.contentassist;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import uk.co.bithatch.zxbasic.ILanguageSettings;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ProjectPreferencesLanguageSettings implements ILanguageSettings {

	@Override
	public boolean isNormalizeCase(EObject context, String id) {
		/* TODO get from project somehow */
		return ZXBasicPreferencesAccess.get().isIgnoreVariableCase(null);
	}

	@Override
	public Map<String, String> defines() {
		/* TODO get from project somehow */
		return ZXBasicPreferencesAccess.get().getDefines(null);
	}

}
