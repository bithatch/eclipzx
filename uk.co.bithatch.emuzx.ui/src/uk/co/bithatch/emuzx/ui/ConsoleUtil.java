package uk.co.bithatch.emuzx.ui;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;

public class ConsoleUtil {

    private static IOConsole zxConsole;

    public static IOConsole getConsole(String name) {
        if (zxConsole != null) return zxConsole;

        IOConsole console = new IOConsole(name, null);
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
        zxConsole = console;
        return console;
    }

    public static void clear(String name) {
        getConsole(name).clearConsole();
    }

    public static IOConsoleOutputStream getOutputStream(String name) {
        return getConsole(name).newOutputStream();
    }

    public static IOConsoleInputStream getInputStream(String name) {
        return getConsole(name).getInputStream();
    }
    
    public static Color getColor(int col) {
    	var display = Display.getDefault();
		switch(col) {
    	case 0:
    		return display.getSystemColor(SWT.COLOR_BLACK);
    	case 1:
    		return display.getSystemColor(SWT.COLOR_DARK_BLUE);
    	case 2:
    		return display.getSystemColor(SWT.COLOR_DARK_RED);
    	case 3:
    		return display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
    	case 4:
    		return display.getSystemColor(SWT.COLOR_DARK_GREEN);
    	case 5:
    		return display.getSystemColor(SWT.COLOR_DARK_CYAN);
    	case 6:
    		return display.getSystemColor(SWT.COLOR_DARK_YELLOW);
    	case 7:
    		return display.getSystemColor(SWT.COLOR_GRAY);
    	case 8:
    		return display.getSystemColor(SWT.COLOR_BLACK);
    	case 9:
    		return display.getSystemColor(SWT.COLOR_BLUE);
    	case 10:
    		return display.getSystemColor(SWT.COLOR_RED);
    	case 11:
    		return display.getSystemColor(SWT.COLOR_MAGENTA);
    	case 12:
    		return display.getSystemColor(SWT.COLOR_GREEN);
    	case 13:
    		return display.getSystemColor(SWT.COLOR_CYAN);
    	case 14:
    		return display.getSystemColor(SWT.COLOR_YELLOW);
    	default:
    		return display.getSystemColor(SWT.COLOR_WHITE);
    	}
    }

    public static void showConsoleView() {
        Display.getDefault().asyncExec(() -> {
            try {
                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (window != null) {
                    IWorkbenchPage page = window.getActivePage();
                    if (page != null) {
                        page.showView(IConsoleConstants.ID_CONSOLE_VIEW, null, IWorkbenchPage.VIEW_ACTIVATE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
