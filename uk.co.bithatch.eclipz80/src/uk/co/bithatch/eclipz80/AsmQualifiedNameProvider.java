package uk.co.bithatch.eclipz80;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;

import uk.co.bithatch.eclipz80.asm.AsmLabelDef;

/**
 * Custom qualified name provider that strips leading dots from label names.
 * <p>
 * In z88dk assembly, labels can be defined with a leading dot (e.g.
 * {@code .my_label:}) but referenced without it ({@code JP my_label}).
 * The dot is a z88dk convention indicating the token is a label definition,
 * not part of the label's actual name. This provider normalises such names
 * so that Xtext's cross-reference resolution treats {@code .foo} and
 * {@code foo} as the same symbol.
 */
public class AsmQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {

	@Override
	public QualifiedName getFullyQualifiedName(EObject obj) {
		if (obj instanceof AsmLabelDef) {
			String name = ((AsmLabelDef) obj).getName();
			if (name != null) {
				name = stripLeadingDot(name);
				return QualifiedName.create(name);
			}
		}
		return super.getFullyQualifiedName(obj);
	}

	/**
	 * Strip a single leading dot from a name, if present.
	 * This normalises z88dk-style {@code .label} definitions to bare
	 * {@code label} names.
	 */
	static String stripLeadingDot(String name) {
		if (name != null && name.startsWith(".")) {
			return name.substring(1);
		}
		return name;
	}
}
