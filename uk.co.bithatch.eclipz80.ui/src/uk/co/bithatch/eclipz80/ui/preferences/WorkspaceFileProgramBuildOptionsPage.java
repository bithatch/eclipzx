package uk.co.bithatch.eclipz80.ui.preferences;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static uk.co.bithatch.bitzx.AbstractResourceProperties.getProperty;
import static uk.co.bithatch.bitzx.AbstractResourceProperties.setProperty;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import uk.co.bithatch.emuzx.ui.DefaultWorkspaceFileProgramBuildOptionsPage;
import uk.co.bithatch.emuzx.ui.ResourceProperties;


public class WorkspaceFileProgramBuildOptionsPage extends DefaultWorkspaceFileProgramBuildOptionsPage {

	private Spinner clearAddress;
	private Label clearAddressLabel;

	@Override
	protected void postCreate(Composite composite) {
		var file = (IFile) getElement().getAdapter(IFile.class);
		
		clearAddressLabel = new Label(composite, SWT.NONE);
		clearAddressLabel.setText("RAMTOP Address:");
		clearAddress = new Spinner(composite, SWT.NONE);
		clearAddress.setToolTipText("When a BASIC loader is generated, a CLEAR <address> is injected with this address.");
		clearAddress.setValues(0, 0, 65536, 0, 1, 256);
		clearAddress.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());

		clearAddress.setSelection(getProperty(file, ResourceProperties.CLEAR_ADDRESS, 0));
	}

	@Override
	protected void performDefaults() {
		clearAddress.setSelection(0);
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		var file = getElement().getAdapter(IFile.class);
		setProperty(file, ResourceProperties.CLEAR_ADDRESS, clearAddress.getSelection());
		return super.performOk();
	}

	@Override
	protected void updateState() {
		var sel = isEnable();
		clearAddressLabel.setEnabled(sel);
		clearAddress.setEnabled(sel);
	}

}
