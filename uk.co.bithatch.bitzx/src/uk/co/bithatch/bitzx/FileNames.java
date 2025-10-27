package uk.co.bithatch.bitzx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;

public class FileNames {

	public static boolean hasExtensions(File f, String... extensions) {
		var ext = getExtension(f);
		return Arrays.asList(extensions).stream().map(String::toLowerCase).filter(s -> s.equals(ext)).findFirst().isPresent();
	}

	public static boolean hasExtensions(String name, String... extensions) {
		var ext = getExtension(name);
		return Arrays.asList(extensions).stream().map(String::toLowerCase).filter(s -> s.equals(ext)).findFirst().isPresent();
	}

	public static String getExtensionOrFilename(String filename) {
		var x = getExtension(filename);
		return x == null ? filename : x;
	}
	
	public static String getExtension(File file) {
		return getExtension(file.getName());
	}
	public static String getExtension(String filename) {
		var i = filename.lastIndexOf('.');
		if(i == -1)
			return null;
		else
			return filename.substring(i + 1);
	}

	public static File changeExtension(File file, String newExtension) {
		File parent = file.getParentFile();
		if(parent == null)
			return new File(changeExtension(file.getName(), newExtension));
		else
			return new File(parent, changeExtension(file.getName(), newExtension));
	}

	public static String changeExtension(String filename, String newExtension) {
		var idx = filename.lastIndexOf('.');
		return ( idx == -1 ? filename : filename.substring(0, idx)) + "." + newExtension ;
	}
	
	public static boolean isNeedsProcessing(File srcfile, File binfile) {
		
		if(srcfile.exists()) {
			if(binfile == null)
				return true;
			var srcmod = srcfile.lastModified();
			var binmod = binfile.exists() ? binfile.lastModified() : -1;
			return binmod == -1 || srcmod / 1000 != binmod / 1000;
		}
		else {
			return false;
		}
	}
	
	public static File targetFile(File srcfile, File outdir, String newFilename) {
		return targetFile(srcfile, outdir, null, newFilename);
	}
	
	public static File targetFile(File srcfile, File outdir, File workingdir, String newFilename) {
		if(outdir != null) {
			var dir = (workingdir == null ? outdir: workingdir).toPath();
			var rel = dir.getParent().relativize(srcfile.toPath()).getParent();
			if(rel != null) {
				outdir = new File(outdir, rel.toString());
			}
			return new File(outdir, newFilename);
		} else {
			return new File(srcfile.getParentFile(), changeExtension(srcfile.getName(), newFilename));
		}
	}

	public static Path findCommand(String... cmd) throws IOException {
		for(var c : cmd) {
			for(var path : systemPaths()) {
				if (Platform.getOS().equals(Platform.OS_WIN32) && !c.toLowerCase().endsWith(".exe")) {
					c += ".exe";
				}
				var fullPath = Paths.get(path).resolve(c);
				if(Files.exists(fullPath)) {
					return fullPath;
				}
			}
		}
		throw new IOException("Could not find command " +cmd + " on the PATH");
	}

	public static List<String> systemPaths() {
		return Arrays.asList(System.getenv("PATH").split(File.pathSeparator));
	}
}
