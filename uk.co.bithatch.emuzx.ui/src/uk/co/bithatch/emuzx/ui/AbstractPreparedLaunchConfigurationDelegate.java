package uk.co.bithatch.emuzx.ui;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.LaunchContext;
import uk.co.bithatch.emuzx.AbstractConfigurationDelegate;
import uk.co.bithatch.emuzx.DefaultPreparationContext;
import uk.co.bithatch.emuzx.LaunchableRegistry;
import uk.co.bithatch.emuzx.api.ILaunchable;
import uk.co.bithatch.emuzx.api.IPreparationTarget;
import uk.co.bithatch.emuzx.api.IWritablePreparationContext;

public abstract class AbstractPreparedLaunchConfigurationDelegate<L extends ILaunchable> extends AbstractConfigurationDelegate {
	private final static ILog LOG = ILog.of(AbstractPreparedLaunchConfigurationDelegate.class);
	private Class<L> launchableType;

	protected AbstractPreparedLaunchConfigurationDelegate(String projectKey, String programKey, Class<L> launchableType) {
		super(projectKey, programKey);
		this.launchableType = launchableType;
	}

	@Override
	public final void launch(IFile file, ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		var prepCtx = createPreparationContext(file, configuration);
		var externallyLaunchable = LaunchableRegistry.launchableFor(launchableType, file);

		/*
		 * Initialise preparation target if there is one. Don't do preparation just yet,
		 * just set up context for dynamic variables etc
		 */
		var preparationTarget = PreparationTargetRegistry.targetFor(configuration);
		if (preparationTarget.isPresent()) {
			prepCtx.preparedBinaryFilePath(preparationTarget.get().init(prepCtx));
		}
		var launchCtx = LaunchContext.set(configuration);
		try {

			/* Pick the best format to use */		
			var actualFormat = externallyLaunchable.getLaunchFormat(configuration, file);
			prepCtx.outputFormat(actualFormat);

			/* Compile to the chosen format for the launch. */
			externallyLaunchable.compileForLaunch(mode, prepCtx, monitor, getSupportedFormatsFilter());
			launchCtx.attr(LaunchContext.LAUNCH_FILE, prepCtx.launchFile());

			/* If there is preparation to do, do it now */
			if (preparationTarget.isPresent()) {

				var externalFiles = new ArrayList<FileSet>();

				/* Do the prep */
				try {
					/* Plugins that can find more resources to contribute */
					var enabled = PreparationSourceRegistry.getSourceIds(configuration);
					for (var desc : PreparationSourceRegistry.descriptors()) {
						if(enabled.contains(desc.id())) {
							LOG.info(String.format("Contributing preparation source %s", desc.id()));
							desc.createSource().contribute(prepCtx, externalFiles, monitor);
						}
						else {
							LOG.info(String.format("Preparation source %s is not enabled, skipping.", desc.id()));
						}
					}
					
					var status = preparationTarget.get().prepare(monitor, externalFiles);
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				}
				catch (CoreException ce) {
					closeContexts(launchCtx, prepCtx);
					throw ce;
				}
				catch(Exception e) {
					closeContexts(launchCtx, prepCtx);
					throw new CoreException(Status.error("Failed to prepare for launch.", e));
				}
				
				preparationTarget.get().preparationDone();
			}
			
			preparedLaunch(configuration, launch, monitor, preparationTarget, mode, file, prepCtx, externallyLaunchable, launchCtx);

			
		} catch (IllegalStateException ise) {
			closeContexts(launchCtx, prepCtx);
			if (ise.getCause() instanceof CoreException ce) {
				throw ce;
			} else {
				throw ise;
			}
		} finally {
			/*
			 * Clean up preparation if there is any (remove any context from dynamic
			 * variables that are based on launch attributes). This should NOT
			 * close the preparation context as the debug target may still want to access it, but it
			 */
			try {
				if (preparationTarget.isPresent()) {
					preparationTarget.get().cleanUp();
				}
			}
			finally {
				/* Un-set the thread local (does not remove temp files yet) */
				LaunchContext.clear();
			}
		}

	}
	
	protected Predicate<IOutputFormat> getSupportedFormatsFilter() {
		return (f) -> true;
	}

	protected abstract void preparedLaunch(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor,
			Optional<IPreparationTarget> preparationTarget, String mode, IFile file, IWritablePreparationContext prepCtx, L launchable, LaunchContext launchCtx) throws CoreException;

	protected IWritablePreparationContext createPreparationContext(IFile file, ILaunchConfiguration configuration) {
		return new DefaultPreparationContext(configuration, file);
	}

	protected void closeContexts(LaunchContext launchCtx, IWritablePreparationContext prepCtx) {
	}

}
