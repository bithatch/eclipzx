package uk.co.bithatch.ayzxfx.navigator;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

import uk.co.bithatch.ayzxfx.editor.AFBEditor;

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

    @Override
    public void init(ICommonActionExtensionSite site) {
    	
    	openAction = new AFXAction(false, "openEffect", null, "Open Effect", site, afxNodes -> {
    		var fx = afxNodes.get(0);
			var editor = AFBEditor.findOpenAFBEditorFor(fx.getFile());
			editor.afx(fx.getAfx());
    	});    	
    	renameAction = new AFXAction(false, "renameEffect", null, "Rename Effect", site, afxNodes -> {});    	
    	deleteAction = new AFXAction(true, "deleteEffect", ISharedImages.IMG_TOOL_DELETE, "Delete Effect", site, afxNodes -> {
    		var fx = afxNodes.get(0);
			var editor = AFBEditor.findOpenAFBEditorFor(fx.getFile());
			editor.removeEffects(fx.getAfx());
    	});    	
    	copyAction = new AFXAction(false, "copyEffect", ISharedImages.IMG_TOOL_COPY, "Copy Effect", site, afxNodes -> {});
        
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        menu.add(openAction);
        menu.add(deleteAction);
        menu.add(renameAction);
        menu.add(copyAction);
    }
}
