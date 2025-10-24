package uk.co.bithatch.squashzx.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class CompressFileWizardPage extends AbstractZX0WizardPage {
	private Button quickMode;
	private Spinner skipBytes;
	private Spinner threads;

	protected CompressFileWizardPage(String pageName) {
		super(pageName, "Compressed", "Decompressed");
		setTitle("Compress File");
		setDescription("Compresses a file with the ZX Spectrum friendly ZX0 algorithm.");
	}

	@Override
	public int getThreads() {
		return threads.getSelection();
	}

	@Override
	public int getSkip() {
		return skipBytes.getSelection();
	}

	@Override
	protected void onCreateControl(Composite parent, SelectionAdapter stateUpdate) {
        quickMode = new Button(parent, SWT.CHECK);
        quickMode.setText("Quick Mode");
        quickMode.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        
		quickMode.addSelectionListener(stateUpdate);
		
        var skipLabel = new Label(parent, SWT.NONE);
        skipLabel.setText("Skip Bytes:");
        skipBytes = new Spinner(parent, SWT.BOLD);
        skipBytes.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
        skipBytes.setValues(0, 0, Integer.MAX_VALUE, 0, 8, 256);
        
        var threadsLabel = new Label(parent, SWT.NONE);
        threadsLabel.setText("Threads:");
        threads = new Spinner(parent, SWT.BOLD);
        threads.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
        threads.setValues(Math.max(1,  Runtime.getRuntime().availableProcessors() / 2), 0, 256, 0, 1, 4);
	}

	@Override
	protected void checkTargetExtension(IFile targetFile) {
		if (targetFile.getName().toLowerCase().endsWith(".zx0")) {
			setVisible(nameDecoration, false);
		} else {
			nameDecoration.setImage(
					PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
			nameDecoration.setDescriptionText("Compressed file name should ideally end with .zx0.");
			setVisible(nameDecoration, true);
		}
	}

	@Override
	protected void updateDefaultName(IFile file) {
		if(destText.getText().equals("")) {
			destText.setText(file.getParent().getFullPath().toPortableString());
		}
		if(nameText.getText().equals("")) {
			nameText.setText(file.getName() + ".zx0");
		}
	}

	@Override
	public boolean isQuick() {
		return quickMode.getSelection();
	}
}
