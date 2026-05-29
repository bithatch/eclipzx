package uk.co.bithatch.eclipz80.ui.language;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.WellKnownArchitecture;

public enum AsmArchitecture implements IArchitecture {
	ZX;

	public String description() {
		switch (this) {
		case ZX:
			return "All ZX Spectrum Models";
		default:
			return name();
		}
	}

	@Override
	public Optional<WellKnownArchitecture> wellKnown() {
		return Optional.of(WellKnownArchitecture.valueOf(name()));
	}

	@Override
	public List<IOutputFormat> supportedFormats() {
		return Arrays.asList(AsmOutputFormat.BIN);
	}

}
