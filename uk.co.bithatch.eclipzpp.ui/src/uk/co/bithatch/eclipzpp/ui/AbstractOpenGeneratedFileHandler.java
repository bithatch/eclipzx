package uk.co.bithatch.eclipzpp.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

public abstract class AbstractOpenGeneratedFileHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        var sourceFile = resolveSourceFile(event);
        if (sourceFile == null) {
            showInfo(event, "Open Generated File", "No source file is selected.");
            return null;
        }

        try {
            var generatedFile = sourceToGeneratedResource(sourceFile);
            if (generatedFile != null && generatedFile.exists()) {
                openInEditor(generatedFile);
            } else {
                showInfo(event, "Open Generated File",
                        "No generated file was found for '" + sourceFile.getName() + "'. Build the project first.");
            }
        } catch (Exception e) {
            showInfo(event, "Open Generated File", "Failed to resolve generated file: " + e.getMessage());
        }

        return null;
    }

    private IFile resolveSourceFile(ExecutionEvent event) {
        var selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection structuredSelection && structuredSelection.size() == 1) {
            var selected = structuredSelection.getFirstElement();
            if (selected instanceof IFile file) {
                return file;
            }
            if (selected instanceof org.eclipse.core.runtime.IAdaptable adaptable) {
                var adapted = adaptable.getAdapter(IFile.class);
                if (adapted != null) {
                    return adapted;
                }
            }
        }

        var activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor == null || activeEditor.getEditorInput() == null) {
            return null;
        }
        return activeEditor.getEditorInput().getAdapter(IFile.class);
    }

	public abstract IFile sourceToGeneratedResource(IFile sourceFile);

    private void showInfo(ExecutionEvent event, String title, String message) {
        var shell = HandlerUtil.getActiveShell(event);
        MessageDialog.openInformation(shell, title, message);
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
