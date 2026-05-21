package uk.co.bithatch.zximgconv;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Project nature that installs / removes the
 * {@link ZXImageConversionBuilder} on a project.
 */
public class ZXImageConversionNature implements IProjectNature {

	public static final String NATURE_ID = "uk.co.bithatch.zximgconv.nature";

	private IProject project;

	@Override
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (ICommand cmd : commands) {
			if (ZXImageConversionBuilder.BUILDER_ID.equals(cmd.getBuilderName())) {
				return; // already present
			}
		}

		ICommand newCommand = desc.newCommand();
		newCommand.setBuilderName(ZXImageConversionBuilder.BUILDER_ID);

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		newCommands[commands.length] = newCommand;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	@Override
	public void deconfigure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		ICommand[] newCommands = java.util.Arrays.stream(commands)
				.filter(c -> !ZXImageConversionBuilder.BUILDER_ID.equals(c.getBuilderName()))
				.toArray(ICommand[]::new);

		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}
}
