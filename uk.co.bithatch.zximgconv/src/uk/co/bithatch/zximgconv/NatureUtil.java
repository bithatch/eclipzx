package uk.co.bithatch.zximgconv;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

/**
 * Utility methods for adding / removing the ZX Image Conversion nature.
 */
public final class NatureUtil {

	private NatureUtil() {}

	public static void addNature(IProject project) throws CoreException {
		if (project.hasNature(ZXImageConversionNature.NATURE_ID)) {
			return;
		}
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = ZXImageConversionNature.NATURE_ID;
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}

	public static void removeNature(IProject project) throws CoreException {
		if (!project.hasNature(ZXImageConversionNature.NATURE_ID)) {
			return;
		}
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		String[] newNatures = new String[natures.length - 1];
		int j = 0;
		for (String nature : natures) {
			if (!ZXImageConversionNature.NATURE_ID.equals(nature)) {
				newNatures[j++] = nature;
			}
		}
		description.setNatureIds(newNatures);
		project.setDescription(description, null);
	}
}
