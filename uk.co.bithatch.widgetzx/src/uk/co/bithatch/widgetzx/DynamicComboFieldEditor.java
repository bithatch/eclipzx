package uk.co.bithatch.widgetzx;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

public class DynamicComboFieldEditor extends FieldEditor {
    private Combo combo;
    private String[][] entryNamesAndValues;
    private String value;
	private Composite parent;

    public DynamicComboFieldEditor(String name, String labelText, String[][] entryNamesAndValues, Composite parent) {
        init(name, labelText);
        this.parent = parent;
        this.entryNamesAndValues = entryNamesAndValues;
        createControl(parent);

        populateCombo(entryNamesAndValues);
    }
    
    public Combo getCombo() {
    	return combo;
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        ((GridData) combo.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        getLabelControl(parent);

        combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numColumns - 1, 1));
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = combo.getSelectionIndex();
                if (index >= 0 && index < entryNamesAndValues.length) {
                    value = entryNamesAndValues[index][1];
                    fireValueChanged(VALUE, null, value);
                }
            }
        });
    }

    @Override
	public void setEnabled(boolean enabled, Composite parent) {
		super.setEnabled(enabled, this.parent);
		combo.setEnabled(enabled);
	}

	private void populateCombo(String[][] entryNamesAndValues) {
        combo.removeAll();
        for (String[] entry : entryNamesAndValues) {
            combo.add(entry[0]);
        }
        IPreferenceStore preferenceStore = getPreferenceStore();
        if(preferenceStore != null)
        	selectValue(preferenceStore.getString(getPreferenceName()));
    }

    public void updateEntries(String[][] newEntries) {
        this.entryNamesAndValues = newEntries;
        populateCombo(newEntries);
    }

    private void selectValue(String target) {
        for (int i = 0; i < entryNamesAndValues.length; i++) {
            if (entryNamesAndValues[i][1].equals(target)) {
                combo.select(i);
                value = target;
                return;
            }
        }
        if (entryNamesAndValues.length > 0) {
            combo.select(0);
            value = entryNamesAndValues[0][1];
        }
    }

    @Override
    protected void doLoad() {
        selectValue(getPreferenceStore().getString(getPreferenceName()));
    }

    @Override
    protected void doLoadDefault() {
        selectValue(getPreferenceStore().getDefaultString(getPreferenceName()));
    }

    @Override
    protected void doStore() {
        if (value != null) {
            getPreferenceStore().setValue(getPreferenceName(), value);
        }
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }
}
