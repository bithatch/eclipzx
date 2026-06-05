package uk.co.bithatch.eclipzoxo.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.zoxo.dezog.DeZogSettings;

public class DeZogPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Spinner port;

    public DeZogPreferencePage() {
        super("Debugger");
        setDescription("DeZog Debugger Options.");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        var container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(3, false);
        layout.verticalSpacing = 12;
        layout.marginTop = 24;
		container.setLayout(layout);

        var frequencyLabel = new Label(container, SWT.NONE);
        frequencyLabel.setText("Port:");
        port = new Spinner(container, SWT.NONE);
        port.setValues(ZoxoPreferencesAccess.get().settings(DeZogSettings.class).getPort(), 1, 65535, 0, 1, 1);
        port.setLayoutData(GridDataFactory.defaultsFor(port).span(2, 0).create());

        return container;
    }

    @Override
    public boolean performOk() {
        var pax = ZoxoPreferencesAccess.get();
        pax.getPreferenceStore().putValue(DeZogSettingsImpl.PORT, String.valueOf(port.getSelection()));
        return true;
    }

    @Override
    protected void performDefaults() {
    	var settings = ZoxoPreferencesAccess.get().settings(DeZogSettings.class);
    	port.setSelection(settings.getPort());
        super.performDefaults();
    }
}
