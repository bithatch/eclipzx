package uk.co.bithatch.ayzxfx.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage; 

public class NewAFBFileWizardPage extends WizardNewFileCreationPage {

    public NewAFBFileWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, selection);
        setTitle("New AFB File");
        // https://shiru.untergrund.net/software.shtml
        setDescription("Create a AYFX Bank File. Can contain multiple AYFX (.afx) effect definitions.");
    }

    public void setFileExtension(String ext) {
        setFileName("effects." + ext);
    }

//    @Override
//    protected InputStream getInitialContents() {
//    	var afb = AFB.create();
//    	afb.add(AFX.named("Effect 1"));
//    	var out = new ByteArrayOutputStream();
//    	try {
//			afb.save(Channels.newChannel(out));
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//        return new ByteArrayInputStream(out.toByteArray());
//    }
    
}
