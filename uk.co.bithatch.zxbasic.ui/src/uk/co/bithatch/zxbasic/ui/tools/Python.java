package uk.co.bithatch.zxbasic.ui.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;

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
		var l = pythonPaths();
		if(!l.isEmpty()) {
			return l.get(0);
		}
		
		throw new IllegalStateException("Could not find system python interpreter.");
	}
	
	public List<Path> pythonPaths() {
		var l  = new ArrayList<Path>();
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			for(var path : Stream.concat(
					FileNames.systemPaths().stream(),
					Arrays.asList(
						"%ProgramFiles%\\Python",
						"%ProgramFiles%(x86)\\Python",
						"%UserHome%\\AppData\\Local\\Programs\\Python").stream()).toList()) {
				var ppath = Paths.get(processPath(path));
				if(Files.exists(ppath)) {
					/* Look either here, or in sub directories (assuming they are versions) */
					for(var n : PYTHON_WINDOWS_NAMES) {
						var r = ppath.resolve(n);
						if(Files.exists(r)) {
							l.add(r); 
						}
					}
					
					try {
						for(var dir : Files.list(ppath).filter(Files::isDirectory).toList()) {
							for(var n : PYTHON_WINDOWS_NAMES) {
								var r = dir.resolve(n);
								if(Files.exists(r)) {
									l.add(r); 
								}
							}		
						}
					}
					catch(IOException ioe) {
						//
					}
				}
			}
		}
		else {
			for(var path : FileNames.systemPaths().stream().map(Paths::get).toList()) {
				if(Files.exists(path)) {
					for(var n : PYTHON_NAMES) {
						var r = path.resolve(n);
						if(Files.exists(r)) {
							l.add(r); 
						}
					}
				}
			}
		}
		return l;
	}
	
	private final static String[] PYTHON_NAMES = new  String[] { "python3", "python" };
	private final static String[] PYTHON_WINDOWS_NAMES = new  String[] { "pythonw.exe", "python.exe" };
	
	private static String envOr(String key, String def) {
		var val = System.getenv(def);
		if(val == null)
			return def;
		return val;
	}
	
	private static String processPath(String path) {
		var programFiles = envOr("ProgramFiles", "C:\\Program Files");
		var programFilesX86 = envOr("ProgramFiles(x86)", "C:\\Program Files (x86)");
		var userHome = envOr("UserHome", System.getProperty("user.home", ""));
		return path.replace("%ProgramFiles%", programFiles).
					replace("%ProgramFiles(x86)%", programFilesX86).
					replace("%UserHome%", userHome);
	}
}
