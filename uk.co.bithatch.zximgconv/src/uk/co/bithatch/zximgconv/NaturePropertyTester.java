package uk.co.bithatch.zximgconv;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Tests whether a project has the ZX Image Conversion nature.
 * Used in visibleWhen expressions in plugin.xml.
 */
public class NaturePropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if ("hasNature".equals(property) && receiver instanceof IProject project) {
			try {
				return project.isOpen() && project.hasNature(ZXImageConversionNature.NATURE_ID);
			} catch (CoreException e) {
				return false;
			}
		}
		return false;
	}
}
