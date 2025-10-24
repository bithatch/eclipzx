package uk.co.bithatch.zxbasic.ui.preferences;

import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.setProperty;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.fieldassist.ControlDecoration;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.zxbasic.ui.builder.ResourceProperties;
import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;


public class DiskImagePreparationOptionsPage extends PropertyPage {

	private Button includeInPreparation;
	private Label folderLabel;
	private Text folder;
	private Button flatten;
	private Text programs;
	private Button selectPrograms;
	private Button programsInThisFolder;
	private Label infoLabel;
	private Label andLabel;
	private Label programsLabel;
	private ControlDecoration programsDecoration;

	@Override
	protected Control createContents(Composite parent) {
		var composite = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.horizontalSpacing = layout.verticalSpacing = 8;
		composite.setLayout(layout);

		includeInPreparation = new Button(composite, SWT.CHECK);
		includeInPreparation.setText("Include in preparation.");
		includeInPreparation.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).create());
		includeInPreparation.addSelectionListener(SelectionListener.widgetSelectedAdapter(evt -> updateState()));

		flatten = new Button(composite, SWT.CHECK);
		flatten.setText("Flatten.");
		flatten.setToolTipText("When selected, this folders contents will be flattened into a single directory.");
		flatten.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).create());
		flatten.addSelectionListener(SelectionListener.widgetSelectedAdapter(evt -> updateState()));
        
        folderLabel = new Label(composite, SWT.NONE);
        folderLabel.setLayoutData(GridDataFactory.swtDefaults().create());
        folderLabel.setText("Folder:");

        folder = new Text(composite, SWT.NONE);
        folder.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, false).create());
        folder.setToolTipText("When empty, file will be copied to the same directory as the target. When not empty, this folder "
        		+ "will be created in the parent as the output file and this file copied into that.");
        folder.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
			}
		});
        
		programsLabel = new Label(composite, SWT.NONE);
		programsLabel.setText("Associated Programs");
		programsLabel.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).indent(0, 24).create());
		
		infoLabel = new Label(composite, SWT.WRAP);
		infoLabel.setText("When any associated program is launched, this resource will be copied to the preparation area following rules as specified above and in the launcher.");
		infoLabel.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).hint(200, 48).create());
        infoLabel.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));

		programsInThisFolder = new Button(composite, SWT.CHECK);
		programsInThisFolder.setText("Programs In This Folder.");
		programsInThisFolder.setToolTipText("Any .BAS or .ASM files in the same directory as this resource will trigger a copy of the resource.");
		programsInThisFolder.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).indent(0, 8).create());
		programsInThisFolder.addSelectionListener(SelectionListener.widgetSelectedAdapter(evt -> updateState()));
        
		andLabel = new Label(composite, SWT.NONE);
		andLabel.setText("And these paths in this project ... (one per line)");
		andLabel.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).indent(0, 8).create());
		
		programs = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		programs.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, true).indent(16, 8).create());
		programs.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateState();
			}
		});

		programsDecoration = new ControlDecoration(programs, SWT.LEFT | SWT.TOP);
		programsDecoration.setImage(PlatformUI.getWorkbench().getSharedImages()
		        .getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		programsDecoration.setDescriptionText("You do not normally need to copy programs (e.g. .bas or .asm to the preparation area)");
		programsDecoration.hide();
        
		var file = getElement().getAdapter(IResource.class);
		
		selectPrograms = new Button(composite, SWT.PUSH);
		selectPrograms.setLayoutData(GridDataFactory.fillDefaults().create());
		selectPrograms.setText("Select");
		selectPrograms.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL,  SWT.TOP).create());
		selectPrograms.addListener(SWT.Selection, e -> {
			var elementDialog = new ResourceSelectionDialog(parent.getShell(), file.getProject(),
					"Select programs that when prepared for an external emulator launch will trigger a copy of this file or folder.");

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
        

		programs.setText(String.join(System.lineSeparator(), ResourceProperties.getProperty(file, ResourceProperties.DISK_IMAGE_OTHER_TRIGGER_PROGRAMS, Collections.emptySet())));
		includeInPreparation.setSelection(ResourceProperties.getProperty(file, ResourceProperties.DISK_IMAGE_INCLUDE_IN_PREPARATION, false));
		flatten.setSelection(ResourceProperties.getProperty(file, ResourceProperties.DISK_IMAGE_FLATTEN_PREPARATION, false));
		programsInThisFolder.setSelection(ResourceProperties.getProperty(file, ResourceProperties.DISK_IMAGE_TRIGGER_PROGRAMS_IN_THIS_FOLDER, true));
		folder.setText(ResourceProperties.getProperty(file, ResourceProperties.DISK_IMAGE_PREPARATION_FOLDER, ""));
        
		updateState();

		return composite;
	}

	@Override
	public boolean performOk() {
		var file = getElement().getAdapter(IResource.class);

		setProperty(file, ResourceProperties.DISK_IMAGE_OTHER_TRIGGER_PROGRAMS, getCurrentTriggerPrograms());
		setProperty(file, ResourceProperties.DISK_IMAGE_INCLUDE_IN_PREPARATION, includeInPreparation.getSelection());
		setProperty(file, ResourceProperties.DISK_IMAGE_TRIGGER_PROGRAMS_IN_THIS_FOLDER, programsInThisFolder.getSelection());
		setProperty(file, ResourceProperties.DISK_IMAGE_FLATTEN_PREPARATION, flatten.getSelection());
		setProperty(file, ResourceProperties.DISK_IMAGE_PREPARATION_FOLDER, folder.getText());
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
		var sel = includeInPreparation.getSelection();
		folderLabel.setEnabled(sel);
		flatten.setEnabled(sel);
		programsInThisFolder.setEnabled(sel);
		selectPrograms.setEnabled(sel);
		programs.setEnabled(sel);
		programsLabel.setEnabled(sel);
		folder.setEnabled(sel);
		andLabel.setEnabled(sel);
		infoLabel.setEnabled(sel);

		var project = getElement().getAdapter(IResource.class).getProject();
		var triggerPrograms = getCurrentTriggerPrograms();
		var nonProgramSources = 0;
		var missingTriggerPrograms = 0;
		for(var program : triggerPrograms) {
			if(!project.getFile(program).exists()) {
				missingTriggerPrograms++;
			}
			if(!FileNames.hasExtensions(program, ZXBasicBuilder.EXTENSIONS)) 
				nonProgramSources++;
			
		}
		if(missingTriggerPrograms > 0) {
			programsDecoration.setImage(PlatformUI.getWorkbench().getSharedImages()
			        .getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
			programsDecoration.setDescriptionText(MessageFormat.format("{0} of these paths do not exist.", missingTriggerPrograms));
			programsDecoration.show();
		}
		else if(nonProgramSources > 0) {
			programsDecoration.setImage(PlatformUI.getWorkbench().getSharedImages()
			        .getImage(ISharedImages.IMG_OBJS_WARN_TSK));
			programsDecoration.setDescriptionText(MessageFormat.format("{0} of these paths dot not appear to be programs, they usually end in .bas or .asm.", nonProgramSources));
			programsDecoration.show();
		}
		else {
			programsDecoration.hide();
		}
	}
}
