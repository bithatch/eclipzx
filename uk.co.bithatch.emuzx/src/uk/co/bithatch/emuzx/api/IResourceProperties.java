package uk.co.bithatch.emuzx.api;

import org.eclipse.core.runtime.QualifiedName;

import uk.co.bithatch.bitzx.AbstractResourceProperties;
import uk.co.bithatch.emuzx.Activator;

public class IResourceProperties  extends AbstractResourceProperties {
	
	public final static QualifiedName BUILD = new QualifiedName(Activator.PLUGIN_ID, "program.build");
	public final static QualifiedName ORG_ADDRESS = new QualifiedName(Activator.PLUGIN_ID, "program.orgAddress");
	public final static QualifiedName HEAP_ADDRESS = new QualifiedName(Activator.PLUGIN_ID, "program.heapAddress");
	public final static QualifiedName HEAP_SIZE = new QualifiedName(Activator.PLUGIN_ID, "program.heapSize");

}
