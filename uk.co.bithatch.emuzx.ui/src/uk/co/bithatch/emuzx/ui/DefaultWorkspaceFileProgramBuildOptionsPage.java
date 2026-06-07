package uk.co.bithatch.emuzx.ui;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.jface.resource.JFaceResources.getFontRegistry;
import static uk.co.bithatch.bitzx.AbstractResourceProperties.getProperty;
import static uk.co.bithatch.bitzx.AbstractResourceProperties.setProperty;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.dialogs.PropertyPage;

public class DefaultWorkspaceFileProgramBuildOptionsPage extends PropertyPage {

	private Spinner orgAddress;
	private Label orgAddressLabel;

	@Override
	protected final Control createContents(Composite parent) {
		var composite = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.verticalSpacing = 8;
		composite.setLayout(layout);
		
		var infoLabel = new Label(composite, SWT.WRAP);
		infoLabel.setText("Depending on the source type, these settings may be overriden by special in-source instructions. E.g. NextBuild-like special processing comments in the source header for ZX Basic, or '$org' statements in asseembly.");
		infoLabel.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).hint(200, 48).create());
        infoLabel.setFont(getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));

		preCreate(composite);

		orgAddressLabel = new Label(composite, SWT.NONE);
		orgAddressLabel.setText("ORG Address:");
		orgAddress = new Spinner(composite, SWT.NONE);
		orgAddress.setValues(0, 0, 65536, 0, 1, 256);
		orgAddress.setToolTipText("Set the ORG address. This address will be used when generating tapes and other loadable formats for the location to LOAD your code. When zero, the default will be used (32678). ");
		orgAddress.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());

		postCreate(composite);

		
		var file = (IFile) getElement().getAdapter(IFile.class);
		orgAddress.setSelection(getProperty(file, ResourceProperties.ORG_ADDRESS, 0));
		
		updateState();
		return composite;
	}

	protected void postCreate(Composite composite) {
	}

	protected void preCreate(Composite composite) {
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		orgAddress.setSelection(0);
		updateState();
	}

	@Override
	public boolean performOk() {
		var file = getElement().getAdapter(IFile.class);
		setProperty(file, ResourceProperties.ORG_ADDRESS, orgAddress.getSelection());
		return true;
	}
	
	protected boolean isEnable() {
		return true;
	}
	
	protected void updateState() {
		var sel = isEnable();
		orgAddressLabel.setEnabled(sel);
		orgAddress.setEnabled(sel);
	}

}
