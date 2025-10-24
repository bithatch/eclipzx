package uk.co.bithatch.nextbuild;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.zxbasic.tools.NexConverter;
import uk.co.bithatch.zxbasic.tools.NexConverter.NexConfiguration;
import uk.co.bithatch.zxbasic.ui.api.INEXConfigurer;

/**
 * Scans ZX BASIC program for <strong>LoadSDBank</strong> statements and injects
 * them into the NEX file (when configured to do so). 
 * <p>
 * You probably don't want to use this if using the built in File Properties based configuration
 * for the same job. It here is for compatibility with NextBuild projects.
 */
public class FileContentNEXConfigurer implements INEXConfigurer {


	public final static ILog LOG = ILog.of(FileContentNEXConfigurer.class);

	@Override
	public void configure(IFile file, NexConfiguration nexConfiguration) throws CoreException {
		if(file.getProject().hasNature(NextBuildNature.NATURE_ID)) {
			try (var rdr = new BufferedReader(new InputStreamReader(file.getContents()))) {
				String str = null;
				int line = 0;
				while ((str = rdr.readLine()) != null) {
					if (line < 256) {
						var arr = str.trim().split("\\s+");
						if (arr.length == 0)
							continue;
						var arg = arr[0];
						if (arg.toLowerCase().startsWith("'bmp=")) {
							var bmpfile = resolve(file, arg.substring(5));
							LOG.info(String.format("Adding bitmap file %s", bmpfile));
							nexConfiguration.bmp(bmpfile, false, true, 0, 0, 0, 0, 255);
						}
					}
					line++;
	
					var x = str.replace(")", ",").replace("(", ",");
					var lower = x.toLowerCase().strip();
					var idx = lower.indexOf("loadsdbank,");
	
					if (idx != -1 && lower.indexOf("sub") == -1 && lower.charAt(0) != '\'') {
						var filename = resolve(file, x.split("\"")[1]);
						if (!Files.exists(filename)) {
							throw new IOException("File specified by LoadSDBank does not exist at " + filename + ".");
						}
						var xargs = x.split(",");

						var offval = NexConverter.parseInt(xargs[2]) & 0x1fff;
						var fileoffset = NexConverter.parseInt(xargs[4]);
						var bank = NexConverter.parseInt(xargs[5]);
						if (fileoffset > 0) {
							var tmpfile = Files.createTempFile("eclipzx", ".bnk");
							try {
								try (var out = Files.newOutputStream(tmpfile)) {
									try (var in = Files.newInputStream(filename)) {
										in.skipNBytes(fileoffset);
										in.transferTo(out);
									}
								}
								LOG.info(String.format("Adding file %s from 'LoadSDBank()' to NEX at %04x in bank %d", filename, offval, bank));
								nexConfiguration.mmu(tmpfile, bank, offval);
							} finally {
								Files.delete(tmpfile);
							}
						} else {
							LOG.info(String.format("Adding file %s from 'LoadSDBank()' to NEX in bank %d to default offset", filename, bank, offval));
							nexConfiguration.mmu(filename, bank, offval);
						}
					}
					else {
						idx = lower.indexOf("loadbmp,");
						if (idx !=-1 && lower.indexOf("sub") == -1 && lower.charAt(0) != '\'') {
							var bmpfile = resolve(file, x.split("\"")[1]);
							if (!Files.exists(bmpfile)) {
								throw new IOException("File specified by LoadBMP does not exist at " + bmpfile + ".");
							}
							LOG.info(String.format("Adding bitmap file %s", bmpfile));
							nexConfiguration.bmp(bmpfile, false, true, 0, 0, 0, 0, 255);
						}
					}
				}
			} catch (CoreException ce) {
				throw new IllegalStateException(ce);
			} catch (IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}
	}

	protected Path resolve(IFile file, String name) {
		return file.getLocation().toPath().getParent().resolve("data/" + name);
	}

}
