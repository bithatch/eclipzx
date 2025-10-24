package uk.co.bithatch.zxbasic.ui.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class ZXBasicNature implements IProjectNature {
	public static final String NATURE_ID = "uk.co.bithatch.zxbasic.ZXBasicNature";

	private IProject project;
	
	public ZXBasicNature() {} 

	@Override
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (ICommand cmd : commands) {
			if (cmd.getBuilderName().equals(ZXBasicBuilder.BUILDER_ID))
				return; // already added
		}

		ICommand newCommand = desc.newCommand();
		newCommand.setBuilderName(ZXBasicBuilder.BUILDER_ID);

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
		List<ICommand> newCommands = new ArrayList<>();

		for (ICommand cmd : commands) {
			if (!cmd.getBuilderName().equals(ZXBasicBuilder.BUILDER_ID)) {
				newCommands.add(cmd);
			}
		}

		desc.setBuildSpec(newCommands.toArray(new ICommand[0]));
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
