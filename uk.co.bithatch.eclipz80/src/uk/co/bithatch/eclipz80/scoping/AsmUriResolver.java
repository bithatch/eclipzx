//package uk.co.bithatch.eclipz80.scoping;
//
//import org.eclipse.emf.common.util.URI;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.xtext.scoping.impl.ImportUriResolver;
//
//import com.google.inject.Inject;
//
//import uk.co.bithatch.eclipz80.IAsmIncludeSource;
//import uk.co.bithatch.eclipz80.asm.AsmInclude;
//
//
///**
// * TODO share code with ZXBasicUriResolver
// */
//public class AsmUriResolver  extends ImportUriResolver {
//
//	@Inject(optional = true)
//	private IAsmIncludeSource includeSource;
//	
//	
//	@Inject
//	public AsmUriResolver() {
//	}
//
//	@Override
//	public String resolve(EObject object) {
//		if (object instanceof AsmInclude ppinclude) {
//			var importURI = ppinclude.getImportURI();
//			if(importURI.startsWith("\"") && importURI.endsWith("\"")) {
//				/* Relative to this resource */
//				importURI = importURI.substring(1, importURI.length() - 1);
//				var res = object.eResource();
//				if(res != null) {
//					var uri = URI.createFileURI(importURI).resolve(res.getURI()).toString();
//					return uri;
//				}
//				else {
//					return super.resolve(object);
//				}
//			}
//			else if(includeSource != null && importURI.startsWith("<") && importURI.endsWith(">")) {
//				importURI = importURI.substring(1, importURI.length() - 1);
//				var path = includeSource.find(object.eResource(), importURI);
//				if(path != null && path.toFile().exists()) {
//					return path.toUri().toString();
//				}
//			}
//		}
//
//		return super.resolve(object);
//	}
//}



package uk.co.bithatch.eclipz80.scoping;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;

import uk.co.bithatch.eclipz80.asm.AsmInclude;

public class AsmUriResolver extends ImportUriResolver {

	@Override
	public String resolve(EObject object) {
		String importURI = null;

		if (object instanceof AsmInclude) {
			importURI = ((AsmInclude) object).getImportURI();
		}

		if (importURI != null) {
			// Strip surrounding quotes if present
			importURI = stripQuotes(importURI);

			// Resolve relative to the containing resource
			if (object.eResource() != null) {
				URI baseURI = object.eResource().getURI();
				URI resolved = URI.createFileURI(importURI).resolve(baseURI);
				return resolved.toString();
			}
			return importURI;
		}

		return super.resolve(object);
	}

	private String stripQuotes(String s) {
		if (s != null && s.length() >= 2) {
			char first = s.charAt(0);
			char last = s.charAt(s.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				return s.substring(1, s.length() - 1);
			}
		}
		return s;
	}
}