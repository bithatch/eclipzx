package uk.co.bithatch.zxbasic;

import java.util.List;

public interface IReferenceIndex {

	boolean isDefined(String referenceId);

	List<String> definitions();

}
