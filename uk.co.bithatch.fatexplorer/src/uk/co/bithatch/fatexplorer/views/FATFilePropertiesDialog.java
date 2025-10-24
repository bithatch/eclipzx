package uk.co.bithatch.fatexplorer.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import uk.co.bithatch.fatexplorer.vfs.FATImageFileStore;

public class FATFilePropertiesDialog extends Dialog {

	private final FATImageFileStore store;

	public FATFilePropertiesDialog(Shell parentShell, FATImageFileStore store) {
		super(parentShell);
		this.store = store;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout(2, false));
		
		new Label(composite, SWT.NONE).setText("Name:");

		var name = new Label(composite, SWT.NONE);
		name.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		name.setText(store.getName());
		
		new Label(composite, SWT.NONE).setText("Size:");

		var info = store.fetchInfo();
		var size = new Label(composite, SWT.NONE);
		size.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		size.setText(String.format("%d bytes",  info.getLength()));
		
		return composite;
	}


}
