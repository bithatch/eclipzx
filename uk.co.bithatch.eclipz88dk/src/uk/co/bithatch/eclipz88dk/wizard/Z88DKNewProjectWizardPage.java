package uk.co.bithatch.eclipz88dk.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKSDK;

public class Z88DKNewProjectWizardPage extends AbstractZ88DKProjectWizardPage {

	Combo sdk;

	public Z88DKNewProjectWizardPage() {
		super("Z8DK New Project");
		setTitle("Z88DK New Project");
		setDescription(
				"Create a new Z88DK C project for the ZX Spectrum or ZX Spectrum Next");
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		setProject("my-zx-c-project");
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
