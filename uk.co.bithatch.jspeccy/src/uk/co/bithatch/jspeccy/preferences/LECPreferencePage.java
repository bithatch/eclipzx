package uk.co.bithatch.jspeccy.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import uk.co.bithatch.jspeccy.Activator;

public class LECPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Button lecEnabled;

    public LECPreferencePage() {
        super("LEC");
        setDescription("LEC options.");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        var layout = new GridLayout(1, false);
        layout.verticalSpacing = 12;
        layout.marginTop = 24;
        container.setLayout(layout);
        
        var info = new Label(container, SWT.NONE);
        info.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));
        info.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).create());
        info.setText("""
        		The LEC is a Spectrum 48k memory extension designed in 1987 by 
        		Jiri Lamac, providing 528k of RAM.
        		
        		Jiri wrote his own ROM called LEC ROM. It fixes some well known bugs
        		of the original ROM, delivers the better de-tokenized basic editor
        		and  a few commands for copying data between RAM banks. But it is 
        		just optional, CP/M and custom format programs works with the orginal
        		ROM as well.
        		
        		Jiri's CP/M v2.2 can handle the Microdrive units for storage, using
        		his own Microdrive format. Search for LEC at WoS.
        		""");

        lecEnabled = new Button(container, SWT.CHECK);
        lecEnabled.setText("Connected");
        lecEnabled.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        
        lecEnabled.setSelection(Activator.getDefault().settings().jspeccy().getSpectrumSettings().isLecEnabled());

        return container;
    }

    @Override
    public boolean performOk() {
        var settings = Activator.getDefault().settings();
		var jspeccySettings = settings.jspeccy();
        jspeccySettings.getSpectrumSettings().setLecEnabled(lecEnabled.getSelection());
        settings.save();
        return true;
    }

    @Override
    protected void performDefaults() {
        lecEnabled.setSelection(false);
        super.performDefaults();
    }
}
