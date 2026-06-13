package uk.co.bithatch.eclipz80.ui.preprocessing;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import uk.co.bithatch.eclipzpp.ui.PPReferenceIndex;

public class AsmReferenceIndex extends PPReferenceIndex {
	
	private Set<String> BUILT_INS = Set.of("limit", "$");
	
	@Override
	public boolean isDefined(String offending) {
		System.out.println("ASM isDefined " + offending);
		if(BUILT_INS.contains(offending.toLowerCase())) {
			return true;
		}
		return super.isDefined(offending);
	}

	@Override
	public List<String> definitions() {
		return Stream.concat(BUILT_INS.stream(), super.definitions().stream()).toList();
	}

}
