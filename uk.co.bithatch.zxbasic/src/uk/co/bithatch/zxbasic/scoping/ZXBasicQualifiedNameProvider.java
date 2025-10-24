package uk.co.bithatch.zxbasic.scoping;

import static org.eclipse.xtext.naming.QualifiedName.create;
import static uk.co.bithatch.zxbasic.scoping.ScopingUtils.normalizeIdentifier;
import static uk.co.bithatch.zxbasic.scoping.ScopingUtils.stripLabelSuffix;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;

import com.google.inject.Inject;

import uk.co.bithatch.zxbasic.ILanguageSettings;
import uk.co.bithatch.zxbasic.basic.Group;
import uk.co.bithatch.zxbasic.basic.Referable;

public class ZXBasicQualifiedNameProvider extends IQualifiedNameProvider.AbstractImpl {
	@Inject
	private ILanguageSettings languageSettings;

	@Override
	public QualifiedName getFullyQualifiedName(EObject obj) {
		if (obj instanceof Group grp && grp.getName() != null) {
			var name = grp.getName();
			if (ScopingUtils.isLabel(name)) {
				return create(stripLabelSuffix(name));
			} else {
				return create(name);
			}
		} else if (obj instanceof Referable element) {
			if (element.getName() != null) {
				return create(normalizeIdentifier(element.getName(), languageSettings, element));
			}
		}
		return null;
	}

}
