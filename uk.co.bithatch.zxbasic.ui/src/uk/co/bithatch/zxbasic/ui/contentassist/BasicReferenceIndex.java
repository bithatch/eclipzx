package uk.co.bithatch.zxbasic.ui.contentassist;

import java.util.List;

import uk.co.bithatch.zxbasic.IReferenceIndex;
import uk.co.bithatch.zxbasic.interpreter.ZXStdlib;
import uk.co.bithatch.zxbasic.scoping.SourceMapRegistry;

public class BasicReferenceIndex implements IReferenceIndex {
	
	private final static IReferenceIndex DEFAULT = ZXStdlib.get();

	@Override
	public boolean isDefined(String offending) {
		if(DEFAULT.isDefined(offending))
			return true;


		var map = SourceMapRegistry.get();
		if(map != null && map.defines().containsKey(offending)) {
			return true;
		}
		
		return false;
	}

	@Override
	public List<String> definitions() {
		return DEFAULT.definitions();
	}

}
