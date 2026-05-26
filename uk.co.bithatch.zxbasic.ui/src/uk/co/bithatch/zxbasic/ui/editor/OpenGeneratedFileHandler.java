package uk.co.bithatch.zxbasic.ui.editor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import uk.co.bithatch.zxbasic.ui.builder.ZXDebugBuild;

public class OpenGeneratedFileHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        var activeEditor = HandlerUtil.getActiveEditor(event);
        if (!(activeEditor instanceof XtextEditor editor)) {
            return null;
        }

        var document = editor.getDocument();
        var selection = (ITextSelection) editor.getSelectionProvider().getSelection();

        var selectedElement = getEObjectAtOffset(document, selection.getOffset());
        if (selectedElement == null) {
            return null;
        }

        // Use your logic to resolve the generated file
        var asmFile = resolveGeneratedAsmFile(selectedElement);
        if (asmFile != null && asmFile.exists()) {
            openInEditor(asmFile);
        }

        return null;
    }

    private EObject getEObjectAtOffset(IXtextDocument document, int offset) {
        return document.readOnly(resource -> {
            IParseResult parseResult = resource.getParseResult();
            if (parseResult == null || parseResult.getRootNode() == null)
                return null;
            INode node = NodeModelUtils.findLeafNodeAtOffset(parseResult.getRootNode(), offset);
            return NodeModelUtils.findActualSemanticObjectFor(node);
        });
    }

    private IFile resolveGeneratedAsmFile(EObject sourceElement) {
    	var resource = sourceElement.eResource();
        if (resource == null || resource.getURI() == null || resource.getURI().isPlatformResource() == false)
            return null;

        var root = ResourcesPlugin.getWorkspace().getRoot();
        var sourcePath = new Path(resource.getURI().toPlatformString(true));
        var sourceFile = root.getFile(sourcePath);
        
        return ZXDebugBuild.generateAsm(sourceFile);
    }

    private void openInEditor(IFile file) {
        Display.getDefault().asyncExec(() -> {
            try {
                IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
            } catch (PartInitException e) {
                e.printStackTrace();
            }
        });
    }
}
