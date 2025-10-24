package uk.co.bithatch.zxbasic.ui.preferences;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.zxbasic.ui.library.ContributedLibraryRegistry;
import uk.co.bithatch.zxbasic.ui.library.ZXLibrary;

public class ContributedLibrariesFieldEditor extends FieldEditor {

    private CheckboxTableViewer viewer;
    private List<ZXLibrary> allLibraries;
    private Set<String> selectedNames = new HashSet<>();
    private final IProject project;

    public ContributedLibrariesFieldEditor(String name, String labelText, Composite parent, IProject project) {
        this.project = project;
        init(name, labelText);
        createControl(parent);
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Label label = getLabelControl(parent);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, numColumns, 1));
        
       	viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, numColumns, 1);
		viewer.getTable().setLayoutData(gd);
        
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new LibraryLabelProvider());

        refresh();
    }

	@Override
	public void setEnabled(boolean enabled, Composite parent) {
		viewer.getTable().setEnabled(enabled);
	}

	public void refresh() {
		allLibraries = ContributedLibraryRegistry.getEditableProjectLibraries(project);
        viewer.setInput(allLibraries);
	}
    

    @Override
    protected void doLoad() {
        if (viewer != null) {
            String stored = getPreferenceStore().getString(getPreferenceName());
            selectedNames = stored.equals("") ? Collections.emptySet() : new HashSet<>(Arrays.asList(stored.split(Pattern.quote(File.pathSeparator))));
            updateViewerChecks();
        }
    }

    @Override
    protected void doLoadDefault() {
        selectedNames = Set.of(); // no default libraries selected
        updateViewerChecks();
    }

    @Override
    protected void doStore() {
        if (viewer != null) {
            Object[] checked = viewer.getCheckedElements();
            String joined = Arrays.stream(checked)
                .map(obj -> ((ZXLibrary) obj).name())
                .collect(Collectors.joining(File.pathSeparator));
            getPreferenceStore().setValue(getPreferenceName(), joined);
        }
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }

    private void updateViewerChecks() {
        for (ZXLibrary lib : allLibraries) {
            viewer.setChecked(lib, selectedNames.contains(lib.name()));
        }
    }

    public List<ZXLibrary> getSelectedLibraries() {
        return Arrays.stream(viewer.getCheckedElements())
                     .map(e -> (ZXLibrary) e)
                     .toList();
    }

	@Override
	protected void adjustForNumColumns(int numColumns) {
		// TODO Auto-generated method stub
		
	}
}
