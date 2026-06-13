package uk.co.bithatch.bitzx;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;

public class FileNames {
	
	public static List<Path> findAllChildDirectories(Path dir) {
		try {
			return Files.walk(dir).filter(Files::isDirectory).toList();
		}
		catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static String stripExtension(String path) {
		var idx = path.lastIndexOf('.');
		return idx == -1 ? path : path.substring(0, idx);
	}

	public static boolean hasExtensions(File f, String... extensions) {
		var ext = getExtension(f);
		return Arrays.asList(extensions).stream().map(String::toLowerCase).filter(s -> s.equals(ext)).findFirst().isPresent();
	}

	public static boolean hasExtensions(Path p, String... extensions) {
		var ext = getExtension(p);
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

	public static String getExtension(Path file) {
		return getExtension(file.getFileName().toString());
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

	public static Path changeExtension(Path file, String newExtension) {
		Path parent = file.getParent();
		if(parent == null)
			return Paths.get(changeExtension(file.getFileName().toString(), newExtension));
		else
			return parent.resolve(changeExtension(file.getFileName().toString(), newExtension));
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
		var paths = systemPaths();
		for(var c : cmd) {
			if (Platform.getOS().equals(Platform.OS_WIN32) && !c.toLowerCase().endsWith(".exe")) {
				c += ".exe";
			}
			for(var path : paths) {
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
