package uk.co.bithatch.widgetzx;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import uk.co.bithatch.bitzx.ZXPerspectives;

public class ZXPerspectivesUI {

	public static boolean isPerspective(String id) {
        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        var page = window.getActivePage();
        var currentPerspective = page.getPerspective();
        return id.equals(currentPerspective.getId());
	}

	public static void zxCodingPerspective(String pluginId) {
		perspective(pluginId, "ZX Coding", ZXPerspectives.ZX_CODING_ID);
	}

	public static void zxDebuggingPerspective(String pluginId) {
		perspective(pluginId, "ZX Debugging", ZXPerspectives.ZX_DEBUG_ID);
	}

	public static void zxMediaPerspective(String pluginId) {
		perspective(pluginId, "ZX Media", ZXPerspectives.ZX_MEDIA_ID);
	}

	public static void perspective(String pluginId, String name, String id) {
		final String PREFERENCE_KEY = id + ".switch.perspective";

        var prefs = InstanceScope.INSTANCE.getNode(pluginId);
        var pref = prefs.get(PREFERENCE_KEY, "prompt");

        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        var page = window.getActivePage();
        var currentPerspective = page.getPerspective();

        if (!id.equals(currentPerspective.getId())) {
            if ("prompt".equals(pref)) {
                var dialog = MessageDialogWithToggle.openYesNoQuestion(
                    window.getShell(),
                    "Switch to " + name + " Perspective?",
                    "This project works best in the " + name + " perspective.\n" +
                    "Would you like to switch now?",
                    "Remember my decision and do not ask again",
                    false, // toggle default
                    null,
                    null
                );

                var switchPerspective = (dialog.getReturnCode() == IDialogConstants.YES_ID);
                var toggleState = dialog.getToggleState() ? (switchPerspective ? "always" : "never") : "prompt";

                prefs.put(PREFERENCE_KEY, toggleState);
                try {
                    prefs.flush();
                } catch (BackingStoreException e) {
                    e.printStackTrace();
                }

                if (switchPerspective) {
                    switchToPerspective(id, window);
                }

            } else if ("always".equals(pref)) {
                switchToPerspective(id, window);
            }
        }
	}
    
    public static void switchToPerspective(String perspectiveId, IWorkbenchWindow window) {
        IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
        IPerspectiveDescriptor desc = registry.findPerspectiveWithId(perspectiveId);
        if (desc != null) {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().setPerspective(desc);
        }
    }
}
