package uk.co.bithatch.zxbasic.ui.launch;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.tools.ZXBC;

public class ZXBasicVariableResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		var ctx = ZXBasicLaunchContext.get();
		if (ctx != null) {
			var projectName = ctx.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROJECT, "");
			var programName = ctx.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROGRAM, "");

			if (variable.getName().equals("zxbasic_program_path")) {
				return programName;
			} else if (variable.getName().equals("zxbasic_program_name")) {
				return stripExtension(basename(programName));
			} else if (variable.getName().equals("zxbasic_program_ext")) {
				return extension(basename(programName));
			} else {
				var project = PlatformUI.getWorkbench().getAdapter(IWorkspace.class).getRoot().getProject(projectName);
				if (project != null) {
					var srcfile = project.getFile(programName);
					if (variable.getName().equals("zxbasic_program_loc")) {
						return srcfile.getLocation().toString();
					} else {
						var outputFormat = (BorielZXBasicOutputFormat)ZXBasicPreferencesAccess.get().getOutputFormat(project);
						var outputFolder = ZXBasicPreferencesAccess.get().getOutputFolder(project).getRawLocation().toFile();
						var binfile = ZXBC.targetFile(srcfile.getRawLocation().toFile(), outputFolder, outputFormat);
						if (variable.getName().equals("zxbasic_output_path")) {
							return project.getRawLocation().toPath().relativize(binfile.toPath()).toString();	
						} else if (variable.getName().equals("zxbasic_output_loc")) {
							return binfile.getAbsolutePath();
						} else if (variable.getName().equals("zxbasic_output_name")) {
							return binfile.getName();
						}
					}
				}
			}

		}
		return null;
	}

	private String extension(String name) {
		if (name == null)
			return null;
		var idx = name.lastIndexOf('.');
		return idx == -1 ? null : name.substring(idx + 1);
	}

	private String stripExtension(String name) {
		if (name == null)
			return null;
		var idx = name.lastIndexOf('.');
		return idx == -1 ? name : name.substring(0, idx);
	}

	private String basename(String name) {
		if (name == null)
			return null;
		var arr = name.split("\\\\|/");
		return arr[arr.length - 1];
	}

}
