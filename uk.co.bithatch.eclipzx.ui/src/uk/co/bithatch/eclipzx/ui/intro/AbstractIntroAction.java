package uk.co.bithatch.eclipzx.ui.intro;

import java.util.Properties;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

public abstract class AbstractIntroAction implements IIntroAction {
    @Override
    public final void run(IIntroSite site, Properties params) {

        var workbench = PlatformUI.getWorkbench();
        Shell shell = workbench.getActiveWorkbenchWindow().getShell();
        
        /* Get off the webkit thread (Linux) or it acts weird when the wizard finishes */
        shell.getDisplay().asyncExec(() -> {
        	var wizard = createWizard(workbench);

            var dialog = new WizardDialog(shell, wizard);
            int res = dialog.open();
    		if(res == WizardDialog.OK) {
    	        IIntroManager introManager = PlatformUI.getWorkbench().getIntroManager();
    	        IIntroPart introPart = introManager.getIntro();
    	        if (introPart != null) {
    	            introManager.closeIntro(introPart);
    	        }
            }	
        });

        
	}

	public abstract IWizard createWizard(IWorkbench workbench);

	
}
