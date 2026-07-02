package uk.co.bithatch.eclipz80.ui.preprocessing;

import java.util.List;
import java.util.stream.Stream;

import uk.co.bithatch.eclipz80.AsmStdlib;
import uk.co.bithatch.eclipzpp.ui.PPReferenceIndex;

public class AsmReferenceIndex extends PPReferenceIndex {

	@Override
	public boolean isDefined(String offending) {
		if (AsmStdlib.get().isDefined(offending)) {
			return true;
		}
		return super.isDefined(offending);
	}

	@Override
	public List<String> definitions() {
		return Stream.concat(
			AsmStdlib.get().definitions().stream(), 
			super.definitions().stream()
		).toList();
	}

}
