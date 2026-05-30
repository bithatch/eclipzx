package uk.co.bithatch.eclipz80.ui.language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import uk.co.bithatch.bitzx.ISourceAdressMap;
import uk.co.bithatch.bitzx.SourceLocation;

public class AsmSourceAddressMap implements ISourceAdressMap {
	
	private Map<String, TreeMap<Integer, Integer>> lineToAddressMaps = new HashMap<>();
	private TreeMap<Integer, SourceLocation> addressToLineMaps = new TreeMap<>();

	public AsmSourceAddressMap(Path file) throws IOException {
		try(var rdr = Files.newBufferedReader(file)) {
			String line;
			String filename = null;
			while( ( line = rdr.readLine()) != null) {
				var paths = line.split("\\|");
				if(filename == null || !paths[0].equals("")) {
					if(paths[0].equals("")) {
						throw new IOException("First line in map must have fileename.");
					}
					filename = paths[0];
				}
				
				lineToAddressMaps.computeIfAbsent(filename, (k) -> new TreeMap<>()).put(Integer.parseInt(paths[1]), Integer.parseInt(paths[2], 16));
				addressToLineMaps.put(Integer.parseInt(paths[2], 16),  new SourceLocation(filename, Integer.parseInt(paths[1])));
				
			}
		}
	}
	
	@Override
	public int getAddress(String fileName, int line) {
		var fmap = lineToAddressMaps.get(fileName);
		if(fmap == null) {
			return 0;
		}
		else {
			var en = fmap.floorEntry(line);
			return en == null ? 0 : en.getValue();
		}
	}

	@Override
	public SourceLocation getSourceLocation(int address) {
		var en = addressToLineMaps.floorEntry(address);
		return en == null ? null : en.getValue();
	}

	@Override
	public int getSymbolAddress(String symbolName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasDebugInfo() {
		return true;
	}

}
