package uk.co.bithatch.eclipz88dk.toolchain;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.eclipz88dk.wizard.CdtProjectCreator;

/**
 * Project nature for Z88DK C projects.  Adding this nature configures the
 * project with the Z88DK clean builder and enables Z88DK features (content
 * type mappings, language settings provider, indexer reindex).
 */
public class Z88DKNature implements IProjectNature {

	public static final String NATURE_ID = "uk.co.bithatch.eclipz88dk.Z88DKNature";

	private static final ILog LOG = ILog.of(Z88DKNature.class);

	private IProject project;

	public Z88DKNature() {}

	@Override
	public void configure() throws CoreException {
		/* Add the clean builder */
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		boolean hasCleanBuilder = false;

		for (ICommand cmd : commands) {
			if (cmd.getBuilderName().equals(Z88DKCleanBuilder.BUILDER_ID)) {
				hasCleanBuilder = true;
				break;
			}
		}

		if (!hasCleanBuilder) {
			ICommand newCommand = desc.newCommand();
			newCommand.setBuilderName(Z88DKCleanBuilder.BUILDER_ID);

			ICommand[] newCommands = new ICommand[commands.length + 1];
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			newCommands[commands.length] = newCommand;

			desc.setBuildSpec(newCommands);
			project.setDescription(desc, null);
		}

		/* Enable Z88DK features (content types, language settings, etc.) */
		try {
			CdtProjectCreator.enableZ88DKFeatures(project);
		} catch (Exception e) {
			LOG.error("Failed to enable Z88DK features when configuring nature", e);
		}
	}

	@Override
	public void deconfigure() throws CoreException {
		/* Remove the clean builder */
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();
		List<ICommand> newCommands = new ArrayList<>();

		for (ICommand cmd : commands) {
			if (!cmd.getBuilderName().equals(Z88DKCleanBuilder.BUILDER_ID)) {
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
