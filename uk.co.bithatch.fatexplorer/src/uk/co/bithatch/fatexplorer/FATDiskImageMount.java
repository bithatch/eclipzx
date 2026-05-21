package uk.co.bithatch.fatexplorer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.eclipse.core.resources.IProject;

/**
 * Represents a single FAT disk image mount configuration.
 * The image path can be project-relative (e.g. "Debug/test-image.img")
 * or absolute (e.g. "/home/user/disk.img").
 * <p>
 * Project-relative paths do not start with "/" or a drive letter.
 * Absolute paths start with "/" (Unix) or a drive letter (Windows).
 */
public class FATDiskImageMount {

	private String name;
	private String imagePath;
	private boolean automount;

	public FATDiskImageMount() {
		this.name = "";
		this.imagePath = "";
		this.automount = true;
	}

	public FATDiskImageMount(String name, String imagePath, boolean automount) {
		this.name = name;
		this.imagePath = imagePath;
		this.automount = automount;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public boolean isAutomount() {
		return automount;
	}

	public void setAutomount(boolean automount) {
		this.automount = automount;
	}

	/**
	 * Returns true if the image path is absolute (not project-relative).
	 */
	public boolean isAbsolute() {
		return imagePath.startsWith("/") || (imagePath.length() >= 2 && imagePath.charAt(1) == ':');
	}

	/**
	 * Build a fatimg:// URI for this mount.
	 * <p>
	 * Uses the same URI path convention as the original {@code FATPreferencesAccess.encodeToURI()}:
	 * <ul>
	 *   <li>Absolute paths: 4-slash prefix → {@code fatimg:////home/user/disk.img}</li>
	 *   <li>Project-relative paths: single-slash prefix, prepended with project name to be 
	 *       workspace-findable → {@code fatimg:/myproject/Debug/test.img}</li>
	 * </ul>
	 * This is critical for:
	 * <ul>
	 *   <li>{@code FATImageFileSystem.toDiskFile()} which does {@code getPath().substring(1)}</li>
	 *   <li>{@code FATLock} which uses {@code getPath()} as the lock key</li>
	 *   <li>{@code FATPreferencesAccess.addKeyToURI()/resolve()} which re-encode the path</li>
	 * </ul>
	 * 
	 * @param project the project, needed for project-relative paths (prepends project name)
	 */
	public URI toURI(IProject project) {
		try {
			String uriPath;
			if (isAbsolute()) {
				// Absolute: "///" + path → URI path = "////home/user/disk.img"
				uriPath = "///" + imagePath;
			} else {
				// Project-relative: "/" + projectName + "/" + path → workspace-relative
				uriPath = "/" + project.getName() + "/" + imagePath;
			}
			return new URI("fatimg", null, uriPath, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid disk image path", e);
		}
	}

	/**
	 * Build a fatimg:// URI without project context (for absolute paths only).
	 * @throws IllegalStateException if path is project-relative
	 */
	public URI toURI() {
		if (!isAbsolute()) {
			throw new IllegalStateException("Project-relative path '" + imagePath 
					+ "' requires a project context. Use toURI(IProject) instead.");
		}
		try {
			return new URI("fatimg", null, "///" + imagePath, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid disk image path", e);
		}
	}

	/**
	 * Serialize to a storable string: name|imagePath|automount
	 */
	public String serialize() {
		return String.join("|", name, imagePath, String.valueOf(automount));
	}

	/**
	 * Deserialize from stored string.
	 */
	public static FATDiskImageMount deserialize(String s) {
		var parts = s.split("\\|", -1);
		if (parts.length < 2) return null;
		var m = new FATDiskImageMount();
		m.name = parts[0];
		m.imagePath = parts[1];
		m.automount = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : true;
		return m;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof FATDiskImageMount other)) return false;
		return Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return name + " [" + imagePath + "]";
	}
}
