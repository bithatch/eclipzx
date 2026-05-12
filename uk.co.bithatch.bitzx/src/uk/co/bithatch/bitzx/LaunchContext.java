package uk.co.bithatch.bitzx;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.ILaunchConfiguration;

public class LaunchContext implements Closeable {
	private static final ThreadLocal<LaunchContext> currentConfig = new ThreadLocal<>();

	public static final String BINARY_FILE = "attr.binaryFile";

	private final ILaunchConfiguration config;
	private List<Path> tempFiles = new ArrayList<>();
	private Map<String, Object> attributes = new HashMap<>();

	private LaunchContext(ILaunchConfiguration config) {
		this.config = config;
	}

	public Object attr(String key) {
		return  attributes.get(key);
	}
	
	public Object attr(String key, Object val) {
		return attributes.put(key, val);
	}

	public ILaunchConfiguration config() {
		return config;
	}

	public Path tempFile(String suffix) {
		try {
			var tmpfile = Files.createTempFile("bitzx", suffix);
			tempFiles.add(tmpfile);
			
			// TEMP
			tmpfile.toFile().deleteOnExit();
			return tmpfile;
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}
	
	@Override
	public void close() {
		tempFiles.forEach(f -> {
			try {
				Files.delete(f);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		tempFiles.clear();
	}

	public static LaunchContext set(ILaunchConfiguration config) {
		var ctx = new LaunchContext(config);
		currentConfig.set(ctx);
		return ctx;
	}

	public static LaunchContext get() {
		return currentConfig.get();
	}

	public static void clear() {
		currentConfig.remove();
	}
}
