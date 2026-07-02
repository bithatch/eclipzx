package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.ILog;

public class Z88DKConfigurations {
	
	private final static ILog LOG = ILog.of(Z88DKConfigurations.class);
	
	private Map<String, Z88DKConfigurationFile> configurations = new HashMap<>();


	public Z88DKConfigurations() {
	}
	
	public Z88DKConfigurations(Path dir) {
		try {
			for(var file : Files.list(dir).filter(f -> f.getFileName().toString().toLowerCase().endsWith(".cfg")).toList()) {
				var pf = file.getFileName().toString();
				var pl = pf.substring(0, pf.length() - 4);
				try {
					configurations.put(pl.toLowerCase(), new Z88DKConfigurationFile(file));
				}
				catch(Exception e) {
					LOG.error("Failed to load SDK.", e);
				}
			}
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	public Z88DKConfigurationFile get(String name) {
		return configuration(name).orElseThrow(() -> new IllegalArgumentException("No such platform as " + name));
	}
	
	public Optional<Z88DKConfigurationFile> configuration(String name) {
		return Optional.ofNullable(configurations.get(name));
	}
	
	public List<Z88DKConfigurationFile> configurations() {
		return configurations.values().stream().sorted((c1,c2) -> c1.name().compareTo(c2.name())).toList();
	}

	public String[][] getAllSystemsAsOptions() {
		return configurations().stream().map(sdk -> new String[] { sdk.name(), sdk.name() }).toList().toArray(new String[0][0]);
	}

	public String[][] getAllZXSystemsAsOptions() {
		return configurations().stream().
				filter(this::isZXSystem).
				map(sdk -> new String[] { sdk.name(), sdk.name() }).toList().toArray(new String[0][0]);
	}
	
	public boolean isZXSystem(Z88DKConfigurationFile system) {
		/* TODO ... really WellKnownArchitectures should be doing this */
		return system.name().toLowerCase().startsWith("zx");
	}

	public static void main(String[] args) {
		var zcccfg = System.getenv("ZCCCFG");
		var cfgs = new Z88DKConfigurations(Paths.get(zcccfg));
		for(var cfg : cfgs.configurations()) {
			System.out.println(cfg);
		}
	}
}
