package uk.co.bithatch.tnfs.eclipse;

import org.eclipse.core.runtime.QualifiedName;

import uk.co.bithatch.bitzx.AbstractResourceProperties;

public class TNFSResourceProperties extends AbstractResourceProperties {

    public final static QualifiedName SHARED = new QualifiedName(TNFSActivator.PLUGIN_ID, "shared");
    public final static QualifiedName MOUNT_PATH = new QualifiedName(TNFSActivator.PLUGIN_ID, "mountPath");
	
}
