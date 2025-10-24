package uk.co.bithatch.zxbasic;

import java.nio.file.Path;

import org.eclipse.emf.ecore.resource.Resource;

public interface IIncludeSource {

	Path find(Resource resource, String importURI);

}
