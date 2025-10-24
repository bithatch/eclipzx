package uk.co.bithatch.eclipz88dk.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;

public class Z88DKPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

	public Z88DKPreferencePage() {
		super("Z88DK");
		setPreferenceStore(Z88DKPreferencesAccess.get().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public IAdaptable getElement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setElement(IAdaptable element) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Control createContents(Composite parent) {

		var lbl = new Label(parent, SWT.WRAP);
		lbl.setText("""
				Z88DK is a collection of software development tools that targets the 8080 
				and z80 family of machines. It allows development of programs in C, 
				assembly language or any mixture of the two. What makes z88dk unique is 
				its ease of use, built-in support for many z80 machines and its extensive 
				set of assembly language library subroutines implementing the C standard 
				and extensions.
				
				This Eclipse extension focuses on Z88DK as its used for the ZX Spectrum.
				Other targets may follow if there is interest.
				""");
		
		return lbl;
	}
}
