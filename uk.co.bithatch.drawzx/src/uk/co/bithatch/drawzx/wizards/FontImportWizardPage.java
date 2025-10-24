package uk.co.bithatch.drawzx.wizards;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import uk.co.bithatch.drawzx.widgets.ImageSelectionComposite;
import uk.co.bithatch.drawzx.widgets.ImageSelectionComposite.ImageEntry;

public class FontImportWizardPage extends WizardPage {

	private ImageSelectionComposite selector;

	protected FontImportWizardPage(String pageName) {
		super(pageName);
		setTitle("Choose Font");
		setDescription("Choose from one of the bundled fonts.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		selector = new ImageSelectionComposite(container, SWT.NONE, "fonts/");
		selector.setLayoutData(
				GridDataFactory.swtDefaults().grab(true, true).hint(600, 500).align(SWT.CENTER, SWT.CENTER).create());
		selector.addSelectionListener(SelectionListener.widgetSelectedAdapter(evt -> setPageComplete(getSource() != null)));

		setControl(container);
		setPageComplete(false);
	}

	public ImageEntry getSource() {
		return selector.getSelectedImageEntry();
	}

}
