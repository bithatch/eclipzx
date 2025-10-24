package uk.co.bithatch.zxbasic.ui.preferences;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import uk.co.bithatch.zxbasic.ui.library.ContributedLibraryRegistry;
import uk.co.bithatch.zxbasic.ui.library.ZXLibrary;

public class BuiltInLibrariesField extends FieldEditor {

	private TableViewer viewer;
	private List<ZXLibrary> allLibraries;
	private final IProject project;

	public BuiltInLibrariesField(String name, String labelText, Composite parent, IProject project) {
		this.project = project;
		init(name, labelText);
		createControl(parent);
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		Label label = getLabelControl(parent);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, numColumns, 1));

		viewer = new TableViewer(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, numColumns, 1);
//		gd.minimumHeight = 130;
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
		allLibraries = ContributedLibraryRegistry.getBuiltInProjectLibraries(project);
		viewer.setInput(allLibraries);
	}

	@Override
	public int getNumberOfControls() {
		return 2;
	}

	@Override
	protected void adjustForNumColumns(int numColumns) {
	}

	@Override
	protected void doLoad() {
	}

	@Override
	protected void doLoadDefault() {
	}

	@Override
	protected void doStore() {
	}
}
