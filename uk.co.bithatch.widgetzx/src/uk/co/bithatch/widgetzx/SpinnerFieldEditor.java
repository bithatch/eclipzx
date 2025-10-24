package uk.co.bithatch.widgetzx;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class SpinnerFieldEditor extends FieldEditor {
    private Spinner spinner;
    private int min, max, increment;
	private Composite parent;

    public SpinnerFieldEditor(String name, String labelText, Composite parent,
                              int min, int max, int increment) {
        init(name, labelText);
        this.min = min;
        this.parent = parent;
        this.max = max;
        this.increment = increment;
        createControl(parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        ((GridData) spinner.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Label label = getLabelControl(parent);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        spinner = new Spinner(parent, SWT.BORDER);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setIncrement(increment);
        spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    @Override
	public void setEnabled(boolean enabled, Composite parent) {
		super.setEnabled(enabled, this.parent);
		spinner.setEnabled(enabled);
	}

	@Override
    protected void doLoad() {
        if (spinner != null) {
            spinner.setSelection(getPreferenceStore().getInt(getPreferenceName()));
        }
    }

    @Override
    protected void doLoadDefault() {
        if (spinner != null) {
            spinner.setSelection(getPreferenceStore().getDefaultInt(getPreferenceName()));
        }
    }

    @Override
    protected void doStore() {
        getPreferenceStore().setValue(getPreferenceName(), spinner.getSelection());
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }
}
