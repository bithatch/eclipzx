package uk.co.bithatch.zxbasic.ui.preferences;

import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.jface.resource.JFaceResources.getFontRegistry;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.setProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;

import uk.co.bithatch.zxbasic.ui.builder.ResourceProperties;
import uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.Listener;


public class NEXBundlingTriggerOptionsPage extends PropertyPage implements Listener {

	private Text programs;
	private Button selectPrograms;
	private Button programsInThisFolder;
	private Label infoLabel;
	private Label andLabel;
	@Override
	protected Control createContents(Composite parent) {
		
		var composite = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = layout.verticalSpacing = 8;
		composite.setLayout(layout);

		infoLabel = new Label(composite, SWT.WRAP);
		infoLabel.setText("When any associated program is built as a NEX (for the ZX Next), this resource will be bundled into the final artifact.");
		infoLabel.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(4, 1).hint(200, SWT.DEFAULT).create());
        infoLabel.setFont(getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));

		programsInThisFolder = new Button(composite, SWT.CHECK);
		programsInThisFolder.setText("Programs In This Folder.");
		programsInThisFolder.setToolTipText("Any .BAS or .ASM files in the same directory as this resource will trigger a copy of the resource.");
		programsInThisFolder.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(4, 1).indent(0, 8).create());
		programsInThisFolder.addSelectionListener(SelectionListener.widgetSelectedAdapter(evt -> updateState()));
        
		andLabel = new Label(composite, SWT.NONE);
		andLabel.setText("And these paths in this project ... (one per line)");
		andLabel.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(4, 1).indent(0, 8).create());
		
		programs = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		programs.setLayoutData(fillDefaults().span(3, 1).grab(true, true).indent(16, 8).create());
		programs.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateState();
			}
		});

		var file = getElement().getAdapter(IResource.class);
		
		selectPrograms = new Button(composite, SWT.PUSH);
		selectPrograms.setText("Select");
		selectPrograms.setLayoutData(swtDefaults().align(SWT.FILL,  SWT.TOP).grab(true, false).hint(32, SWT.DEFAULT).create());
		selectPrograms.addListener(SWT.Selection, e -> {
			var elementDialog = new ResourceSelectionDialog(parent.getShell(), file.getProject(),
					"Select programs that when build will trigger bundling of this resource.");

			if (elementDialog.open() == Window.OK) {
				var result = elementDialog.getResult();

					if (result != null) {
						var finalList = new LinkedHashSet<>(Arrays.asList((Object[]) result).
							stream().
							map(o -> ((IResource)o).getProjectRelativePath().toString()).
							toList());
						finalList.addAll(getCurrentTriggerPrograms());
						programs.setText(String.join(System.lineSeparator(), finalList));
						
//					addOtherFiles(Arrays.asList((Object[]) result).stream().map(o -> ((IResource)o)).toList());
				}
			}
		});
        

		programs.setText(String.join(System.lineSeparator(), ResourceProperties.getProperty(file, ResourceProperties.NEX_OTHER_TRIGGER_PROGRAMS, Collections.emptySet())));
		programsInThisFolder.setSelection(ResourceProperties.getProperty(file, ResourceProperties.NEX_TRIGGER_PROGRAMS_IN_THIS_FOLDER, true));
        
		updateState();

		ResourceProperties.addListener(this);

		return composite;
	}

	@Override
	public void propertyChanged(QualifiedName name, String oldVal, String newVal) {
		updateState();
	}

	@Override
	public void dispose() {
		super.dispose();
		ResourceProperties.removeListener(this);
	}

	@Override
	protected void updateApplyButton() {
		super.updateApplyButton();
		updateState();
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		programs.setText("");
		programsInThisFolder.setSelection(true);
	}

	@Override
	public boolean performOk() {
		var file = getElement().getAdapter(IResource.class);

		setProperty(file, ResourceProperties.NEX_OTHER_TRIGGER_PROGRAMS, getCurrentTriggerPrograms());
		setProperty(file, ResourceProperties.NEX_TRIGGER_PROGRAMS_IN_THIS_FOLDER, programsInThisFolder.getSelection());
		return true;
	}

	private List<String> getCurrentTriggerPrograms() {
		return Arrays.asList(programs.getText().equals("")
				? new String[0] 
						: programs.getText().split(System.lineSeparator())).
							stream().
							map(String::trim).
							toList();
	}

	private void updateState() {
//		var sel = includeInNEX.getSelection();
		var file = getElement().getAdapter(IResource.class);
		var sel = ResourceProperties.getProperty(file, ResourceProperties.NEX_BUNDLE, false); // TODO
		programsInThisFolder.setEnabled(sel);
		selectPrograms.setEnabled(sel);
		programs.setEnabled(sel);
		andLabel.setEnabled(sel);
		infoLabel.setEnabled(sel);
		
	}
}
