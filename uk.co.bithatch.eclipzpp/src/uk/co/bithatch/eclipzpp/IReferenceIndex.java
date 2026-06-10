package uk.co.bithatch.eclipzpp;

import java.util.List;

public interface IReferenceIndex {

	boolean isDefined(String referenceId);

	List<String> definitions();

}
