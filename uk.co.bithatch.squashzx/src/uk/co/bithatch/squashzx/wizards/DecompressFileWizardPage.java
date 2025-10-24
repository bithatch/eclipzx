package uk.co.bithatch.squashzx.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class DecompressFileWizardPage extends AbstractZX0WizardPage {

	public DecompressFileWizardPage(String pageName) {
		super(pageName, "Decompressed", "Compressed");
		setTitle("Decompress File");
		setDescription("Decompresses a file with the ZX Spectrum friendly ZX0 algorithm.");
	}

	@Override
	protected void checkTargetExtension(IFile targetFile) {
		if (!targetFile.getName().toLowerCase().endsWith(".zx0")) {
			setVisible(nameDecoration, false);
		} else {
			nameDecoration.setImage(
					PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
			nameDecoration.setDescriptionText("Uncompressed file name should not end with .zx0.");
			setVisible(nameDecoration, true);
		}
	}

	@Override
	protected void onCreateControl(Composite parent, SelectionAdapter onSelection) {
		super.onCreateControl(parent, onSelection);
		removeSource.setSelection(true);
	}

	@Override
	protected void updateDefaultName(IFile file) {
		if(destText.getText().equals("")) {
			destText.setText(file.getParent().getFullPath().toPortableString());
		}
		if(nameText.getText().equals("")) {
			if(file.getFileExtension().equalsIgnoreCase("zx0")) {
				nameText.setText(stripExtension(file.getName()));
			}
			else {
				nameText.setText(file.getName() + ".uncompressed");
			}
		}
	}

	@Override
	public boolean isQuick() {
		return false;
	}

}
