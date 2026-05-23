package uk.co.bithatch.emuzx.ui;

import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.OUTPUT_FORMAT;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PROGRAM;
import static uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes.PROJECT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.bitzx.FileSet;
import uk.co.bithatch.bitzx.LanguageSystem;
import uk.co.bithatch.bitzx.LaunchContext;
import uk.co.bithatch.emuzx.AbstractConfigurationDelegate;
import uk.co.bithatch.emuzx.DefaultPreparationContext;
import uk.co.bithatch.emuzx.ExternallyLaunchableRegistry;

/**
 * Launch delegate for "Bundle Application" configurations. Performs compilation
 * and preparation (directory or FAT disk image) but does NOT launch any
 * emulator. The resulting artefact path is stored in the launch so the export
 * wizard can pick it up for optional archiving.
 */
public class BundleLaunchConfiguration extends AbstractConfigurationDelegate {

	public static final String ATTR_BUNDLE_OUTPUT_PATH = "uk.co.bithatch.emuzx.ui.bundleOutputPath";
	public static final String ATTR_ZIP_OUTPUT = "uk.co.bithatch.emuzx.ui.zipOutput";
	public static final String ATTR_ZIP_DESTINATION = "uk.co.bithatch.emuzx.ui.zipDestination";

	private static final ILog LOG = ILog.of(BundleLaunchConfiguration.class);

	public BundleLaunchConfiguration() {
		super(PROJECT, PROGRAM);
	}

	@Override
	public void launch(IFile file, ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {

		var prepCtx = new DefaultPreparationContext(configuration, file);
		var externallyLaunchable = ExternallyLaunchableRegistry.externallyLaunchableFor(file);

		var preparationTarget = PreparationTargetRegistry.targetFor(configuration);
		if (preparationTarget.isPresent()) {
			prepCtx.preparedBinaryFilePath(preparationTarget.get().init(prepCtx));
		}

		var launchCtx = LaunchContext.set(configuration);
		try {
			/* Pick the best format */
			var launchFmt = configuration.getAttribute(OUTPUT_FORMAT, "");
			var prj = file.getProject();
			var prjFmt = externallyLaunchable.getOutputFormat(prj);
			var actualFormat = launchFmt.equals("") ? prjFmt : LanguageSystem.outputFormatOrDefault(prj, launchFmt);
			prepCtx.outputFormat(actualFormat);

			/* Compile */
			externallyLaunchable.compileForLaunch(mode, prepCtx, monitor);
			launchCtx.attr(LaunchContext.BINARY_FILE, prepCtx.binaryFile());

			/* Prepare */
			if (preparationTarget.isPresent()) {
				var externalFiles = new ArrayList<FileSet>();
				try {
					var enabled = PreparationSourceRegistry.getSourceIds(configuration);
					for (var desc : PreparationSourceRegistry.descriptors()) {
						if (enabled.contains(desc.id())) {
							LOG.info(String.format("Contributing preparation source %s", desc.id()));
							desc.createSource().contribute(prepCtx, externalFiles, monitor);
						}
					}
					var status = preparationTarget.get().prepare(monitor, externalFiles);
					if (!status.isOK()) {
						throw new CoreException(status);
					}
				} catch (CoreException ce) {
					throw ce;
				} catch (Exception e) {
					throw new CoreException(Status.error("Failed to prepare bundle.", e));
				}
				preparationTarget.get().preparationDone();
			}

			/* Store the actual filesystem output path so the wizard can find it. */
			String actualOutputPath = null;
			if (preparationTarget.isPresent()) {
				actualOutputPath = preparationTarget.get().outputPath();
			}
			if (actualOutputPath != null) {
				launch.setAttribute(ATTR_BUNDLE_OUTPUT_PATH, actualOutputPath);
			}

			/* Optional zip */
			var zipOutput = configuration.getAttribute(ATTR_ZIP_OUTPUT, false);
			if (zipOutput && actualOutputPath != null && !actualOutputPath.isEmpty()) {
				var zipDest = configuration.getAttribute(ATTR_ZIP_DESTINATION, "");
				if (!zipDest.isEmpty()) {
					try {
						zipPath(new File(actualOutputPath), new File(zipDest), monitor);
					} catch (IOException e) {
						throw new CoreException(Status.error("Failed to create zip archive.", e));
					}
				}
			}

		} finally {
			try {
				prepCtx.close();
			} catch (Exception e) {
				LOG.error("Failed to close preparation context", e);
			}
			try {
				launchCtx.close();
			} catch (Exception e) {
				LOG.error("Failed to close launch context", e);
			}
			try {
				if (preparationTarget.isPresent()) {
					preparationTarget.get().cleanUp();
				}
			} finally {
				LaunchContext.clear();
			}
		}
	}

	/**
	 * Zip a file or directory to the given destination zip file.
	 */
	static void zipPath(File source, File zipFile, IProgressMonitor monitor) throws IOException {
		try (var zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			if (source.isDirectory()) {
				zipDirectory(source, source, zos, monitor);
			} else {
				zipSingleFile(source, zos, monitor);
			}
		}
	}

	private static void zipDirectory(File root, File current, ZipOutputStream zos, IProgressMonitor monitor)
			throws IOException {
		var files = current.listFiles();
		if (files == null) return;
		for (var f : files) {
			if (monitor.isCanceled()) return;
			if (f.isDirectory()) {
				zipDirectory(root, f, zos, monitor);
			} else {
				var entryName = root.toPath().relativize(f.toPath()).toString();
				zos.putNextEntry(new ZipEntry(entryName));
				try (var fis = new FileInputStream(f)) {
					fis.transferTo(zos);
				}
				zos.closeEntry();
			}
		}
	}

	private static void zipSingleFile(File file, ZipOutputStream zos, IProgressMonitor monitor) throws IOException {
		zos.putNextEntry(new ZipEntry(file.getName()));
		try (var fis = new FileInputStream(file)) {
			fis.transferTo(zos);
		}
		zos.closeEntry();
	}
}
