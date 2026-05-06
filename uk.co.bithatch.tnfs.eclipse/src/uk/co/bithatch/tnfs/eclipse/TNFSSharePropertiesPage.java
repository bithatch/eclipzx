package uk.co.bithatch.tnfs.eclipse;

import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.jface.resource.JFaceResources.getFontRegistry;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;


public class TNFSSharePropertiesPage extends PropertyPage {

    private Button sharedButton;
    private Text mountPath;

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = layout.verticalSpacing = 8;
		composite.setLayout(layout);

		var infoLabel = new Label(composite, SWT.WRAP);
		infoLabel.setText("You can share this folder to any device capable of making of a TNFS connection, such as");
		infoLabel.setLayoutData(swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(4, 1).hint(200, SWT.DEFAULT).create());
        infoLabel.setFont(getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));

        sharedButton = new Button(composite, SWT.CHECK);
        sharedButton.setText("Shared");
        sharedButton.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(4, 1).indent(0, 8).create());

		var mountPathLabel = new Label(composite, SWT.WRAP);
		mountPathLabel.setText("Mount Path");
        
        mountPath = new Text(composite, SWT.CHECK);
        mountPath.setText("/");
        mountPath.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).indent(0, 8).create());
        
        IResource resource = (IResource) getElement().getAdapter(IResource.class);
        IResource parentResource = resource;
        
        boolean parentShared = false;
        String parentMountPath = null;
        while(parentResource != null && !parentShared) {        	
        	parentResource = parentResource.getParent();
        	if(parentResource != null) {
        		parentShared = TNFSResourceProperties.getProperty(parentResource, TNFSResourceProperties.SHARED, false);
        		parentMountPath = TNFSResourceProperties.getProperty(parentResource, TNFSResourceProperties.MOUNT_PATH, parentResource.getFullPath().toString()); 
        	}
        }
        
        if(parentShared) {
        	mountPath.setEnabled(false);
        	mountPath.setText(parentMountPath);
        	sharedButton.setSelection(true);
        	sharedButton.setEnabled(false);
        	mountPath.setToolTipText("Shared by parent folder");
        	sharedButton.setToolTipText("Shared by parent folder");
        }
        else {
        	if(resource instanceof IFolder || resource instanceof IProject) {
		        sharedButton.setSelection(TNFSResourceProperties.getProperty(resource, TNFSResourceProperties.SHARED, false));
		        mountPath.setText(TNFSResourceProperties.getProperty(resource, TNFSResourceProperties.MOUNT_PATH, resource.getFullPath().toString()));
		        
		        sharedButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						mountPath.setEnabled(sharedButton.getSelection());
					}
				});
            	mountPath.setEnabled(sharedButton.getSelection());
        	}
        	else {
            	mountPath.setToolTipText("Only folders or projects may be shared");
            	sharedButton.setToolTipText("Only folders or projects may be shared");
        		sharedButton.setEnabled(false);
            	sharedButton.setSelection(false);
            	mountPath.setEnabled(false);
            	mountPath.setText("");
        	}
        }

        return composite;
    }

    @Override
	protected void performDefaults() {
		super.performDefaults();
		if(sharedButton.getEnabled()) {
			sharedButton.setSelection(false);
		}
		if(mountPath.getEnabled()) {
	        IResource resource = (IResource) getElement().getAdapter(IResource.class);
			mountPath.setText(resource == null ? "" : resource.getFullPath().toString());
		}
	}

	@Override
    public boolean performOk() {
        IResource resource = (IResource) getElement().getAdapter(IResource.class);
        if (resource != null) {
        	TNFSResourceProperties.setProperty(resource, TNFSResourceProperties.SHARED, sharedButton.getSelection());
        	TNFSResourceProperties.setProperty(resource, TNFSResourceProperties.MOUNT_PATH, mountPath.getText());
        	try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, false, new  ScanSharedFoldersOperation());
			} catch (InvocationTargetException  |  InterruptedException e) {
				throw new IllegalStateException(e);
			}
        }
        return true;
    }
}
