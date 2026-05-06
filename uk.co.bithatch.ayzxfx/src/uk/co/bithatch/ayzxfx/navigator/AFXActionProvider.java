package uk.co.bithatch.ayzxfx.navigator;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import uk.co.bithatch.ayzxfx.ay.AFX;
import uk.co.bithatch.ayzxfx.ay.NamedAFX;
import uk.co.bithatch.ayzxfx.editor.AFBEditor;
import uk.co.bithatch.ayzxfx.editor.AFXTransfer;

public class AFXActionProvider extends CommonActionProvider {
	
	
	public final static class AFXAction extends Action {
		
		private final Consumer<List<AFXNode>> onAction;
		private final ICommonActionExtensionSite site;
		private final boolean multi; 

		protected AFXAction(boolean multi, String id, String img, String label, ICommonActionExtensionSite site, Consumer<List<AFXNode>> onAction) {
			super(label);
			this.multi = multi;
			this.onAction = onAction;
			this.site = site;
			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(id));
	        setId(id);
		}

        @Override
		public boolean isEnabled() {
        	var sz = getSelectedNodes().size(); 
			return sz == 1 && !multi || multi;
		}

		public void run() {
            var selnodes = getSelectedNodes();
            if (!selnodes.isEmpty()) {
            	onAction.accept(selnodes);
            }
        }

		private List<AFXNode> getSelectedNodes() {
			var selection = (IStructuredSelection) site.getStructuredViewer().getSelection();
            var selnodes = selection.stream().filter(o -> o instanceof AFXNode).map(o -> (AFXNode)o).toList();
			return selnodes;
		}
	}

	private IAction openAction;
	private IAction deleteAction;
	private IAction renameAction;
	private IAction copyAction;
	private IAction pasteAction;

    @Override
    public void init(ICommonActionExtensionSite site) {
    	
    	openAction = new AFXAction(false, "openEffect", null, "Open Effect", site, afxNodes -> {
    		var fx = afxNodes.get(0);
    	    var editor = AFBEditor.findOpenAFBEditorFor(fx.getFile());
    	    if (editor == null) {
    	        // Open the editor for the file
    	        try {
    	            var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    	            var part = org.eclipse.ui.ide.IDE.openEditor(page, fx.getFile());
    	            if (part instanceof AFBEditor afbEditor) {
    	                editor = afbEditor;
    	                var index = fx.getAfb().indexOf(fx.getAfx());
    	    	        editor.afx(editor.afb().get(index));
    	            }
    	        } catch (org.eclipse.ui.PartInitException e) {
    	            e.printStackTrace();
    	        }
    	    }
    	    else {
    	        editor.afx(fx.getAfx());
    	    }
    	});    	
    	
    	renameAction = new AFXAction(false, "renameEffect", null, "Rename Effect", site, afxNodes -> {
    		var fx = afxNodes.get(0);
			var editor = AFBEditor.findOpenAFBEditorFor(fx.getFile());
			if (editor != null) {
				var afx = fx.getAfx();
				var currentName = afx instanceof NamedAFX nafx ? (nafx.name() != null ? nafx.name() : "") : "";
				var dialog = new InputDialog(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"Rename Effect",
					"Enter the new name for the effect:",
					currentName,
					null
				);
				if (dialog.open() == Window.OK) {
					editor.renameEffect(afx, dialog.getValue());
				}
			}
    	});    	
    	deleteAction = new AFXAction(true, "deleteEffect", ISharedImages.IMG_TOOL_DELETE, "Delete Effect", site, afxNodes -> {
    		var fx = afxNodes.get(0);
			var editor = AFBEditor.findOpenAFBEditorFor(fx.getFile());
			if (editor != null) {
				editor.removeEffects(afxNodes.stream().map(AFXNode::getAfx).toArray(AFX[]::new));
			}
    	});    	
    	copyAction = new AFXAction(false, "copyEffect", ISharedImages.IMG_TOOL_COPY, "Copy Effect", site, afxNodes -> {
    		var fx = afxNodes.get(0);
    		var afx = fx.getAfx();
    		var clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
    		try {
    			clipboard.setContents(
    				new Object[] { afx },
    				new Transfer[] { AFXTransfer.getInstance() }
    			);
    		} finally {
    			clipboard.dispose();
    		}
    	});
    	pasteAction = new Action("Paste Effect") {
    		{
    			setId("pasteEffect");
    			setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
    		}
    		
    		@Override
    		public boolean isEnabled() {
    			var clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
    			try {
    				return clipboard.getContents(AFXTransfer.getInstance()) != null;
    			} finally {
    				clipboard.dispose();
    			}
    		}
    		
    		@Override
    		public void run() {
    			var clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
    			try {
    				var contents = clipboard.getContents(AFXTransfer.getInstance());
    				if (contents instanceof AFX afx) {
    					// Try to find the target AFB editor from selection or active editor
    					var selection = (IStructuredSelection) site.getStructuredViewer().getSelection();
    					var node = selection.stream()
    						.filter(o -> o instanceof AFXNode)
    						.map(o -> (AFXNode) o)
    						.findFirst()
    						.orElse(null);
    					
    					AFBEditor editor = null;
    					if (node != null) {
    						editor = AFBEditor.findOpenAFBEditorFor(node.getFile());
    					}
    					if (editor == null) {
    						var activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    						if (activePart instanceof AFBEditor afbEditor) {
    							editor = afbEditor;
    						}
    					}
    					
    					if (editor != null) {
    						// Deep copy the AFX so we get independent frames
    						var copy = copyAfx(afx);
    						editor.addEffect(copy);
    					}
    				}
    			} finally {
    				clipboard.dispose();
    			}
    		}
    		
    		private AFX copyAfx(AFX source) {
    			var copy = AFX.create();
    			// Remove the default frame that create() adds
    			copy.remove(0);
    			for (var frame : source.frames()) {
    				copy.add(frame.copy());
    			}
    			if (source instanceof NamedAFX nafx && nafx.name() != null) {
    				var named = copy.named();
    				named.name(nafx.name() + " (Copy)");
    				return named;
    			}
    			return copy;
    		}
    	};
        
    }
    
    @Override
    public void fillActionBars(IActionBars actionBars) {
        actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
    }


    @Override
    public void fillContextMenu(IMenuManager menu) {
        menu.add(openAction);
        menu.add(deleteAction);
        menu.add(renameAction);
        menu.add(copyAction);
        menu.add(pasteAction);
    }
}
