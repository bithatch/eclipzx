package uk.co.bithatch.drawzx.sprites;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import uk.co.bithatch.drawzx.editor.AbstractSpriteEditor;

public class RedoHandler extends AbstractHandler implements IOperationHistoryListener, IPartListener {

    private final IOperationHistory history;

    public RedoHandler() {
        history = OperationHistoryFactory.getOperationHistory();
        history.addOperationHistoryListener(this);
        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            window.getPartService().addPartListener(this);
        }
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart part = HandlerUtil.getActiveEditor(event);
        if (part instanceof AbstractSpriteEditor ase) {
            var context = ase.getUndoContext();
            if (history.canRedo(context)) {
                history.redo(context, null, null);
            }
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            var page = window.getActivePage();
            if (page != null) {
                var part = page.getActiveEditor();
                if (part instanceof AbstractSpriteEditor ase) {
                    return history.canRedo(ase.getUndoContext());
                }
            }
        }
        return false;
    }

    @Override
    public void historyNotification(OperationHistoryEvent event) {
        var type = event.getEventType();
        if (type == OperationHistoryEvent.DONE || type == OperationHistoryEvent.UNDONE
                || type == OperationHistoryEvent.REDONE || type == OperationHistoryEvent.OPERATION_REMOVED) {
            notifyChanged();
        }
    }

    @Override
    public void partActivated(IWorkbenchPart part) {
        notifyChanged();
    }

    @Override public void partBroughtToTop(IWorkbenchPart part) {}
    @Override public void partClosed(IWorkbenchPart part) {}
    @Override public void partDeactivated(IWorkbenchPart part) {}
    @Override public void partOpened(IWorkbenchPart part) {}

    private void notifyChanged() {
        var display = PlatformUI.getWorkbench().getDisplay();
        if (!display.isDisposed()) {
            display.asyncExec(() -> fireHandlerChanged(new HandlerEvent(this, true, false)));
        }
    }

    @Override
    public void dispose() {
        history.removeOperationHistoryListener(this);
        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            window.getPartService().removePartListener(this);
        }
        super.dispose();
    }
}