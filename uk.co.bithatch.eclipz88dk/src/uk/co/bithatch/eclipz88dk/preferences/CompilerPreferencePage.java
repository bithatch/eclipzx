package uk.co.bithatch.eclipz88dk.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionListener;

import uk.co.bithatch.eclipz88dk.toolchain.Z88DKSDK;
import uk.co.bithatch.widgetzx.DynamicComboFieldEditor;

public class CompilerPreferencePage extends AbstractZ88DKPreferencePage {

	private DynamicComboFieldEditor sdks;
	private DynamicComboFieldEditor system;
	private DynamicComboFieldEditor clib;
	private BooleanFieldEditor allArchs;

	public CompilerPreferencePage() {
		super(Z88DKPreferenceConstants.COMPILER, GRID);
	}

	@Override
	protected void createFieldEditors() {
		
		super.createFieldEditors();

		var sdkChoices = getSDKChoices();
		sdks = new DynamicComboFieldEditor(Z88DKPreferenceConstants.SDK, "SDK:", sdkChoices, getFieldEditorParent());
		sdks.getCombo().addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			updateSystems(getSDKFromCombo());
		}));

		addField(sdks);

		var initSdk = getSDKFromCombo();

		system = new DynamicComboFieldEditor(Z88DKPreferenceConstants.ARCHITECTURE, "System:", new String[0][0],
				getFieldEditorParent());
		system.getCombo().addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			updateClibs(getSDKFromCombo(), system.getCombo().getText());
		}));

		addField(system);

		clib = new DynamicComboFieldEditor(Z88DKPreferenceConstants.CLIB, "C Library:",
				new String[0][0], getFieldEditorParent());
		addField(clib);

		allArchs = new BooleanFieldEditor(Z88DKPreferenceConstants.ALL_ARCHITECTURES, "Enable all (non ZX) systems (not yet recommended)",
				BooleanFieldEditor.DEFAULT, getFieldEditorParent()) {
					@Override
					protected void valueChanged(boolean oldValue, boolean newValue) {
						super.valueChanged(oldValue, newValue);
						updateSystems(getSDKFromCombo());
						updateClibs(getSDKFromCombo(), system.getCombo().getText());
					}
			
		};
		addField(allArchs);

		updateSystems(initSdk);
		updateClibs(initSdk, getPreferenceStore().getString(Z88DKPreferenceConstants.ARCHITECTURE));
		
		if(sdkChoices.length == 0) {
			setErrorMessage("No SDKs found. Please add an SDK path in the main preferences.");
			setValid(false);
		}

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
		if (Z88DKPreferenceConstants.SDK_PATHS.equals(event.getProperty())) {
			sdks.updateEntries(getSDKChoices());
		} else if (Z88DKPreferenceConstants.SDK.equals(event.getProperty())) {
			updateSystems(getSDKFromCombo());
			updateClibs(getSDKFromCombo(), system.getCombo().getText());
		} else if (Z88DKPreferenceConstants.ARCHITECTURE.equals(event.getProperty())) {
			updateClibs(getSDKFromCombo(), (String) event.getNewValue());
		} else if (Z88DKPreferenceConstants.ALL_ARCHITECTURES.equals(event.getProperty())) {
			updateSystems(getSDKFromCombo());
			updateClibs(getSDKFromCombo(), system.getCombo().getText());
		}

	}

	private String[][] getSystems(Z88DKSDK sdk) {
		if (sdk == null)
			return new String[0][0];
		else {
			if(allArchs.getBooleanValue()) {
				return sdk.configurations().getAllSystemsAsOptions();
			}
			else {
				return sdk.configurations().getAllZXSystemsAsOptions();
			}
		}

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
