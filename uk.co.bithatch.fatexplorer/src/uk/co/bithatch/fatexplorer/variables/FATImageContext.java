package uk.co.bithatch.fatexplorer.variables;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.ILog;


public class FATImageContext {

	private final static ILog LOG = ILog.of(FATImageContext.class);
	
	private static final ThreadLocal<Map<String, String>> currentConfig = new ThreadLocal<>();
	
	public final static String IMAGE = "fat_image";
	public final static String DEFAULT = "fat_default";
	public final static String FOLDER = "fat_folder";

	public static void set(Map<String, String> config) {
		currentConfig.set(config);
	}

	public static void set(String key, String value) {
		LOG.info(String.format("Setting %s=%s for disk image context %s", key, value, Thread.currentThread().getName()));
		var map = get();
		if(map == null) {
			map = new HashMap<String, String>();
			set(map);
		}
		map.put(key, value);
	}

	public static String get(String key, String defaultValue) {
		var map = get();
		if(map != null && map.containsKey(key)) {
			return map.get(key);
		}
		else {
			return defaultValue;
		}
	}

	public static Map<String, String> get() {
		return currentConfig.get();
	}

	public static void clear() {
		currentConfig.remove();
	}
}
