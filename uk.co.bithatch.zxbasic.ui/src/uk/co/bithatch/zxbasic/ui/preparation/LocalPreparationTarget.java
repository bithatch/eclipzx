package uk.co.bithatch.zxbasic.ui.preparation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.VariablesPlugin;

import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.util.FileItem;
import uk.co.bithatch.zxbasic.ui.util.FileSet;

public class LocalPreparationTarget extends AbstractPreparationTarget {

	private boolean cleanBeforeUse;
	private String targetLocation;

	public static String defaultLocalPreparation() {
		return Paths.get(System.getProperty("user.home")).resolve("Documents").resolve("EclipZX").resolve("Preparation")
				.toAbsolutePath().toString();
	}

	@Override
	public IStatus prepare(IProgressMonitor monitor, List<FileSet> files) throws CoreException {

		var efs = new File(targetLocation);

		if (cleanBeforeUse) {
			for (var c : efs.listFiles()) {
				try (var paths = Files.walk(c.toPath())) {
					paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
				} catch (IOException ioe) {
					throw new CoreException(Status.error("Failed clean files for preparation.", ioe));
				}
			}
		}

		var remaining = files.size();
		var subMonitor = SubMonitor.convert(monitor, remaining);
		for (var fileSet : files) {
			subMonitor.setWorkRemaining(remaining--);

			var dest = efs;
			var subfolder = fileSet.destination();

			if (!subfolder.equals("") && !subfolder.equals("\\") && !subfolder.equals("/")) {
				dest = new File(dest, subfolder.replace("\\", "/"));
			}

			var filesRemain = fileSet.files().length;
			var fileSetSubMonitor = subMonitor.split(filesRemain);

			for (var file : flatten(fileSet)) {
				fileSetSubMonitor.setWorkRemaining(filesRemain--);
				try {
					copyFileToFile(file, dest, fileSetSubMonitor.split(1));
				} catch (IOException e) {
					throw new CoreException(Status.error("Failed to copy file to local directory.", e));
				}
			}
		}

		return Status.OK_STATUS;
	}

	private void copyFileToFile(FileItem fileItem, File dest, SubMonitor monitor) throws IOException {
		var file = fileItem.file();
		if (file.isFile()) {
			var remain = (int) file.length();
			var subMonitor = monitor.split(remain);
			int rd;
			var buf = new byte[8192];
			try (var out = new FileOutputStream(new File(dest, fileItem.targetName()))) {
				try (var in = new FileInputStream(file)) {
					while ((rd = in.read(buf)) != -1) {
						subMonitor.setWorkRemaining(remain -= rd);
						out.write(buf, 0, rd);
					}
				}
			}
		} else {
			var t = new AtomicInteger();
			Files.walk(file.toPath()).forEach(s -> t.addAndGet(1));
			
			var subMonitor = monitor.split(t.get());
			Files.walk(file.toPath()).forEach(source -> {
				subMonitor.setWorkRemaining(t.getAndAdd(-1));
				var destination = dest.toPath().resolve(source);
				try {
					copyFileToFile(new FileItem(source.toFile()), destination.toFile(), subMonitor.split(1));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
	}

	@Override
	public String init(IPreparationContext prepCtx) throws CoreException {
		var strmgr = VariablesPlugin.getDefault().getStringVariableManager();
		targetLocation = strmgr.performStringSubstitution(prepCtx.launchConfiguration().getAttribute(
				ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_TARGET_LOCATION, defaultLocalPreparation()));
		cleanBeforeUse = prepCtx.launchConfiguration()
				.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PREPARATION_CLEAR_BEFORE_USE, false);
		
		return "/";
	}

	@Override
	public void cleanUp() {
	}

}
