package uk.co.bithatch.bitzx;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;

public abstract class AbstractResourceProperties {
	
	public interface Listener {
		void propertyChanged(QualifiedName name, String oldVal, String newVal);
	}
	
	private static final List<Listener> listeners = new CopyOnWriteArrayList<>();
	
	public static void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public static void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public static boolean getProperty(IResource file, QualifiedName key, boolean defaultValue) {
		try {
			return Boolean.parseBoolean(getProperty(file, key, String.valueOf(defaultValue)));
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}

	public static int getProperty(IResource file, QualifiedName key, int defaultValue) {
		try {
			return Integer.parseInt(getProperty(file, key, String.valueOf(defaultValue)));
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}

	public static String getProperty(IResource file, QualifiedName key, String defaultValue) {
		try {
			var val = file.getPersistentProperty(key);
			if (val == null)
				return defaultValue;
			else
				return val;
		} catch (Exception e1) {
			throw new IllegalArgumentException(e1);
		}
	}

	public static void setProperty(IResource file, QualifiedName key, boolean value) {
		setProperty(file, key, String.valueOf(value));
	}

	public static void setProperty(IResource file, QualifiedName key, int value) {
		setProperty(file, key, String.valueOf(value));
	}

	public static void setProperty(IResource file, QualifiedName key, String value) {
		try {
			var was = getProperty(file, key, (String) null);
			if (!Objects.equals(was, value)) {
				file.setPersistentProperty(key, value);
				file.touch(null); // resource change events wont fire without this
				listeners.forEach(l -> l.propertyChanged(key, was, value));
			}
		} catch (Exception e1) {
			throw new IllegalArgumentException(e1);
		}
	}

	public static Set<String> getProperty(IResource file, QualifiedName key, Set<String> defaultValue) {
		var val = getProperty(file, key, String.join(";", defaultValue));
		if (val.equals(""))
			return Collections.emptySet();
		else
			return Arrays.asList(val.split(";")).stream().collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
	}

	public static void setProperty(IResource file, QualifiedName key, List<String> value) {
		setProperty(file, key, String.join(";", value));
	}

	public static void addToProperty(IResource file, QualifiedName key, String value) {
		addToProperty(file, key, value, true);
	}

	public static void addToProperty(IResource file, QualifiedName key, String value, boolean first) {
		addToSetProperty(file, key, value, first, -1);
	}

	public static void addToSetProperty(IResource file, QualifiedName key, String value, boolean first, int maxItems) {
		try {
			var lst = new LinkedHashSet<>(getProperty(file, key, Collections.emptySet()));

			if (first)
				lst.addFirst(value);
			else
				lst.addLast(value);

			if (maxItems != -1) {
				while (lst.size() > maxItems) {
					if (first) {
						lst.removeLast();
					} else {
						lst.removeFirst();
					}
				}
			}

			setProperty(file, key, String.join(";", lst));
		} catch (Exception e1) {
			throw new IllegalArgumentException(e1);
		}
	}
}
