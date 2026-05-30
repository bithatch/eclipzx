package uk.co.bithatch.bitzx;

public interface ISourceAdressMap {

	int getAddress(String fileName, int line);

	SourceLocation getSourceLocation(int address);

	int getSymbolAddress(String symbolName);

	boolean hasDebugInfo();
}
