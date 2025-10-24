package uk.co.bithatch.fatexplorer.vfs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import uk.co.bithatch.fatexplorer.vfs.FileOverwritePolicy.Decision;

public class UIOverwritePolicy implements FileOverwritePolicy {

    @Override
    public Decision queryOverwrite(String path, boolean isMove) {
        final Decision[] result = new Decision[1];
        Display.getDefault().syncExec(() -> {
            MessageDialog dialog = new MessageDialog(
                Display.getDefault().getActiveShell(),
                "File Already Exists",
                null,
                "The file already exists:\n" + path + "\n\n" +
                (isMove ? "Do you want to move and replace it?" : "Do you want to copy and replace it?"),
                MessageDialog.QUESTION,
                new String[]{"Replace", "Skip", "Cancel"},
                0);
            int response = dialog.open();
            result[0] = switch (response) {
                case 0 -> Decision.REPLACE;
                case 1 -> Decision.SKIP;
                default -> Decision.CANCEL;
            };
        });
        return result[0];
    }
}
