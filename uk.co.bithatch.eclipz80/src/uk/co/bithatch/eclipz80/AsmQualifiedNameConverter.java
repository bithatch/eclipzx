package uk.co.bithatch.eclipz80;

import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;

/**
 * Normalizes assembler symbols so an optional leading dot does not affect linking.
 */
public class AsmQualifiedNameConverter extends IQualifiedNameConverter.DefaultImpl {

	@Override
	public QualifiedName toQualifiedName(String qualifiedNameAsText) {
		if (qualifiedNameAsText == null) {
			return super.toQualifiedName(null);
		}
		return super.toQualifiedName(stripLeadingDot(qualifiedNameAsText));
	}

	static String stripLeadingDot(String name) {
		if (name != null && name.startsWith(".")) {
			return name.substring(1);
		}
		return name;
	}
}
