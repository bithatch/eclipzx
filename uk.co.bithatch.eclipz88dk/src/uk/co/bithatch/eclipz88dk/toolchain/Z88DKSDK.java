package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.bitzx.ISDK;


public final class Z88DKSDK implements ISDK {
	private final static ILog LOG = ILog.of(Z88DKSDK.class);
	
	private final String name;
	private final File location;
	
	private Z88DKConfigurations configurations;

	public Z88DKSDK(String name, File location) {
		this.name = name;
		this.location = location;
	}
	
	public final String name() {
		return name;
	}

	public final File location() {
		return location;
	}

	public static Z88DKSDK fromLocation(File location) {
		var chglog = new File(location, "changelog.txt");
		if(chglog.exists()) {
			try(var rdr = new BufferedReader(new FileReader(chglog))) {
				String line;
				while((line = rdr.readLine()) != null) {
					if(line.matches("z88dk\\s+v.*\\s+-.*")) {
						return new Z88DKSDK("Z88DK " + line.substring(6).split("\\s+")[0], location);
					}
				}
			}
			catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}

		return new Z88DKSDK("Unknown", location);
	}
	
	public Z88DKConfigurations configurations() {
		if(configurations == null) {
			try {
				configurations = new Z88DKConfigurations(location.toPath().resolve("lib").resolve("config"));
			}
			catch(Exception e) {
				LOG.error("Cannot load any configurations, does an SDK exist at all at " + location + "?");
				configurations = new Z88DKConfigurations();
			}
		}
		return configurations;
	}

	/**
	 * Returns the path to the z88dk-gdb binary in this SDK.
	 */
	public File z88dkGdb() {
		return new File(new File(location, "bin"), "z88dk-gdb");
	}

	/**
	 * Returns the path to the z80nm binary in this SDK.
	 */
	public File z80nm() {
		return new File(new File(location, "bin"), "z88dk-z80nm");
	}
}