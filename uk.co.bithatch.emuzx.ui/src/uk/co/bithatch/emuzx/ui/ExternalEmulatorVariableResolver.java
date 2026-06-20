package uk.co.bithatch.emuzx.ui;

import java.nio.file.Path;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.bitzx.LaunchContext;
import uk.co.bithatch.emuzx.DebugLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.emuzx.LaunchableRegistry;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;

public class ExternalEmulatorVariableResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		var ctx = LaunchContext.get();
		if (ctx != null) {
			var cfg = ctx.config();
			var projectName = cfg.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROJECT, "");
			var programName = cfg.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROGRAM, "");
			
			if (variable.getName().equals("ee_debug_port")) {
				return String.valueOf(cfg.getAttribute(DebugLaunchConfigurationAttributes.PORT, 0));
			} 
			else if (variable.getName().equals("ee_program_path")) {
				return programName;
			} else if (variable.getName().equals("ee_program_name")) {
				return stripExtension(basename(programName));
			} else if (variable.getName().equals("eeprogram_ext")) {
				return extension(basename(programName));
			} else {
				var project = PlatformUI.getWorkbench().getAdapter(IWorkspace.class).getRoot().getProject(projectName);
				if (project != null) {
					var srcfile = project.getFile(programName);
					var launchable = LaunchableRegistry.launchableFor(IExternallyLaunchable.class, srcfile);
					var lang = LanguageSystem.languageSystem(project);
					var outputFolder = lang.preferenceAccess().getOutputFolder(project).getLocation().toPath();
					if (variable.getName().equals("ee_program_loc")) {
						return srcfile.getLocation().toString();
					} else {
						var outputFormat = launchable.getOutputFormat(project);
						var binfile = launchable.getBinFile(srcfile.getLocation().toPath(), outputFolder, outputFormat).toFile();
						if (variable.getName().equals("ee_output_path")) {
							return project.getRawLocation().toPath().relativize(binfile.toPath()).toString();
						} else if (variable.getName().equals("ee_output_loc")) {
							return binfile.getAbsolutePath();
						} else if (variable.getName().equals("ee_output_name")) {
							return binfile.getName();
						}
					}

					var launchFile = (Path) ctx.attr(LaunchContext.LAUNCH_FILE);
					if(launchFile != null) {
						if (variable.getName().equals("ee_launch_path")) {
							return project.getRawLocation().toPath().relativize(launchFile).toString();
						} else if (variable.getName().equals("ee_launch_loc")) {
							return launchFile.toAbsolutePath().toString();
						} else if (variable.getName().equals("ee_launch_name")) {
							return launchFile.getFileName().toString();
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
