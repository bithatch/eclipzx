package uk.co.bithatch.zxbasic.ui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;

public class DefinesTableFieldEditor extends FieldEditor {
    private TableViewer viewer;
    private List<DefineEntry> defines = new ArrayList<>();
	private Button removeBtn;
	private Button addBtn;

    public DefinesTableFieldEditor(String name, String labelText, Composite parent) {
        init(name, labelText);
        createControl(parent);
    }

	@Override
	public void setEnabled(boolean enabled, Composite parent) {
		viewer.getTable().setEnabled(enabled);
		addBtn.setEnabled(enabled);
		removeBtn.setEnabled(enabled);
	}

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Label label = getLabelControl(parent);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, numColumns, 1));

        viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, numColumns, 1));
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);

        TableViewerColumn nameCol = new TableViewerColumn(viewer, SWT.LEFT);
        nameCol.getColumn().setText("Name");
        nameCol.getColumn().setWidth(150);

        TableViewerColumn valueCol = new TableViewerColumn(viewer, SWT.LEFT);
        valueCol.getColumn().setText("Value");
        valueCol.getColumn().setWidth(200);

        viewer.setContentProvider(ArrayContentProvider.getInstance());

        nameCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DefineEntry) element).name;
            }
        });

        valueCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((DefineEntry) element).value;
            }
        });

        // Add inline editors
        viewer.setColumnProperties(new String[] { "name", "value" });

        CellEditor[] editors = new CellEditor[] {
            new TextCellEditor(viewer.getTable()),
            new TextCellEditor(viewer.getTable())
        };

        viewer.setCellEditors(editors);
        viewer.setCellModifier(new ICellModifier() {
            @Override
            public boolean canModify(Object element, String property) {
                return true;
            }

            @Override
            public Object getValue(Object element, String property) {
                DefineEntry entry = (DefineEntry) element;
                return "name".equals(property) ? entry.name : entry.value;
            }

            @Override
            public void modify(Object element, String property, Object value) {
                TableItem item = (TableItem) element;
                DefineEntry entry = (DefineEntry) item.getData();
                if ("name".equals(property)) {
                    entry.name = value.toString();
                } else if ("value".equals(property)) {
                    entry.value = value.toString();
                }
                viewer.update(entry, null);
            }
        });

        // Buttons
        Composite buttonComp = new Composite(parent, SWT.NONE);
        buttonComp.setLayout(new GridLayout(2, false));
        buttonComp.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, numColumns, 1));

        addBtn = new Button(buttonComp, SWT.PUSH);
        addBtn.setText("Add");
        addBtn.addListener(SWT.Selection, e -> {
            DefineEntry newEntry = new DefineEntry("new_define", "");
            defines.add(newEntry);
            viewer.refresh();
            viewer.editElement(newEntry, 0); // immediately edit name
        });

        removeBtn = new Button(buttonComp, SWT.PUSH);
        removeBtn.setText("Remove");
        removeBtn.addListener(SWT.Selection, e -> {
            IStructuredSelection selection = viewer.getStructuredSelection();
            if (!selection.isEmpty()) {
                defines.remove(selection.getFirstElement());
                viewer.refresh();
            }
        });

        viewer.setInput(defines);
    }

    @Override
    protected void doStore() {
        String serialized = defines.stream()
            .map(e -> e.name + "=" + e.value)
            .collect(Collectors.joining(";"));
        getPreferenceStore().setValue(getPreferenceName(), serialized);
    }

    @Override
    protected void doLoad() {
        defines.clear();
        String value = getPreferenceStore().getString(getPreferenceName());
        if (value != null && !value.isBlank()) {
            for (String pair : value.split(";")) {
                String[] nv = pair.split("=", 2);
                defines.add(new DefineEntry(nv[0], nv.length > 1 ? nv[1] : ""));
            }
        }
        if (viewer != null) viewer.refresh();
    }

    @Override
    protected void doLoadDefault() {
        getPreferenceStore().setToDefault(getPreferenceName());
        doLoad();
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }

    static class DefineEntry {
        String name;
        String value;

        DefineEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

	@Override
	protected void adjustForNumColumns(int arg0) {
		// TODO Auto-generated method stub
		
	}
}
