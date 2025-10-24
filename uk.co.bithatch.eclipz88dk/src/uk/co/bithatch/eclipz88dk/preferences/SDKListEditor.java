package uk.co.bithatch.eclipz88dk.preferences;

import java.io.File;
import java.util.regex.Pattern;

import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;

public class SDKListEditor extends ListEditor {

	public SDKListEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
	}

	@Override
	public void setEnabled(boolean enabled, Composite parent) {
		getList().setEnabled(enabled);
		getAddButton().setEnabled(enabled);
		getRemoveButton().setEnabled(enabled);
	}

	@Override
	protected String createList(String[] items) {
		return String.join(File.pathSeparator, items);
	}

	@Override
	protected String[] parseString(String stringList) {
		return stringList.isEmpty() ? new String[0] : stringList.split(Pattern.quote(File.pathSeparator));
	}

	@Override
	protected String getNewInputObject() {
		var shell = getShell();
		var dirDialog = new DirectoryDialog(shell);
		dirDialog.setText("Select Folder");
		var selected = dirDialog.open();
		if (selected != null) {
			return selected;
		}

		return null;
	}

	@Override
	protected void adjustForNumColumns(int numColumns) {
		var gd = new GridData(SWT.FILL, SWT.FILL, true, true, numColumns, 1);
		getList().setLayoutData(gd);
		super.adjustForNumColumns(numColumns);
	}
}
