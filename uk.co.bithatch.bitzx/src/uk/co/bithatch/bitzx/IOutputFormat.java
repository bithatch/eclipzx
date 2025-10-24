package uk.co.bithatch.bitzx;

import java.util.Comparator;
import java.util.Optional;

public interface IOutputFormat extends IDescribed {

	default String extension() {
		return name().toLowerCase();
	}

	@Override
	default String fullDescription() {
		return description() + " (*." + extension() + ")";
	}

	default Optional<WellKnownOutputFormat> wellKnown() {
		try {
			return Optional.of(WellKnownOutputFormat.valueOf(name().toUpperCase()));
		} catch (IllegalArgumentException iae) {
			return Optional.empty();
		}
	}
	
	public static Comparator<IOutputFormat> comparator() {
		return new Comparator<IOutputFormat>() {
			
			@Override
			public int compare(IOutputFormat o1, IOutputFormat o2) {
				return o1.name().compareTo(o2.name());
			}
		};
	}

}
