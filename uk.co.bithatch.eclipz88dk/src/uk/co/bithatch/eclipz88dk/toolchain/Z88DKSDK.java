package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;


public record Z88DKSDK(String name, File location) {

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
		return new Z88DKConfigurations(location.toPath().resolve("lib").resolve("config"));
	}
}
