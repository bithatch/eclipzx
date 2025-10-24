package uk.co.bithatch.nextbuild;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import uk.co.bithatch.zxbasic.ui.api.IProgramBuildOptions;
import uk.co.bithatch.zxbasic.ui.api.IProgramBuildOptionsFactory;

public class FileHeaderProgramBuildOptionsFactory implements IProgramBuildOptionsFactory {

	@Override
	public Optional<IProgramBuildOptions> buildOptionsFor(IFile file) {
		try {
			if(file.getProject().hasNature(NextBuildNature.NATURE_ID)) {
				try(var rdr = new BufferedReader(new InputStreamReader(file.getContents()))) {
					var line = 0;
					String str = null;
					var bldr = new IProgramBuildOptions.Builder();
					while(line++ < 256 && ( str = rdr.readLine()) != null) {
						var arr = str.trim().split("\\s+");
						if(arr.length == 0)
							continue;
						var arg = arr[0];
						if(arg.toLowerCase().startsWith("'org=")) {
							bldr.withOrgAddress(Integer.parseInt(arg.split("=")[1]));
						}
						if(arg.toLowerCase().startsWith("'heap=")) {
							bldr.withOrgAddress(Integer.parseInt(arg.split("=")[1]));
						}
						// TODO more!
					}
					return Optional.of(bldr.build());
				}
			}
			else {
				return Optional.empty();
			}
		}
		catch(CoreException ce) {
			throw new IllegalStateException(ce);
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

}
