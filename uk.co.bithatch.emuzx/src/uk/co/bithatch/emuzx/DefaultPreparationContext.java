package uk.co.bithatch.emuzx;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.IProgramBuildOptions;
import uk.co.bithatch.emuzx.api.IWritablePreparationContext;

public class DefaultPreparationContext implements IWritablePreparationContext {

	private IProgramBuildOptions buildOptions;
	private final IFile programFile;
	private Path binaryFile;
	private IOutputFormat outputFormat;
	private String preparedBinaryFilePath;
	private ILaunchConfiguration configuration;
	private List<Runnable> cleanUpTasks = new ArrayList<>();
	
	public DefaultPreparationContext(ILaunchConfiguration configuration, IFile programFile) {
		this.programFile = programFile;
		this.configuration = configuration;
	}

	public DefaultPreparationContext(ILaunchConfiguration configuration, IFile programFile, IOutputFormat outputFormat) {
		this(configuration, programFile);
		outputFormat(outputFormat);
	}

	@Override
	public IProgramBuildOptions buildOptions() {
		if(buildOptions == null)
			throw new IllegalStateException("Build options accessed before they were set.");
		return buildOptions;
	}

	@Override
	public void buildOptions(IProgramBuildOptions buildOptions) {
		this.buildOptions = buildOptions;
	}

	@Override
	public IFile programFile() {
		return programFile;
	}

	@Override
	public Path launchFile() {
		return binaryFile;
	}

	@Override
	public void launchFile(Path binaryFile) {
		this.binaryFile = binaryFile;
	}

	@Override
	public IOutputFormat outputFormat() {
		if(outputFormat == null)
			throw new IllegalStateException("Output format accessed before is was set.");
		return outputFormat;
	}

	@Override
	public void outputFormat(IOutputFormat outputFormat) {
		this.outputFormat = outputFormat;		
	}

	@Override
	public Optional<String> preparedBinaryFilePath() {
		return Optional.ofNullable(preparedBinaryFilePath);
	}

	@Override
	public void preparedBinaryFilePath(String preparedBinaryFilePath) {
		this.preparedBinaryFilePath = preparedBinaryFilePath;
	}

	@Override
	public ILaunchConfiguration launchConfiguration() {
		return configuration;
	}

	@Override
	public void addCleanUpTask(Runnable task) {
		cleanUpTasks.add(task);		
	}

	@Override
	public void close() {
		cleanUpTasks.forEach(Runnable::run);
	}
}
