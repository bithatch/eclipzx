package uk.co.bithatch.nextbuild;

import java.net.MalformedURLException;
import java.net.URI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;
import uk.co.bithatch.zxbasic.ui.library.ZXSDK;
import uk.co.bithatch.zxbasic.ui.wizard.AbstractBasicProjectWizardPage;

public class NextBuildExamplesWizardPage extends AbstractBasicProjectWizardPage {

    Combo sdk;

	public NextBuildExamplesWizardPage() {
        super("ZX BASIC NextBuild Examples Project");
        setTitle("ZX BASIC NextBuild Examples Project");
        setDescription("Create a new ZX BASIC project for the ZX Next, with the  examples from NextBuild.");
        
//        The project will  be configured to "
//        		+ "output as a NEX file, and use NextLib. The built-in emulator "
//        		+ "may not currently be used with this, so you'll need setup an External Emulator launch.");
    }

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		setProject("next-build-examples");
		dialogChanged();
	}

	@Override
	protected void createFields(Composite container) {
		
        var label = new Label(container, SWT.NONE);
        label.setText("SDK:");

        sdk = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        sdk.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sdk.setItems(ContributedSDKRegistry.getAllSDKs().stream().map(ZXSDK::name).toList().toArray(new String[0]));
		sdk.select(0);
		
		var info = new Label(container, SWT.WRAP);
		info.setImage(PlatformUI.getWorkbench()
			    .getSharedImages()
			    .getImage(ISharedImages.IMG_TOOL_DELETE));
		info.setText("The project will be configured to output as a NEX file, and use NextLib. "
				+ "The built-in emulator may not currently be used with this, so you'll need setup "
				+ "an External Emulator launch. It also enabled the `Next Build Nature`, that "
				+ "adds support for NextBuilds source header processing instructions. You can "
				+ "add this to your own project too if you want (see Project -> Natures)");
        GridDataFactory.defaultsFor(info).span(2, 1).indent(0, 32).applyTo(info);
		
        var link = new Link(container, SWT.NONE);
        GridDataFactory.generate(link, 2, 1);
        link.setText("<a>The awesome NextBuild IDE</a>");
        link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(URI.create("https://github.com/em00k/NextBuild/").toURL());
				} catch (PartInitException | MalformedURLException e1) {
				}
            }
        });
		
	}
}
