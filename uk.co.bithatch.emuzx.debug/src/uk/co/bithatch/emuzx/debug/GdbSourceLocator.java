package uk.co.bithatch.emuzx.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;

/**
 * Simple source locator for the GDB RSP debugger.
 * Maps stack frame source names (e.g., "main.c") to workspace {@link IFile}s
 * by searching all projects in the workspace.
 */
public class GdbSourceLocator implements ISourceLocator {

	private static final ILog LOG = ILog.of(GdbSourceLocator.class);

	@Override
	public Object getSourceElement(IStackFrame stackFrame) {
		if (stackFrame instanceof GdbZ80StackFrame gdbFrame) {
			var sourceName = gdbFrame.getSourceName();
			if (sourceName != null) {
				return findFile(sourceName);
			}
		}
		return null;
	}

	/**
	 * Search all open projects for a file matching the given name.
	 */
	private IFile findFile(String name) {
		var workspace = ResourcesPlugin.getWorkspace().getRoot();
		for (var project : workspace.getProjects()) {
			if (!project.isOpen()) continue;
			var file = findFileInContainer(project, name);
			if (file != null) {
				LOG.info("Source locator: " + name + " → " + file.getFullPath());
				return file;
			}
		}
		LOG.warn("Source locator: could not find " + name + " in workspace");
		return null;
	}

	private IFile findFileInContainer(org.eclipse.core.resources.IContainer container, String name) {
		try {
			for (var member : container.members()) {
				if (member instanceof IFile file) {
					if (file.getName().equalsIgnoreCase(name)) {
						return file;
					}
				} else if (member instanceof org.eclipse.core.resources.IContainer sub) {
					/* Skip build output directories */
					var subName = sub.getName();
					if (subName.equals("Debug") || subName.equals("Release") || subName.startsWith(".")) {
						continue;
					}
					var found = findFileInContainer(sub, name);
					if (found != null) return found;
				}
			}
		} catch (Exception e) {
			/* Ignore — project might be closing */
		}
		return null;
	}
}
