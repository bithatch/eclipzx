package uk.co.bithatch.zxbasic.ui.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;
import uk.co.bithatch.zxbasic.ui.library.ZXSDK;

public class ZXBasicExamplesWizardPage extends AbstractBasicProjectWizardPage {

    Combo sdk;

	public ZXBasicExamplesWizardPage() {
        super("ZX BASIC Examples Project");
        setTitle("ZX BASIC Examples Project");
        setDescription("Create a new ZX BASIC project with all provided examples.");
    }

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		setProject("zxbasic-examples");
		dialogChanged();
	}

	@Override
	protected void createFields(Composite container) {

        var label = new Label(container, SWT.NONE);
        label.setText("SDK:");

        sdk = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        sdk.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        var items = ContributedSDKRegistry.getAllSDKs().stream().map(ZXSDK::name).toList().toArray(new String[0]);
		sdk.setItems(items);
		sdk.select(0);
		
	}
}
