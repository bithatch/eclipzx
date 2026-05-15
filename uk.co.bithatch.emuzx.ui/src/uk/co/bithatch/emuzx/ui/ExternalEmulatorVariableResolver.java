package uk.co.bithatch.emuzx.ui;

import java.io.File;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.bitzx.LaunchContext;
import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.ExternallyLaunchableRegistry;

public class ExternalEmulatorVariableResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		var ctx = LaunchContext.get();
		if (ctx != null) {
			var cfg = ctx.config();
			var projectName = cfg.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROJECT, "");
			var programName = cfg.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROGRAM, "");
			
			if (variable.getName().equals("ee_program_path")) {
				return programName;
			} else if (variable.getName().equals("ee_program_name")) {
				return stripExtension(basename(programName));
			} else if (variable.getName().equals("eeprogram_ext")) {
				return extension(basename(programName));
			} else {
				var project = PlatformUI.getWorkbench().getAdapter(IWorkspace.class).getRoot().getProject(projectName);
				if (project != null) {
					var srcfile = project.getFile(programName);
					if (variable.getName().equals("ee_program_loc")) {
						return srcfile.getLocation().toString();
					} else {
						var launchable = ExternallyLaunchableRegistry.externallyLaunchableFor(srcfile);
						var outputFormat = launchable.getOutputFormat(project);
						var outputFolder = launchable.getOutputFolder(project);
						var binfile = launchable.getBinFile(srcfile.getLocation().toPath(), outputFolder, outputFormat).toFile();
						if (variable.getName().equals("ee_output_path")) {
							return project.getRawLocation().toPath().relativize(binfile.toPath()).toString();
						} else if (variable.getName().equals("ee_output_loc")) {
							return binfile.getAbsolutePath();
						} else if (variable.getName().equals("ee_output_name")) {
							return binfile.getName();
						}
					}

					var binfile = (File) ctx.attr(LaunchContext.BINARY_FILE);
					if (variable.getName().equals("ee_launch_path")) {
						return project.getRawLocation().toPath().relativize(binfile.toPath()).toString();
					} else if (variable.getName().equals("ee_launch_loc")) {
						return binfile.getAbsolutePath();
					} else if (variable.getName().equals("ee_launch_name")) {
						return binfile.getName();
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
