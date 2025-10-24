package uk.co.bithatch.eclipzx.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.exbin.bined.eclipse.plugin.editors.BinEdEditor;

public class OpenInHEXEditorHandler extends AbstractHandler {
	public final static String ID = "org.exbin.bined.eclipse.plugin.editors.BinEdEditor";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var sel = HandlerUtil.getCurrentSelection(event);
		if (sel != null && sel instanceof IStructuredSelection sssl) {
			if (sssl.getFirstElement() instanceof IFile file) {
				try {
					var editor = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file,
							ID, true);
					
					if(editor == null) {
						throw new IllegalStateException("Could not find editor.");
					}
					else if(!(editor instanceof BinEdEditor)) {
						throw new IllegalStateException("Another editor is already working on this file.");
					}
					
				} catch (PartInitException e) {
					throw new ExecutionException("Failed to start HEX editor.", e);
				}
			}
		}
		return null;
	}

}
