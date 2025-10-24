package uk.co.bithatch.zxbasic.ui.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.xtext.ui.editor.XtextEditor;

import uk.co.bithatch.emuzx.AbstractEmulatorLaunchShortcut;
import uk.co.bithatch.emuzx.ui.ConsoleUtil;
import uk.co.bithatch.zxbasic.basic.Program;
//import uk.co.bithatch.zxbasic.borielsdk.tools.ZXPreprocessor;
import uk.co.bithatch.zxbasic.interpreter.ZXBasicInterpreter;

public class InterpreterLaunchShortcut extends AbstractEmulatorLaunchShortcut {
	
	private static final String ZX_BASIC_CONSOLE = "ZX Basic Console";
	
	public InterpreterLaunchShortcut() {
	}
	
//	static ZXPreprocessor createPreprocessor() {
//		return new ZXPreprocessor.Builder().
//				build();
//	}

	static ZXBasicInterpreter createInterpreter() {
		
		return new ZXBasicInterpreter.Builder().
				withHost(new InterpreterConsoleHost(showBasicConsole())).
				build();
	}

	protected static IOConsole showBasicConsole() {
		IOConsole cns = ConsoleUtil.getConsole(ZX_BASIC_CONSOLE); 
		ConsoleUtil.showConsoleView();
		ConsoleUtil.clear(ZX_BASIC_CONSOLE);
		return cns;
	}

	@Override
	public void launch(IEditorPart editorPart , String mode) {
		try {
			if (!(editorPart instanceof XtextEditor xtextEditor)) {
			    return; 
			}
			
			createInterpreter().run(xtextEditor.getDocument().readOnly(resource -> {
			    var root = resource.getContents().get(0);
			    if (root instanceof Program prog) {
			        return prog;
			    }
			    return null;
			}));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// TEMP DEBUG . REMOVE WHOLE METHOD WHEN LAUNCH WORKS FROM CONTEXT MENU
	@Override
	public IResource getLaunchableResource(ISelection selection) {
	    return ResourcesPlugin.getWorkspace().getRoot(); // dummy resource
	}

	@Override
	protected void doLaunch(IFile file, String mode) throws CoreException {

		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(InterpreterLaunchConfigurationAttributes.ID);

		String name = file.getProject().getName() + " [Interpreter]";
		ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null,
		    manager.generateLaunchConfigurationName(name));

		workingCopy.setAttribute(InterpreterLaunchConfigurationAttributes.PROGRAM, file.getName());
		workingCopy.setAttribute(InterpreterLaunchConfigurationAttributes.PROJECT, file.getProject().getName());

		ILaunchConfiguration config = workingCopy.doSave();
		DebugUITools.launch(config, mode);
		
	}

	@Override
	protected String[] getSupportedExtensions() {
		return new String[] { "bas" };
	}

}