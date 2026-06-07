package uk.co.bithatch.zxbasic.ui.preferences;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static uk.co.bithatch.bitzx.AbstractResourceProperties.getProperty;
import static uk.co.bithatch.bitzx.AbstractResourceProperties.setProperty;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import uk.co.bithatch.emuzx.ui.DefaultWorkspaceFileProgramBuildOptionsPage;
import uk.co.bithatch.zxbasic.ui.builder.ResourceProperties;


public class WorkspaceFileProgramBuildOptionsPage extends DefaultWorkspaceFileProgramBuildOptionsPage {

	private Spinner heapAddress;
	private Spinner heapSize;
	private Button build;
	private Label heapAddressLabel;
	private Label heapSizeLabel;

	@Override
	protected void postCreate(Composite composite) {
		var file = (IFile) getElement().getAdapter(IFile.class);
		
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
		heapAddress.setSelection(getProperty(file, ResourceProperties.HEAP_ADDRESS, 0));
		heapSize.setSelection(getProperty(file, ResourceProperties.HEAP_SIZE, 0));
	}

	@Override
	protected void preCreate(Composite composite) {
		var file = (IFile) getElement().getAdapter(IFile.class);
		build.setSelection(getProperty(file, ResourceProperties.BUILD, true));
		build = new Button(composite, SWT.CHECK);
		build.setText("Compile this program");
		build.setToolTipText("De-select this if this source is not a compiled program, but a library included by others.");
		build.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).create());
		build.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateState()));
	}
	
	@Override
	protected void performDefaults() {
		build.setSelection(true);
		heapAddress.setSelection(0);
		heapSize.setSelection(0);
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		var file = getElement().getAdapter(IFile.class);
		setProperty(file, ResourceProperties.BUILD, build.getSelection());
		setProperty(file, ResourceProperties.HEAP_ADDRESS, heapAddress.getSelection());
		setProperty(file, ResourceProperties.HEAP_SIZE, heapSize.getSelection());
		return super.performOk();
	}

	@Override
	protected boolean isEnable() {
		return build.getSelection();
	}

	@Override
	protected void updateState() {
		var sel = isEnable();
		heapAddressLabel.setEnabled(sel);
		heapAddress.setEnabled(sel);
		heapSizeLabel.setEnabled(sel);
		heapSize.setEnabled(sel);
	}

}
