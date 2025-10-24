package uk.co.bithatch.zxbasic.scoping;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;

import com.google.inject.Inject;

import uk.co.bithatch.zxbasic.IIncludeSource;
import uk.co.bithatch.zxbasic.basic.PPInclude;


public class ZXBasicUriResolver  extends ImportUriResolver {

	@Inject(optional = true)
	private IIncludeSource includeSource;
	
	
	@Inject
	public ZXBasicUriResolver() {
	}

	@Override
	public String resolve(EObject object) {
		if (object instanceof PPInclude ppinclude) {
			var importURI = ppinclude.getImportURI();
			if(importURI.startsWith("\"") && importURI.endsWith("\"")) {
				/* Relative to this resource */
				importURI = importURI.substring(1, importURI.length() - 1);
				var res = object.eResource();
				if(res != null) {
					var uri = URI.createFileURI(importURI).resolve(res.getURI()).toString();
					return uri;
				}
				else {
					return super.resolve(object);
				}
			}
			else if(includeSource != null && importURI.startsWith("<") && importURI.endsWith(">")) {
				importURI = importURI.substring(1, importURI.length() - 1);
				var path = includeSource.find(object.eResource(), importURI);
				if(path != null && path.toFile().exists()) {
					return path.toUri().toString();
				}
			}
		}

		return super.resolve(object);
	}
}
