package uk.co.bithatch.jspeccy.commands;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.jspeccy.views.EmulatorView;

public class OpenInEmulatorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        var selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection sel) {
            var element = sel.getFirstElement();
            if (element instanceof IFile file) {
                File nativeFile = file.getLocation().toFile();
               EmulatorView.open(nativeFile);
            }
        }
        return null;
    }
}
