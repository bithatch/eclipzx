package uk.co.bithatch.zxbasic.ui.editor;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
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

import uk.co.bithatch.zxbasic.ui.builder.ZXBasicBuilder;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

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
        
        var project = sourceFile.getProject();
		var bldr = ZXBasicBuilder.builderForProject(project);
		
        bldr.withMemoryMap(false);
        bldr.withOutputFormat(BorielZXBasicOutputFormat.ASM);
        
        var zxbc = bldr.build();
        var nativeFile = sourceFile.getLocation().toFile();
		if(zxbc.isNeedsProcessing(nativeFile)) {
        	try {
				zxbc.compile(nativeFile);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to generate ASM.");
			}
        }
		
		try {
			ZXBasicPreferencesAccess.get().getOutputFolder(project).refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
		
		var outputPath = zxbc.targetFile(nativeFile).toPath();
		var res = root.findFilesForLocationURI(outputPath.toUri());
		if(res != null && res.length > 0) {
			return res[0];
		}
		else
			return null;
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
