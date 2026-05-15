package uk.co.bithatch.eclipz88dk.toolchain;

import java.util.Optional;

import uk.co.bithatch.bitzx.IOutputFormat;

/**
 * Static context used to pass the desired output format from
 * {@link uk.co.bithatch.eclipz88dk.launch.CExternallyLaunchable#compileForLaunch}
 * into the CDT command-line generator ({@link Z88DKCmdLineGen}).
 * <p>
 * This uses a simple static volatile field rather than a thread-local because
 * CDT may invoke the command-line generator on a different thread. This is safe
 * because Eclipse serialises workspace builds under the workspace lock.
 * <p>
 * Call {@link #set(IOutputFormat)} before triggering the build and
 * {@link #clear()} afterwards (in a {@code finally} block).
 */
public final class Z88DKBuildContext {

	private static volatile IOutputFormat outputFormat;

	private Z88DKBuildContext() {
	}

	public static void set(IOutputFormat fmt) {
		outputFormat = fmt;
	}

	public static Optional<IOutputFormat> get() {
		return Optional.ofNullable(outputFormat);
	}

	public static void clear() {
		outputFormat = null;
	}
}