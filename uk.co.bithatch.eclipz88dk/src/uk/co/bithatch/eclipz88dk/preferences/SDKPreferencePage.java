package uk.co.bithatch.eclipz88dk.preferences;

public class SDKPreferencePage extends AbstractZ88DKPreferencePage {

	public SDKPreferencePage() {
		super(PreferenceConstants.SDKS, GRID);
		setDescription("""
				Add the locations of your Z88DK installations. If you have set the `ZCCCFG`
				variables as per Z88DK instructions, that installation will be automatically
				added.
				""");
	}

	@Override
	protected void createFieldEditors() {
		addField(new SDKListEditor(PreferenceConstants.SDK_PATHS, "Z88DK Homes", getFieldEditorParent()));

	}
}
