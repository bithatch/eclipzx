package uk.co.bithatch.fatexplorer.vfs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Display;

import uk.co.bithatch.fatexplorer.vfs.FileOverwritePolicy.Decision;

public class UIOverwritePolicyWithApplyToAll implements FileOverwritePolicy {

	private Decision globalDecision = null;

	@Override
	public Decision queryOverwrite(String path, boolean isMove) {
		if (globalDecision != null) {
			return globalDecision;
		}

		final Decision[] result = new Decision[1];
		Display.getDefault().syncExec(() -> {
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(
					Display.getDefault().getActiveShell(), "File Exists",
					(isMove ? "Move" : "Copy") + " will overwrite:\n" + path, "Apply this decision to all conflicts",
					false, null, null);

			boolean applyToAll = dialog.getToggleState();
			int returnCode = dialog.getReturnCode();

			Decision decision = switch (returnCode) {
			case IDialogConstants.YES_ID -> Decision.REPLACE;
			case IDialogConstants.NO_ID -> Decision.SKIP;
			default -> Decision.CANCEL;
			};

			if (applyToAll) {
				globalDecision = decision;
			}

			result[0] = decision;
		});

		return result[0];
	}
}
