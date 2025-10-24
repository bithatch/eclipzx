package uk.co.bithatch.eclipz88dk.preferences;

import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionListener;

import uk.co.bithatch.eclipz88dk.toolchain.Z88DKSDK;
import uk.co.bithatch.widgetzx.DynamicComboFieldEditor;

public class CompilerPreferencePage extends AbstractZ88DKPreferencePage {

	private DynamicComboFieldEditor sdks;
	private DynamicComboFieldEditor system;
	private DynamicComboFieldEditor clib;

	public CompilerPreferencePage() {
		super(PreferenceConstants.COMPILER, GRID);
	}

	@Override
	protected void createFieldEditors() {
		super.createFieldEditors();

		sdks = new DynamicComboFieldEditor(PreferenceConstants.SDK, "SDK:", getSDKChoices(), getFieldEditorParent());
		sdks.getCombo().addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			updateSystems(getSDKFromCombo());
		}));

		addField(sdks);

		var initSdk = getSDKFromCombo();

		system = new DynamicComboFieldEditor(PreferenceConstants.SYSTEM, "System:", getSystems(initSdk),
				getFieldEditorParent());
		system.getCombo().addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			updateClibs(getSDKFromCombo(), system.getCombo().getText());
		}));

		addField(system);

		clib = new DynamicComboFieldEditor(PreferenceConstants.CLIB, "C Library:",
				getClibs(initSdk, getPreferenceStore().getString(PreferenceConstants.SYSTEM)), getFieldEditorParent());
		addField(clib);

		getPreferenceStore().addPropertyChangeListener(this);
	}

	protected Z88DKSDK getSDKFromCombo() {
		return Z88DKPreferencesAccess.get().getSDKByName(sdks.getCombo().getText()).orElseGet(() ->
			Z88DKPreferencesAccess.get().getDefaultSDK().orElse(null)
		);
	}

	private String[][] getSDKChoices() {
		return Z88DKPreferencesAccess.get().getAllSDKsAsOptions();
	}

	@Override
	public void dispose() {
		getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (PreferenceConstants.SDK_PATHS.equals(event.getProperty())) {
			sdks.updateEntries(getSDKChoices());
		} else if (PreferenceConstants.SDK.equals(event.getProperty())) {
			updateSystems(getSDKFromCombo());
			updateClibs(getSDKFromCombo(), system.getCombo().getText());
		} else if (PreferenceConstants.SYSTEM.equals(event.getProperty())) {
			updateClibs(getSDKFromCombo(), (String) event.getNewValue());
		}

	}

	private String[][] getSystems(Z88DKSDK sdk) {
		if (sdk == null)
			return new String[0][0];
		else
			return sdk.configurations().getAllSystemsAsOptions();

	}

	private void updateSystems(Z88DKSDK sdk) {
		system.updateEntries(getSystems(sdk));
	}

	private String[][] getClibs(Z88DKSDK sdkPath, String system) {
		if (sdkPath != null) {
			var cfg = sdkPath.configurations().configuration(system);
			if (cfg.isPresent()) {
				return cfg.get().getAllCLibrariesAsOptions();
			}
		}
		return new String[0][0];
	}

	private void updateClibs(Z88DKSDK sdkPath, String system) {
		clib.updateEntries(getClibs(sdkPath, system));
	}

}
