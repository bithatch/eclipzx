package uk.co.bithatch.zxbasic.ui.launch;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.XtextResourceSet;

import uk.co.bithatch.emuzx.AbstractConfigurationDelegate;
import uk.co.bithatch.zxbasic.BasicStandaloneSetup;
import uk.co.bithatch.zxbasic.basic.CodeBlock;
import uk.co.bithatch.zxbasic.basic.Program;
import uk.co.bithatch.zxbasic.preprocessor.ZXPreprocessor;
//import uk.co.bithatch.zxbasic.borielsdk.tools.ZXPreprocessor;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class InterpreterLaunchConfiguration extends AbstractConfigurationDelegate {

	public InterpreterLaunchConfiguration() {
		super(InterpreterLaunchConfigurationAttributes.PROJECT, InterpreterLaunchConfigurationAttributes.PROGRAM);
	}
	@Override
	public void launch(IFile programFile, ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		var absPath = programFile.getLocation().toFile().getAbsolutePath();
		
		/* Build the preprocessor */
		var ppfsBldr = new ZXPreprocessor.FileSystemResourceResolver.Builder().
				addIncludePaths(ZXBasicPreferencesAccess.get().getAllLibs(
						programFile.getProject()).stream().map(File::toPath).toList()).
				withWorkingDir(programFile.getLocation().toFile().getParentFile());
		
		var pp = new ZXPreprocessor.Builder().
				withDefines(ZXBasicPreferencesAccess.get().getDefines(programFile.getProject())).
				withResourceResolver(ppfsBldr.build()).build();
		
		/* Preprocess */
		String preprocessed = "TODO!";
//		try {
//			preprocessed = String.join(System.lineSeparator(),
//					pp.process(programFile.getLocation().toPath()).toList());
//		} catch (IOException e) {
//			throw new CoreException(Status.error("Failed to preprocess.", e));
//		}
		
		var basicInjector = new BasicStandaloneSetup().createInjectorAndDoEMFRegistration();
		var basicResourceSet = basicInjector.getInstance(XtextResourceSet.class);
		
		// Step 1: Create a dummy URI (must be unique in the resource set)
		var dummyURI = URI.createURI("dummy.zxbasic");

		// Step 2: Create the resource using that URI
		var basicResource = basicResourceSet.createResource(dummyURI);

		// Step 3: Load your string into the resource using a Reader
//		try (var reader = new StringInputStream(preprocessed, "US-ASCII")) {
//		    basicResource.load(reader, basicResourceSet.getLoadOptions());
//		}
//		catch(IOException ioe) {
//			throw new CoreException(Status.error("Failed to load preprocessed resource."));
//		}
		
		if (basicResource.getContents().isEmpty() || !(basicResource.getContents().get(0) instanceof CodeBlock)) {
			throw new CoreException(Status.error("Failed to load valid ZX Basic Program from file: " + absPath));
		}

		var programModel = (Program) basicResource.getContents().get(0);
		var interpreter = InterpreterLaunchShortcut.createInterpreter();
		launch.addDebugTarget(new InterpreterDebugTarget(launch, interpreter));
		interpreter.run(programModel);
	}


}