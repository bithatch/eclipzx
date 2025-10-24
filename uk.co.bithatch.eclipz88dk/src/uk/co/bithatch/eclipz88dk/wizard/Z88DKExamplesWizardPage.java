package uk.co.bithatch.eclipz88dk.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKSDK;

public class Z88DKExamplesWizardPage extends AbstractZ88DKProjectWizardPage {

	Combo sdk;

	public Z88DKExamplesWizardPage() {
		super("Z8DK Examples Project");
		setTitle("Z88DK Examples Project");
		setDescription(
				"Create a new Z88DK project with all provided examples configured for the default ZX Spectrum platform");
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		setProject("z88dk-examples");
		dialogChanged();
	}

	@Override
	protected void createFields(Composite container) {

		var label = new Label(container, SWT.NONE);
		label.setText("SDK:");

		sdk = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		sdk.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		var items = Z88DKPreferencesAccess.get().getAllSDKs().stream().map(Z88DKSDK::name).toList().toArray(new String[0]);
		sdk.setItems(items);
		sdk.select(0);

	}
}
