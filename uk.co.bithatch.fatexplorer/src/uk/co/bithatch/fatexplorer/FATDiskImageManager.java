package uk.co.bithatch.fatexplorer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;

import uk.co.bithatch.fatexplorer.vfs.FATImageFileSystem;

/**
 * Manages FAT disk image mounts per project.
 * Mount configs are stored as project persistent properties.
 * Mounts are realized as linked folders under "FAT Disk Images" using the EFS fatimg:// provider.
 */
public class FATDiskImageManager {

	private static final ILog LOG = ILog.of(FATDiskImageManager.class);
	private static final QualifiedName MOUNTS_KEY = new QualifiedName(Activator.PLUGIN_ID, "diskImageMounts");
	public static final String MOUNTS_FOLDER = "FAT Disk Images";

	private static IResourceChangeListener resourceListener;
	private static IResourceChangeListener projectCloseListener;

	/**
	 * Get all configured disk image mounts for a project.
	 */
	public static List<FATDiskImageMount> getMounts(IProject project) {
		try {
			var value = project.getPersistentProperty(MOUNTS_KEY);
			if (value == null || value.isBlank()) return new ArrayList<>();
			var result = new ArrayList<FATDiskImageMount>();
			for (var entry : value.split("\n")) {
				if (!entry.isBlank()) {
					var m = FATDiskImageMount.deserialize(entry.trim());
					if (m != null) result.add(m);
				}
			}
			return result;
		} catch (CoreException e) {
			LOG.error("Failed to read FAT disk image mounts", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Save mount configurations only (does not create/remove linked folders).
	 */
	public static void saveMounts(IProject project, List<FATDiskImageMount> mounts) {
		try {
			var sb = new StringBuilder();
			for (var m : mounts) {
				sb.append(m.serialize()).append("\n");
			}
			project.setPersistentProperty(MOUNTS_KEY, sb.toString());
		} catch (CoreException e) {
			LOG.error("Failed to save FAT disk image mounts", e);
		}
	}

	/**
	 * Save mounts and refresh linked folders.
	 */
	public static void setMounts(IProject project, List<FATDiskImageMount> mounts) {
		saveMounts(project, mounts);
		try {
			refreshLinkedFolders(project, mounts);
		} catch (CoreException e) {
			LOG.error("Failed to refresh linked folders", e);
		}
	}

	/**
	 * Check if a mount's linked folder currently exists.
	 */
	public static boolean isMounted(IProject project, FATDiskImageMount mount) {
		var mountsFolder = project.getFolder(MOUNTS_FOLDER);
		if (!mountsFolder.exists()) return false;
		return mountsFolder.getFolder(mount.getName()).exists();
	}

	/**
	 * Mount a single entry (create its linked folder).
	 * If the disk image file does not exist on disk, the mount is skipped
	 * (the linked folder is not created). This prevents errors during
	 * automount when images have been deleted.
	 */
	public static void mount(IProject project, FATDiskImageMount mount) throws CoreException {
		mount(project, mount, null);
	}

	/**
	 * Mount a single entry (create its linked folder).
	 * If the disk image file does not exist on disk, the mount is skipped
	 * (the linked folder is not created). This prevents errors during
	 * automount when images have been deleted.
	 *
	 * @param project the project
	 * @param mount the mount config
	 * @param lockedUri optional keyed URI (from {@link FATLock#lockImage}); when
	 *        non-null this URI is used for {@code createLink} so that the internal
	 *        {@code EFS.getStore()} call sees the lock key and is allowed through.
	 */
	public static void mount(IProject project, FATDiskImageMount mount, URI lockedUri) throws CoreException {
		var diskFile = FATImageFileSystem.toDiskFile(mount.toURI(project));
		if (!diskFile.exists()) {
			LOG.info("Skipping mount '" + mount.getName() + "' — disk image does not exist: " + diskFile);
			return;
		}

		var monitor = new NullProgressMonitor();
		var mountsFolder = project.getFolder(MOUNTS_FOLDER);
		if (!mountsFolder.exists()) {
			mountsFolder.create(IFolder.VIRTUAL, true, monitor);
		}
		var linkedFolder = mountsFolder.getFolder(mount.getName());
		var uri = mount.toURI(project);
		var linkUri = lockedUri != null ? lockedUri : uri;
		if (linkedFolder.exists()) {
			if (!uri.equals(linkedFolder.getLocationURI())) {
				linkedFolder.delete(true, monitor);
				linkedFolder.createLink(linkUri, IResource.BACKGROUND_REFRESH, monitor);
			}
		} else {
			linkedFolder.createLink(linkUri, IResource.BACKGROUND_REFRESH, monitor);
		}
	}

	/**
	 * Unmount a single entry (remove its linked folder).
	 */
	public static void unmount(IProject project, FATDiskImageMount mount) throws CoreException {
		var monitor = new NullProgressMonitor();
		var mountsFolder = project.getFolder(MOUNTS_FOLDER);
		if (!mountsFolder.exists()) return;
		var linkedFolder = mountsFolder.getFolder(mount.getName());
		if (linkedFolder.exists()) {
			linkedFolder.delete(true, monitor);
		}
		if (mountsFolder.exists() && mountsFolder.members().length == 0) {
			mountsFolder.delete(true, monitor);
		}
	}

	/**
	 * Mount all mounts that have automount enabled for a project.
	 */
	public static void mountAutomounts(IProject project) {
		var mounts = getMounts(project);
		for (var m : mounts) {
			if (m.isAutomount()) {
				try {
					mount(project, m);
				} catch (CoreException e) {
					LOG.error("Failed to automount " + m.getName() + " in project " + project.getName(), e);
				}
			}
		}
	}

	/**
	 * Refresh linked folders: remove stale links, update changed URIs.
	 */
	public static void refreshLinkedFolders(IProject project, List<FATDiskImageMount> mounts) throws CoreException {
		var monitor = new NullProgressMonitor();
		var mountsFolder = project.getFolder(MOUNTS_FOLDER);

		if (!mountsFolder.exists()) return;

		var configNames = mounts.stream().map(FATDiskImageMount::getName).toList();
		for (var member : mountsFolder.members()) {
			if (!configNames.contains(member.getName())) {
				member.delete(true, monitor);
			}
		}

		for (var m : mounts) {
			var linkedFolder = mountsFolder.getFolder(m.getName());
			if (linkedFolder.exists()) {
				var uri = m.toURI(project);
				if (!uri.equals(linkedFolder.getLocationURI())) {
					linkedFolder.delete(true, monitor);
					linkedFolder.createLink(uri, IResource.BACKGROUND_REFRESH, monitor);
				}
			}
		}

		if (mountsFolder.exists() && mountsFolder.members().length == 0) {
			mountsFolder.delete(true, monitor);
		}
	}

	/**
	 * Install a workspace resource change listener that detects when linked
	 * folders under "FAT Disk Images" are deleted from the project explorer,
	 * and removes the corresponding mount configuration.
	 */
	public static void installResourceListener() {
		if (resourceListener != null) return;
		resourceListener = event -> {
			if (event.getType() != IResourceChangeEvent.POST_CHANGE) return;
			var delta = event.getDelta();
			if (delta == null) return;
			try {
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta d) throws CoreException {
						if (d.getKind() != IResourceDelta.REMOVED) return true;
						var resource = d.getResource();
						if (!(resource instanceof IFolder)) return true;

						var parent = resource.getParent();
						if (parent == null || !MOUNTS_FOLDER.equals(parent.getName())) return true;
						var project = resource.getProject();
						if (project == null || !project.isOpen()) return true;

						var deletedName = resource.getName();
						var mounts = getMounts(project);
						var changed = mounts.removeIf(m -> m.getName().equals(deletedName));
						if (changed) {
							LOG.info("Disk image mount '" + deletedName + "' removed (linked folder deleted)");
							saveMounts(project, mounts);
						}
						return false;
					}
				});
			} catch (CoreException e) {
				LOG.error("Error processing resource change for FAT disk image mounts", e);
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);

		// Listen for project close/delete to release file handles
		projectCloseListener = event -> {
			if (event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
				var resource = event.getResource();
				if (resource instanceof IProject project) {
					var location = project.getLocation();
					if (location != null) {
						try {
							var fs = (FATImageFileSystem) org.eclipse.core.filesystem.EFS.getFileSystem("fatimg");
							fs.closeStoresForProject(location.toFile());
						} catch (CoreException e) {
							LOG.error("Failed to get fatimg filesystem to close stores for project " + project.getName(), e);
						}
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(projectCloseListener,
				IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
	}

	/**
	 * Uninstall the resource change listener.
	 */
	public static void uninstallResourceListener() {
		if (resourceListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
			resourceListener = null;
		}
		if (projectCloseListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(projectCloseListener);
			projectCloseListener = null;
		}
	}

	/**
	 * Auto-mount all configured automount entries for all open projects.
	 */
	public static void mountAllAutomounts() {
		for (var project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (project.isOpen()) {
				mountAutomounts(project);
			}
		}
	}

	/**
	 * Refresh any linked folders across all open projects that point to the given
	 * disk image URI. Called when an image is unlocked so the project explorer
	 * re-reads the now-accessible content (replacing the placeholder that was
	 * shown while locked).
	 */
	public static void refreshLinkedFoldersForImage(java.net.URI imageUri) {
		var diskFile = FATImageFileSystem.toDiskFile(imageUri);
		var diskPath = diskFile.getAbsolutePath();
		for (var project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (!project.isOpen()) continue;
			var mountsFolder = project.getFolder(MOUNTS_FOLDER);
			for (var mount : getMounts(project)) {
				try {
					var mountDisk = FATImageFileSystem.toDiskFile(mount.toURI(project));
					if (mountDisk.getAbsolutePath().equals(diskPath)) {
						// Refresh the linked folder so its content is re-read
						if (mountsFolder.exists()) {
							var linkedFolder = mountsFolder.getFolder(mount.getName());
							if (linkedFolder.exists()) {
								LOG.info("Refreshing linked folder '" + mount.getName() + "' after lock state change");
								linkedFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
							}
						}
						// Also refresh the .img file itself so the label provider
						// can update its icon (locked/unlocked)
						if (!mount.isAbsolute()) {
							var imgFile = project.getFile(mount.getImagePath());
							if (imgFile.exists()) {
								imgFile.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
							}
						}
					}
				} catch (Exception e) {
					LOG.error("Failed to refresh linked folder for mount " + mount.getName(), e);
				}
			}
		}
		
		// Update the folder decorator so lock overlay icons are refreshed
		try {
			var display = org.eclipse.swt.widgets.Display.getDefault();
			if (display != null && !display.isDisposed()) {
				display.asyncExec(() -> {
					try {
						org.eclipse.ui.PlatformUI.getWorkbench().getDecoratorManager()
							.update("uk.co.bithatch.fatexplorer.diskImageFolderDecorator");
					} catch (Exception e) {
						// Workbench may not be available yet
					}
				});
			}
		} catch (Exception e) {
			// Ignore if display/workbench not available
		}
	}
}
