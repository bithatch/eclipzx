package uk.co.bithatch.tnfs.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

/**
 * Manages TNFS client mounts per project.
 * Mount configs are stored as project persistent properties.
 * Passwords are stored in Eclipse Secure Storage.
 * Mounts are realized as linked folders under "TNFS Mounts" using the EFS provider.
 */
public class TNFSMountManager {

	private static final QualifiedName MOUNTS_KEY = new QualifiedName(TNFSActivator.PLUGIN_ID, "clientMounts");
	static final String MOUNTS_FOLDER = "TNFS Mounts";
	private static final String SECURE_NODE = "uk.co.bithatch.tnfs.eclipse";

	private static IResourceChangeListener resourceListener;

	/**
	 * Get all configured mounts for a project.
	 */
	public static List<TNFSClientMount> getMounts(IProject project) {
		try {
			var value = project.getPersistentProperty(MOUNTS_KEY);
			if (value == null || value.isBlank()) return new ArrayList<>();
			var result = new ArrayList<TNFSClientMount>();
			for (var entry : value.split("\n")) {
				if (!entry.isBlank()) {
					var m = TNFSClientMount.deserialize(entry.trim());
					if (m != null) result.add(m);
				}
			}
			return result;
		} catch (CoreException e) {
			TNFSActivator.LOG.error("Failed to read TNFS mounts", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Save mount configurations only (does not create/remove linked folders).
	 */
	public static void saveMounts(IProject project, List<TNFSClientMount> mounts) {
		try {
			var sb = new StringBuilder();
			for (var m : mounts) {
				sb.append(m.serialize()).append("\n");
			}
			project.setPersistentProperty(MOUNTS_KEY, sb.toString());
		} catch (CoreException e) {
			TNFSActivator.LOG.error("Failed to save TNFS mounts", e);
		}
	}

	/**
	 * Save mounts and refresh linked folders (removes stale links, updates changed URIs).
	 */
	public static void setMounts(IProject project, List<TNFSClientMount> mounts) {
		saveMounts(project, mounts);
		try {
			refreshLinkedFolders(project, mounts);
		} catch (CoreException e) {
			TNFSActivator.LOG.error("Failed to refresh linked folders", e);
		}
	}

	/**
	 * Check if a mount's linked folder currently exists.
	 */
	public static boolean isMounted(IProject project, TNFSClientMount mount) {
		var mountsFolder = project.getFolder(MOUNTS_FOLDER);
		if (!mountsFolder.exists()) return false;
		return mountsFolder.getFolder(mount.getName()).exists();
	}

	/**
	 * Mount a single entry (create its linked folder).
	 */
	public static void mount(IProject project, TNFSClientMount mount) throws CoreException {
		var monitor = new NullProgressMonitor();
		var mountsFolder = project.getFolder(MOUNTS_FOLDER);
		if (!mountsFolder.exists()) {
			mountsFolder.create(IFolder.VIRTUAL, true, monitor);
		}
		var linkedFolder = mountsFolder.getFolder(mount.getName());
		var uri = mount.toURI();
		if (linkedFolder.exists()) {
			if (!uri.equals(linkedFolder.getLocationURI())) {
				linkedFolder.delete(true, monitor);
				linkedFolder.createLink(uri, 0, monitor);
			}
		} else {
			linkedFolder.createLink(uri, 0, monitor);
		}
	}

	/**
	 * Unmount a single entry (remove its linked folder).
	 */
	public static void unmount(IProject project, TNFSClientMount mount) throws CoreException {
		var monitor = new NullProgressMonitor();
		var mountsFolder = project.getFolder(MOUNTS_FOLDER);
		if (!mountsFolder.exists()) return;
		var linkedFolder = mountsFolder.getFolder(mount.getName());
		if (linkedFolder.exists()) {
			linkedFolder.delete(true, monitor);
		}
		// Clean up empty mounts folder
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
					TNFSActivator.LOG.error("Failed to automount " + m.getName() + " in project " + project.getName(), e);
				}
			}
		}
	}

	/**
	 * Store password in Eclipse Secure Storage.
	 */
	public static void setPassword(TNFSClientMount mount, String password) {
		try {
			var node = getSecureNode();
			if (password == null || password.isEmpty()) {
				node.remove(mount.getName());
			} else {
				node.put(mount.getName(), password, true);
			}
			node.flush();
		} catch (Exception e) {
			TNFSActivator.LOG.error("Failed to store password in secure storage", e);
		}
	}

	/**
	 * Retrieve password from Eclipse Secure Storage.
	 */
	public static String getPassword(TNFSClientMount mount) {
		try {
			var node = getSecureNode();
			return node.get(mount.getName(), "");
		} catch (StorageException e) {
			TNFSActivator.LOG.error("Failed to read password from secure storage", e);
			return "";
		}
	}

	private static ISecurePreferences getSecureNode() {
		return SecurePreferencesFactory.getDefault().node(SECURE_NODE);
	}

	/**
	 * Refresh linked folders: remove stale links (config deleted), update changed URIs.
	 * Does NOT create new links — use {@link #mount(IProject, TNFSClientMount)} for that.
	 */
	public static void refreshLinkedFolders(IProject project, List<TNFSClientMount> mounts) throws CoreException {
		var monitor = new NullProgressMonitor();
		var mountsFolder = project.getFolder(MOUNTS_FOLDER);

		if (!mountsFolder.exists()) return;

		// Remove linked folders for mounts that no longer exist in config
		var configNames = mounts.stream().map(TNFSClientMount::getName).toList();
		for (var member : mountsFolder.members()) {
			if (!configNames.contains(member.getName())) {
				member.delete(true, monitor);
			}
		}

		// Update URIs for mounted entries whose config changed
		for (var m : mounts) {
			var linkedFolder = mountsFolder.getFolder(m.getName());
			if (linkedFolder.exists()) {
				var uri = m.toURI();
				if (!uri.equals(linkedFolder.getLocationURI())) {
					linkedFolder.delete(true, monitor);
					linkedFolder.createLink(uri, 0, monitor);
				}
			}
		}

		// Clean up empty mounts folder
		if (mountsFolder.exists() && mountsFolder.members().length == 0) {
			mountsFolder.delete(true, monitor);
		}
	}

	/**
	 * Install a workspace resource change listener that detects when linked
	 * folders under "TNFS Mounts" are deleted from the project explorer,
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

						// Check if the deleted resource is under "TNFS Mounts"
						var parent = resource.getParent();
						if (parent == null || !MOUNTS_FOLDER.equals(parent.getName())) return true;
						var project = resource.getProject();
						if (project == null || !project.isOpen()) return true;

						// The deleted folder name corresponds to a mount name
						var deletedName = resource.getName();
						var mounts = getMounts(project);
						var changed = mounts.removeIf(m -> m.getName().equals(deletedName));
						if (changed) {
							TNFSActivator.LOG.info("Mount '" + deletedName + "' removed (linked folder deleted)");
							saveMounts(project, mounts);
						}
						return false;
					}
				});
			} catch (CoreException e) {
				TNFSActivator.LOG.error("Error processing resource change for TNFS mounts", e);
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * Uninstall the resource change listener.
	 */
	public static void uninstallResourceListener() {
		if (resourceListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
			resourceListener = null;
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
}
