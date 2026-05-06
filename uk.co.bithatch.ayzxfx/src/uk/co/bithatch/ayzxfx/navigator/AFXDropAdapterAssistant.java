package uk.co.bithatch.ayzxfx.navigator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

import uk.co.bithatch.ayzxfx.editor.AFBEditor;

public class AFXDropAdapterAssistant extends CommonDropAdapterAssistant {

	@Override
	public boolean isSupportedType(TransferData aTransferType) {
		return LocalSelectionTransfer.getTransfer().isSupportedType(aTransferType);
	}

	@Override
	public org.eclipse.core.runtime.IStatus validateDrop(Object target, int operation, TransferData transferType) {
		if (!LocalSelectionTransfer.getTransfer().isSupportedType(transferType))
			return org.eclipse.core.runtime.Status.CANCEL_STATUS;

		var selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null || selection.isEmpty())
			return org.eclipse.core.runtime.Status.CANCEL_STATUS;

		if (!(selection.getFirstElement() instanceof AFXNode draggedNode))
			return org.eclipse.core.runtime.Status.CANCEL_STATUS;

		if (target instanceof AFXNode targetNode) {
			if (targetNode.getFile().equals(draggedNode.getFile()))
				return org.eclipse.core.runtime.Status.OK_STATUS;
		} else if (target instanceof org.eclipse.core.resources.IFile file) {
			if (file.equals(draggedNode.getFile()))
				return org.eclipse.core.runtime.Status.OK_STATUS;
		}

		return org.eclipse.core.runtime.Status.CANCEL_STATUS;
	}

	@Override
	public org.eclipse.core.runtime.IStatus handleDrop(CommonDropAdapter dropAdapter, DropTargetEvent dropTargetEvent,
			Object target) {

		var selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null || selection.isEmpty())
			return org.eclipse.core.runtime.Status.CANCEL_STATUS;

		if (!(selection.getFirstElement() instanceof AFXNode draggedNode))
			return org.eclipse.core.runtime.Status.CANCEL_STATUS;

		var editor = AFBEditor.findOpenAFBEditorFor(draggedNode.getFile());
		if (editor == null)
			return org.eclipse.core.runtime.Status.CANCEL_STATUS;

		var afb = editor.afb();
		int fromIndex = draggedNode.getIndex();

		int toIndex;
		if (target instanceof AFXNode targetNode) {
			int targetIndex = targetNode.getIndex();
			var location = dropAdapter.getCurrentLocation();
			if (location == ViewerDropAdapter.LOCATION_AFTER) {
				toIndex = targetIndex + 1;
			} else {
				toIndex = targetIndex;
			}
		} else {
			toIndex = afb.size();
		}

		int effectiveTo = toIndex > fromIndex ? toIndex - 1 : toIndex;
		if (fromIndex != effectiveTo) {
			editor.moveEffect(fromIndex, toIndex);
		}

		return org.eclipse.core.runtime.Status.OK_STATUS;
	}
}
