package uk.co.bithatch.zxbasic.ui.preferences;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.jface.resource.JFaceResources.getFontRegistry;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.getProperty;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.setProperty;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.dialogs.PropertyPage;

import uk.co.bithatch.zxbasic.ui.builder.ResourceProperties;


public class WorkspaceFileProgramBuildOptionsPage extends PropertyPage {

	private Spinner orgAddress;
	private Spinner heapAddress;
	private Spinner heapSize;
	private Button build;
	private Label orgAddressLabel;
	private Label heapAddressLabel;
	private Label heapSizeLabel;

	@Override
	protected Control createContents(Composite parent) {
		var composite = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.verticalSpacing = 8;
		composite.setLayout(layout);
		
		var infoLabel = new Label(composite, SWT.WRAP);
		infoLabel.setText("These settings may be overriden by special NextBuild-like special processing comments in the source header.");
		infoLabel.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).hint(200, 48).create());
        infoLabel.setFont(getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));

		build = new Button(composite, SWT.CHECK);
		build.setText("Compile this program");
		build.setToolTipText("De-select this if this source is not a compiled program, but a library included by others.");
		build.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).create());
		build.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateState()));

		orgAddressLabel = new Label(composite, SWT.NONE);
		orgAddressLabel.setText("Org Address:");
		orgAddress = new Spinner(composite, SWT.NONE);
		orgAddress.setValues(0, 0, 65536, 0, 1, 256);
		orgAddress.setToolTipText("Set the ORG address (start of code). When zero, the default will be used (32678).");
		orgAddress.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());

		heapAddressLabel = new Label(composite, SWT.NONE);
		heapAddressLabel.setText("Heap Address:");
		heapAddress = new Spinner(composite, SWT.NONE);
		heapAddress.setToolTipText("Set the HEAP address (start of heap). When zero, the default will be used.");
		heapAddress.setValues(0, 0, 65536, 0, 1, 256);
		heapAddress.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());

		heapSizeLabel = new Label(composite, SWT.NONE);
		heapSizeLabel.setText("Heap Size:");
		heapSize = new Spinner(composite, SWT.NONE);
		heapSize.setValues(0, 0, 65536, 0, 1, 256);
		heapSize.setToolTipText("Set the HEAP size. When zero, the default will be used (4768 bytes).");
		heapSize.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).create());

		
		var file = (IFile) getElement().getAdapter(IFile.class);
		build.setSelection(getProperty(file, ResourceProperties.BUILD, true));
		orgAddress.setSelection(getProperty(file, ResourceProperties.ORG_ADDRESS, 0));
		heapAddress.setSelection(getProperty(file, ResourceProperties.HEAP_ADDRESS, 0));
		heapSize.setSelection(getProperty(file, ResourceProperties.HEAP_SIZE, 0));
		
		updateState();
		return composite;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		build.setSelection(true);
		orgAddress.setSelection(0);
		heapAddress.setSelection(0);
		heapSize.setSelection(0);
		updateState();
	}

	@Override
	public boolean performOk() {
		var file = getElement().getAdapter(IFile.class);
		setProperty(file, ResourceProperties.BUILD, build.getSelection());
		setProperty(file, ResourceProperties.ORG_ADDRESS, orgAddress.getSelection());
		setProperty(file, ResourceProperties.HEAP_ADDRESS, heapAddress.getSelection());
		setProperty(file, ResourceProperties.HEAP_SIZE, heapSize.getSelection());
		return true;
	}
	
	private void updateState() {
		var sel = build.getSelection();
		orgAddressLabel.setEnabled(sel);
		orgAddress.setEnabled(sel);
		heapAddressLabel.setEnabled(sel);
		heapAddress.setEnabled(sel);
		heapSizeLabel.setEnabled(sel);
		heapSize.setEnabled(sel);
	}

}
