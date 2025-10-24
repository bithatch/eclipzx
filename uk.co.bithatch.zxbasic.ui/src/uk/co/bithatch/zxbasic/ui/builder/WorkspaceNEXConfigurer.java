package uk.co.bithatch.zxbasic.ui.builder;

import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_ADDRESS;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BANK;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BUNDLE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BUNDLE_TYPE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_OTHER_TRIGGER_PROGRAMS;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_TRIGGER_PROGRAMS_IN_THIS_FOLDER;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_DO_NOT_SAVE_PALETTE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_USE_8_BIT_PALETTE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_BORDER;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_BAR_1;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_BAR_2;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_DELAY_1;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_BMP_DELAY_2;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.getProperty;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.zxbasic.tools.NexConverter.NexConfiguration;
import uk.co.bithatch.zxbasic.ui.api.BundleType;
import uk.co.bithatch.zxbasic.ui.api.INEXConfigurer;

public class WorkspaceNEXConfigurer implements INEXConfigurer {
	public final static ILog LOG = ILog.of(WorkspaceNEXConfigurer.class);

	@Override
	public void configure(IFile file, NexConfiguration nexConfiguration) throws CoreException {
		var project = file.getProject();
		project.accept(resource -> {
			if(resource instanceof IFile ifile &&
			   getProperty(ifile, NEX_BUNDLE, false)) {

				var match = false;
				
				/* The visited file is a candidate for bundling, check to see if it
				 * applies to the current program. First check if its in the same 
				 * folder
				 */
				var inFolder = getProperty(ifile, NEX_TRIGGER_PROGRAMS_IN_THIS_FOLDER, true);
				if(inFolder && file.getParent().getFullPath().equals(ifile.getParent().getFullPath())) {
					match = true;
				}
				
				/* Not in same folder, is it one of the listed programs? */
				var otherPaths = getProperty(ifile, NEX_OTHER_TRIGGER_PROGRAMS, Collections.emptySet());
				for(var p : otherPaths) {
					if(p.equals(file.getProjectRelativePath().toString())) {
						match = true;
						break;
					}
				}
				
				/* The file is a match, add it the NexConverter configuration */
				if(match) {
					var bankVal = emptyIfMinusOne(getProperty(file, NEX_BANK, -1));
					var addressVal = emptyIfMinusOne(getProperty(file, NEX_ADDRESS, -1));
				
					var bundleType = BundleType.valueOf(getProperty(ifile, NEX_BUNDLE_TYPE, BundleType.defaultForFilename(ifile.getName()).name()));
					switch(bundleType) {
					case BY_FILE_TYPE:
						if(ifile.getFileExtension().toLowerCase().equals("scr")) {
							LOG.info(String.format("Adding SCR file %s", ifile.getLocation().toPath()));
							nexConfiguration.scr(ifile.getLocation().toPath());
							break;
						}
						else if(ifile.getFileExtension().toLowerCase().equals("shc")) {
							LOG.info(String.format("Adding SHC file %s", ifile.getLocation().toPath()));
							nexConfiguration.shc(ifile.getLocation().toPath());
							break;
						}
						else if(ifile.getFileExtension().toLowerCase().equals("shr")) {
							LOG.info(String.format("Adding SHR file %s", ifile.getLocation().toPath()));
							nexConfiguration.shr(ifile.getLocation().toPath());
							break;
						}
						else if(ifile.getFileExtension().toLowerCase().equals("slr")) {
							LOG.info(String.format("Adding SLR file %s", ifile.getLocation().toPath()));
							nexConfiguration.slr(ifile.getLocation().toPath());
							break;
						}
						else if(ifile.getFileExtension().toLowerCase().equals("bmp")) {
							LOG.info(String.format("Adding BMP file %s", ifile.getLocation().toPath()));
							nexConfiguration.bmp(
									ifile.getLocation().toPath(), 
									getProperty(file, NEX_BMP_DO_NOT_SAVE_PALETTE, false), 
									getProperty(file, NEX_BMP_USE_8_BIT_PALETTE, false), 
									getProperty(file, NEX_BMP_BORDER, 0), 
									getProperty(file, NEX_BMP_BAR_1, 0), 
									getProperty(file, NEX_BMP_BAR_2, 0), 
									getProperty(file, NEX_BMP_DELAY_1, 0), 
									getProperty(file, NEX_BMP_DELAY_2, 0));
							break;
						}
						break;
					case FILE:
						LOG.info(String.format("Adding raw file %s to bank %d at %04x", ifile.getLocation().toPath(), bankVal, addressVal));
						nexConfiguration.addFile(ifile.getLocation().toPath(), bankVal, addressVal);
						break;
					case MMU:
						LOG.info(String.format("Adding MMU %s to bank %d at %04x", ifile.getLocation().toPath(), bankVal, addressVal));
						nexConfiguration.mmu(ifile.getLocation().toPath(), bankVal, addressVal);
						break;
					case EXTRA:
						LOG.info(String.format("Adding extra file %s", ifile.getLocation().toPath()));
						nexConfiguration.extraFile(ifile.getLocation().toPath());
						break;
					default:
						throw new UnsupportedOperationException();
					}
				}
				
			}
			return true;
		});
	}

	private Optional<Integer> emptyIfMinusOne(int val) {
		return val < 0 ? Optional.empty() : Optional.of(val);
	}

}
