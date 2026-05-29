package uk.co.bithatch.eclipz80.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import uk.co.bithatch.eclipz80.ui.builder.AsmBuilder;
import uk.co.bithatch.eclipz80.ui.builder.NaturePromptUtil;

public class AsmResourceListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			IResourceDelta rootDelta = event.getDelta();
			if (rootDelta != null) {
				rootDelta.accept(delta -> {
					if (delta.getKind() == IResourceDelta.ADDED) {
						IResource res = delta.getResource();
						if (res instanceof IFile file && hasAsmExtension(file.getName())) {
							NaturePromptUtil.maybePromptToEnableNature(file.getProject());
						}
					}
					return true;
				});
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static boolean hasAsmExtension(String name) {
		if (name == null) return false;
		String lower = name.toLowerCase();
		for (String ext : AsmBuilder.EXTENSIONS) {
			if (lower.endsWith("." + ext)) {
				return true;
			}
		}
		return false;
	}
}
