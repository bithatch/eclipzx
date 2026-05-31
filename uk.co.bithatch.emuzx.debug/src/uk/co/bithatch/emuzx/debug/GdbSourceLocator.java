package uk.co.bithatch.emuzx.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.ISourcePresentation;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Simple source locator for the GDB RSP debugger.
 * Maps stack frame source names (e.g., "main.asm") to workspace {@link IFile}s
 * by searching all projects in the workspace.
 * <p>
 * Also implements {@link ISourcePresentation} to ensure the correct editor
 * (e.g., the Xtext Asm editor) is used when opening source files during
 * debugging, rather than a generic text editor.
 */
public class GdbSourceLocator implements ISourceLocator, ISourcePresentation {

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

	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof IFile file) {
			return new FileEditorInput(file);
		}
		return null;
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		if (element instanceof IFile file) {
			var name = file.getName().toLowerCase();
			if (name.endsWith(".asm") || name.endsWith(".s") || name.endsWith(".z80")) {
				return "uk.co.bithatch.eclipz80.Asm";
			}
		}
		return "org.eclipse.ui.DefaultTextEditor";
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
