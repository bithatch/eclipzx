package uk.co.bithatch.zxbasic.ui.contentassist;

import java.util.List;
import java.util.stream.Stream;

import uk.co.bithatch.eclipzpp.IReferenceIndex;
import uk.co.bithatch.eclipzpp.ui.PPReferenceIndex;
import uk.co.bithatch.zxbasic.interpreter.ZXStdlib;

public class BasicReferenceIndex extends PPReferenceIndex {
	
	private final static IReferenceIndex DEFAULT = ZXStdlib.get();

	@Override
	public boolean isDefined(String offending) {
		System.out.println("BASIC isDefined " + offending);
		if(DEFAULT.isDefined(offending))
			return true;

		return super.isDefined(offending);
	}

	@Override
	public List<String> definitions() {
		return Stream.concat(
				super.definitions().stream(),
				DEFAULT.definitions().stream()).
				distinct().
				toList();
	}

}
