package uk.co.bithatch.ayzxfx.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class AFBConfigurationWizardPage extends WizardPage {

	private Spinner numberOfEffects;
	private Spinner numberOfFrames;

	public AFBConfigurationWizardPage() {
		super("Configuration");
        setTitle("Effects Bank Configuration");
        // https://shiru.untergrund.net/software.shtml
        setDescription("Configure the initial effects banks. All configuration can be changed later.");
	}

	@Override
	public void createControl(Composite parent) {

        var container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(2, false);
        layout.verticalSpacing = 8;
        layout.horizontalSpacing = 16;
		container.setLayout(layout);

        var label = new Label(container, SWT.NONE);
        label.setText("Number Of Effects:");

        numberOfEffects = new Spinner(container, SWT.NONE);
        numberOfEffects.setValues(10, 1, 256, 0, 1, 10);
        numberOfEffects.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        label = new Label(container, SWT.NONE);
        label.setText("Initial Number Of Frames:");

        numberOfFrames= new Spinner(container, SWT.NONE);
        numberOfFrames.setValues(10, 1, 256, 0, 1, 10);
        numberOfFrames.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		setControl(container);
		setMessage(null);
		setErrorMessage(null);
		
	}

	public int getNumberOfEffects() {
		return numberOfEffects.getSelection();
	}

	public int getNumberOfFrames() {
		return numberOfFrames.getSelection();
	}

}
