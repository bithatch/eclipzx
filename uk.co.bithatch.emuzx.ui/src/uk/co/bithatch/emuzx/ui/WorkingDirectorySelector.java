package uk.co.bithatch.emuzx.ui;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

public class WorkingDirectorySelector extends Composite {

	private final Text defaultDirLabel;
	private final Text otherDirLocation;
	private final Button defaultDir;
	private final Button otherDir;
	private IPath defaultDirContainer;
	private Button browseWorkspace;
	private Button browseFilesystem;
	private Button variables;

	public WorkingDirectorySelector(Composite parent, int style, IPath defaultDirContainer, IContainer root, Runnable onChange) {
		super(parent, style);

		this.defaultDirContainer = defaultDirContainer;

		var layout = new GridLayout(2, false);
		layout.horizontalSpacing = 8;
		layout.verticalSpacing = 8;
		setLayout(layout);

		defaultDir = new Button(this, SWT.RADIO);
		defaultDir.setText("Default:");
		defaultDir.setSelection(true);
		
		defaultDir.addSelectionListener(SelectionListener.widgetSelectedAdapter(e ->  {
			updateState(); 
			onChange.run();
		}));
		defaultDirLabel = new Text(this, SWT.NONE);
		defaultDirLabel.setEditable(false);
		defaultDirLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		otherDir = new Button(this, SWT.RADIO);
		otherDir.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> { 
			updateState(); 
			onChange.run();
		}));
		otherDir.setText("Other:");
		otherDirLocation = new Text(this, SWT.NONE);
		otherDirLocation.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		otherDirLocation.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				onChange.run();
			}
		});

		var buttons = new Composite(this, SWT.NONE);
		buttons.setLayoutData(
				GridDataFactory.fillDefaults().grab(true, false).span(2, 1).align(SWT.RIGHT, SWT.CENTER).create());
		var buttonsLayout = new GridLayout(3, true);
		buttonsLayout.horizontalSpacing = 8;
		buttons.setLayout(buttonsLayout);

		browseWorkspace = new Button(buttons, SWT.PUSH);
		browseWorkspace.setText("Workspace");
		browseWorkspace.setLayoutData(GridDataFactory.defaultsFor(browseWorkspace).create());
		browseWorkspace.addListener(SWT.Selection, e -> {
			var dialog = new ContainerSelectionDialog(otherDirLocation.getShell(), root, true,
					"Select a workspace relative working directory");
			if (dialog.open() == Window.OK) {
				var result = dialog.getResult();
				if (result.length > 0 && result[0] instanceof Path path) {
					otherDirLocation.setText(String.format("${workspace_loc:%s}", path.makeRelative()));
				}
			}
		});

		browseFilesystem = new Button(buttons, SWT.PUSH);
		browseFilesystem.setText("File System");
		browseFilesystem.setLayoutData(GridDataFactory.defaultsFor(browseFilesystem).create());
		browseFilesystem.addListener(SWT.Selection, e -> {
			var fileDialog = new DirectoryDialog(browseFilesystem.getShell(), SWT.OPEN);
			fileDialog.setText("Select Working Directory");
			var path = fileDialog.open();
			if (path != null) {
				otherDirLocation.setText(path);
			}
		});

		variables = new Button(buttons, SWT.PUSH);
		variables.setText("Variables");
		variables.setLayoutData(GridDataFactory.defaultsFor(variables).create());
		variables.addListener(SWT.Selection, e -> {
			var dialog = new StringVariableSelectionDialog(otherDirLocation.getShell());
			if (dialog.open() == Window.OK) {
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					otherDirLocation.insert(variable);
				}
			}
		});

		updateState();
	}
	
	public void defaultDirContainer(IPath defaultDirText) {
		this.defaultDirContainer = defaultDirText;
		updateState();
	}

	public void workingDirectory(String path) {
		if (path == null) {
			defaultDir.setSelection(true);
			otherDir.setSelection(false);
		} else {
			defaultDir.setSelection(false);
			otherDir.setSelection(true);
			otherDirLocation.setText(path);
		}
		updateState();
	}

	public String workingDirectory() {
		if (defaultDir.getSelection())
			return null;
		else
			return otherDirLocation.getText();
	}

	private void updateState() {
		defaultDirLabel.setText(defaultDirContainer == null ? "" : defaultDirContainer.toString());
		defaultDirLabel.setEnabled(defaultDir.getSelection());
		otherDirLocation.setEnabled(otherDir.getSelection());
		browseWorkspace.setEnabled(otherDir.getSelection());
		browseFilesystem.setEnabled(otherDir.getSelection());
		variables.setEnabled(otherDir.getSelection());

	}

}
