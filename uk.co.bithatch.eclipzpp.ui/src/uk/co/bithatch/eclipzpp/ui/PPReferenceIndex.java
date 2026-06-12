package uk.co.bithatch.eclipzpp.ui;

import java.util.Collections;
import java.util.List;

import uk.co.bithatch.eclipzpp.IReferenceIndex;
import uk.co.bithatch.eclipzpp.SourceMapRegistry;

public class PPReferenceIndex implements IReferenceIndex {
	
	@Override
	public boolean isDefined(String offending) {
		var map = SourceMapRegistry.get();
		if(map != null && map.defines().containsKey(offending)) {
			return true;
		}

		return false;
	}

	@Override
	public List<String> definitions() {
		var map = SourceMapRegistry.get();
		return map == null ? Collections.emptyList() : map.defines().keySet().stream().toList();
	}

}
