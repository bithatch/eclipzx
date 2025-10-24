package uk.co.bithatch.bitzx;

import java.util.List;
import java.util.Optional;

public interface IArchitecture extends IDescribed {

	List<? extends IOutputFormat> supportedFormats();
	
	default Optional<IOutputFormat> outputFormat(WellKnownOutputFormat wellKnown) {
		for(var fmt : supportedFormats()) {
			if(wellKnown.equals(fmt.wellKnown().orElse(null))) {
				return Optional.of(fmt);
			}
		}
		return Optional.empty();
	}
	
	default Optional<IOutputFormat> outputFormat(String name) {
		for(var fmt : supportedFormats()) {
			if(fmt.name().equalsIgnoreCase(name)) {
				return Optional.of(fmt);
			}
		}
		return Optional.empty();
	}
	
	default Optional<WellKnownArchitecture> wellKnown() {
		try {
			return Optional.of(WellKnownArchitecture.valueOf(name().toUpperCase()));
		} catch (IllegalArgumentException iae) {
			return Optional.empty();
		}
	}
}
