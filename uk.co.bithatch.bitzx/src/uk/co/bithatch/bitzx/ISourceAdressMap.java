package uk.co.bithatch.bitzx;

import java.util.Map;
import java.util.NavigableMap;


public interface ISourceAdressMap {

	int getAddress(String fileName, int line);

	SourceLocation getSourceLocation(int address);

	NavigableMap<Integer, SourceLocation> getAddressToLineMap();

	Map<SourceLocation, Integer> getLineToAddressMap();

	int getSymbolAddress(String symbolName);

	boolean hasDebugInfo();
}
