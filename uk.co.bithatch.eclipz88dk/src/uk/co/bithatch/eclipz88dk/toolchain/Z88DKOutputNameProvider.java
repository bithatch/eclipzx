package uk.co.bithatch.eclipz88dk.toolchain;

import org.eclipse.cdt.managedbuilder.core.IManagedOutputNameProvider;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Custom output name provider for the Z88DK compiler tool. Generates output
 * filenames that include the source extension to avoid collisions when a
 * project contains both {@code foo.c} and {@code foo.asm} — they produce
 * {@code foo_c.o} and {@code foo_asm.o} respectively.
 * <p>
 * Without this, CDT's default pattern rule ({@code %.o: ../%.c}) would
 * produce {@code foo.o} for both, and one would overwrite the other.
 */
public class Z88DKOutputNameProvider implements IManagedOutputNameProvider {

	@Override
	public IPath[] getOutputNames(ITool tool, IPath[] primaryInputNames) {
		if (primaryInputNames == null || primaryInputNames.length == 0) {
			return new IPath[0];
		}

		IPath[] outputs = new IPath[primaryInputNames.length];
		for (int i = 0; i < primaryInputNames.length; i++) {
			IPath input = primaryInputNames[i];
			String fileName = input.lastSegment();

			/* Strip the extension and build basename_ext.o */
			String ext = input.getFileExtension();
			String baseName = fileName;
			if (ext != null && !ext.isEmpty()) {
				baseName = fileName.substring(0, fileName.length() - ext.length() - 1);
			}

			String outputName;
			if (ext != null && !ext.isEmpty()) {
				outputName = baseName + "_" + ext.toLowerCase() + ".o";
			} else {
				outputName = baseName + ".o";
			}

			/* Return just the filename — CDT handles the directory mirroring
			 * (e.g. placing it under Debug/src/) itself */
			outputs[i] = new Path(outputName);
		}
		return outputs;
	}
}
