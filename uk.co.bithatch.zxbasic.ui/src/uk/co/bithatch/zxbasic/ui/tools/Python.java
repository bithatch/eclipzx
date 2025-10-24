package uk.co.bithatch.zxbasic.ui.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class Python {

	private final static class Defaults {
		private final static Python DEFAULT = new Python();
	}
	
	public static Python get() {
		return Defaults.DEFAULT;
	}
	
	private Python() {
	}
	
	public boolean isAvailable() {
		try {
			getInterpreter();
			return true;
		}
		catch(IllegalStateException ise) {
			return false;
		}
	}

	public Path getInterpreter() {
		var custom = ZXBasicPreferencesAccess.get().getPython();
		
		Path path = null;
		if(!custom.equals("")) {
			path = Paths.get(custom);
		}
		
		if(path == null || !Files.exists(path)) {
			return getSystemInterpreter().toAbsolutePath();
		}
		else {
			return path;
		}
	}
	
	public boolean hasSystemInterpreter() {
		try {
			getSystemInterpreter();
			return true;
		}
		catch(IllegalStateException iae) {
			return false;
		}
	}

	public Path getSystemInterpreter() {
		try {
			return FileNames.findCommand("python3", "python");
		} catch (IOException e) {
			throw new IllegalStateException("Could not find system python interprete." + e.getMessage());
		}
	}
}
