package uk.co.bithatch.drawzx.editor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.QualifiedName;

import uk.co.bithatch.drawzx.Activator;

public class EditorFileProperties {

	public enum PaletteSource {
		DEFAULT, DEFAULT_TRANSPARENT, FILE
	}

	public enum SpritePaintMode {
		DRAW, SELECT
	}

	public enum ScreenPaintMode {
		BRUSH, ERASE, FILL, SELECT;
		
		public String description() {
			switch(this) {
			case BRUSH:
				return "Brush";
			case ERASE:
				return "Erase";
			case FILL:
				return "Fill";
			case SELECT:
				return "Select";
			default:
				return name();
			}
		}
	}

	public static QualifiedName PALETTE_SOURCE_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "paletteSource");
	public static QualifiedName PALETTE_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "palettePath");
	public static QualifiedName EDITOR_MODE_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "mode");
	public static QualifiedName PALETTE_HISTORY_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "paletteHistory");
	public static QualifiedName PALETTE_PRIMARY_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "palettePrimary");
	public static QualifiedName PALETTE_SECONDARY_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "paletteSecondary");
	public static QualifiedName SPRITE_INDEX_PROPERTY = new QualifiedName(Activator.PLUGIN_ID, "spriteIndex");
	
	public static int getIntProperty(IFile file, QualifiedName key) {
		return getIntProperty(file, key, 0);
	}
	
	public static int getIntProperty(IFile file, QualifiedName key, int defaultValue) {
		try {
			return Integer.parseInt(getProperty(file, key, String.valueOf(defaultValue)));
		}
		catch(NumberFormatException nfe) {
			return defaultValue;
		}
	}

	public static String getProperty(IFile file, QualifiedName key) {
		return getProperty(file, key, null);
	}

	public static String getProperty(IFile file, QualifiedName key, String defaultValue) {
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

	public static Set<String> getSetProperty(IFile file, QualifiedName key, Set<String> defaultValue) {
		var val = getProperty(file, key, String.join(";", defaultValue));
		if (val.equals(""))
			return Collections.emptySet();
		else
			return Arrays.asList(val.split(";")).stream().collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
	}

	public static void setSetProperty(IFile file, QualifiedName key, List<String> value) {
		setProperty(file, key, String.join(";", value));
	}

	public static void addToSetProperty(IFile file, QualifiedName key, String value) {
		addToSetProperty(file, key, value, true);
	}

	public static void addToSetProperty(IFile file, QualifiedName key, String value, boolean first) {
		addToSetProperty(file, key, value, first, -1);
	}

	public static void addToSetProperty(IFile file, QualifiedName key, String value, boolean first, int maxItems) {
		try {
			var lst = new LinkedHashSet<>(getSetProperty(file, key, Collections.emptySet()));
			
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

	public static void setProperty(IFile file, QualifiedName key, int value) {
		setProperty(file, key, String.valueOf(value));
	}

	public static void setProperty(IFile file, QualifiedName key, String value) {
		try {
			var was = getProperty(file, key);
			if (!Objects.equals(was, value)) {
				file.setPersistentProperty(key, value);
				file.touch(null); // resource change events wont fire without this
			}
		} catch (Exception e1) {
			throw new IllegalArgumentException(e1);
		}
	}

	public static PaletteSource paletteSource(IFile file) {
		try {
			return PaletteSource.valueOf(EditorFileProperties.getProperty(file,
					EditorFileProperties.PALETTE_SOURCE_PROPERTY, PaletteSource.DEFAULT_TRANSPARENT.name()));
		} catch (Exception e) {
			return PaletteSource.DEFAULT_TRANSPARENT;
		}
	}
}
