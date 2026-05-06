package uk.co.bithatch.ayzxfx.navigator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;

public class AFXDragAdapterAssistant extends CommonDragAdapterAssistant {

	private static volatile IStructuredSelection draggedSelection;

	public static IStructuredSelection getDraggedSelection() {
		return draggedSelection;
	}

	@Override
	public Transfer[] getSupportedTransferTypes() {
		return new Transfer[] { LocalSelectionTransfer.getTransfer() };
	}

	@Override
	public boolean setDragData(DragSourceEvent event, IStructuredSelection selection) {
		for (var it = selection.iterator(); it.hasNext();) {
			if (!(it.next() instanceof AFXNode)) {
				return false;
			}
		}
		if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
			System.out.println("DRAG START: captured " + selection.size() + " items, first=" + 
				(selection.getFirstElement() instanceof AFXNode n ? "AFXNode index=" + n.getIndex() : selection.getFirstElement()));
			draggedSelection = selection;
			LocalSelectionTransfer.getTransfer().setSelection(selection);
			return true;
		}
		return false;
	}
}
