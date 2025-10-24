package uk.co.bithatch.nextbuild;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;

import uk.co.bithatch.zxbasic.ui.api.IPreparationContext;
import uk.co.bithatch.zxbasic.ui.api.IPreparationSource;
import uk.co.bithatch.zxbasic.ui.util.FileItem;
import uk.co.bithatch.zxbasic.ui.util.FileSet;
import uk.co.bithatch.zxbasic.ui.util.FileSet.Purpose;

/**
 * Scans ZX BASIC program for <strong>LoadSD</strong> statements and adds
 * them to any disk preparation in progress. 
 * <p>
 * You probably don't want to use this if using the built in File Properties based configuration
 * for the same job. It here is for compatibility with NextBuild projects.
 */
public class FileContentPreparationSource implements IPreparationSource {


	public final static ILog LOG = ILog.of(IPreparationSource.class);

	@Override
	public void contribute(IPreparationContext ctx, List<FileSet> fileSets, IProgressMonitor monitor) throws CoreException {
		var programFile = ctx.programFile();
		if(programFile.getProject().hasNature(NextBuildNature.NATURE_ID)) {
			var files = new ArrayList<FileItem>();
			try (var rdr = new BufferedReader(new InputStreamReader(programFile.getContents()))) {
				String str = null;
				while ((str = rdr.readLine()) != null) {
					var x = str.replace(")", ",").replace("(", ",");
					var lower = x.toLowerCase().strip();
					var idx = lower.indexOf("loadsd,");
					if (idx !=-1 && lower.indexOf("sub") == -1 && lower.charAt(0) != '\'') {
						var sdfile = resolve(programFile, x.split("\"")[1]);
						if (Files.exists(sdfile)) {
							LOG.info(String.format("Adding LoadSD file %s to disk image", sdfile));
							files.add(new FileItem(sdfile.toFile()));
						}
						else {
							LOG.warn(String.format("LoadSD file %s does not exist, will show error at runtime", sdfile));
						}
					}
				}
			} catch (CoreException ce) {
				throw new IllegalStateException(ce);
			} catch (IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
			
			if(files.size() > 0) {
				fileSets.add(new FileSet(Purpose.ANCILLARY, files));
			}
		}
	}

	protected Path resolve(IFile file, String name) {
		return file.getLocation().toPath().getParent().resolve("data/" + name);
	}

}
