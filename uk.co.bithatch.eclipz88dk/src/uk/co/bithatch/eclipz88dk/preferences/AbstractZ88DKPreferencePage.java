package uk.co.bithatch.eclipz88dk.preferences;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;

import uk.co.bithatch.widgetzx.preferences.AbstractProjectSpecificPreferencePage;

public abstract class AbstractZ88DKPreferencePage extends AbstractProjectSpecificPreferencePage
		implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

	public AbstractZ88DKPreferencePage(String pssCategory) {
		super(Z88DKPreferencesAccess.get(), pssCategory);
	}

	public AbstractZ88DKPreferencePage(String pssCategory, int style) {
		super(Z88DKPreferencesAccess.get(), pssCategory, style);
	}

	public AbstractZ88DKPreferencePage(String pssCategory, String title, ImageDescriptor image, int style) {
		super(Z88DKPreferencesAccess.get(), pssCategory, title, image, style);
	}

	public AbstractZ88DKPreferencePage(String pssCategory, String title, int style) {
		super(Z88DKPreferencesAccess.get(), pssCategory, title, style);
	}

}
