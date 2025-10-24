package uk.co.bithatch.fatexplorer.vfs;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.*;

import java.io.*;

public class FileStoreCopyUtil {

	public static void copyFileToStore(File source, IFileStore target, boolean recursive, 
			FileOverwritePolicy overwritePolicy, IProgressMonitor monitor) throws IOException, CoreException {
		copyFileToStore(source, source.getName(), target, recursive, overwritePolicy, monitor);
	}

	public static void copyFileToStore(File source, String targetName, IFileStore target, boolean recursive, 
			FileOverwritePolicy overwritePolicy, IProgressMonitor monitor) throws IOException, CoreException {
		target = target.getChild(targetName);
		copyFileToStore(source, target, recursive, false, overwritePolicy, monitor);
	}
	
	public static void moveFileToStore(File source, IFileStore target, boolean recursive, 
			FileOverwritePolicy overwritePolicy, IProgressMonitor monitor) throws IOException, CoreException {
		target = target.getChild(source.getName());
		copyFileToStore(source, target, recursive, true, overwritePolicy, monitor);
	}

	private static void copyFileToStore(File source, IFileStore target, boolean recursive, boolean moveInsteadOfCopy,
			FileOverwritePolicy overwritePolicy, IProgressMonitor monitor) throws IOException, CoreException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		else
			monitor.subTask("Transferring files.");
		

		if (monitor.isCanceled())
			return;

		if (source.isDirectory()) {
			if (!recursive)
				return;

			var children = source.listFiles();
			monitor.beginTask((moveInsteadOfCopy ? "Moving" : "Copying") + ": " + source.getName(),
					children.length);
			
			if (children != null) {
				if(!target.fetchInfo().exists()) {
					target = target.mkdir(EFS.NONE, monitor);
				}
				for (var child : children) {
					var childTarget = target.getChild(child.getName());
					copyFileToStore(child, childTarget, true, moveInsteadOfCopy, overwritePolicy,
							monitor);
					monitor.worked(1);;
				}
			}

			if (moveInsteadOfCopy) {
				source.delete();
			}

		} else {

			monitor.beginTask((moveInsteadOfCopy ? "Moving" : "Copying") + ": " + source.getName(),
					(int)source.length());
			
			if (target.fetchInfo().exists()) {
				var decision = overwritePolicy.queryOverwrite(target.toURI().getPath(), moveInsteadOfCopy);
				if (decision == FileOverwritePolicy.Decision.CANCEL)
					throw new OperationCanceledException();
				if (decision == FileOverwritePolicy.Decision.SKIP)
					return;
			}

			try (var in = new BufferedInputStream(new FileInputStream(source));
					var out = new BufferedOutputStream(target.openOutputStream(0, monitor))) {
				var buffer = new byte[8192];
				int len;
				while ((len = in.read(buffer)) != -1) {
					if (monitor.isCanceled())
						return;
					out.write(buffer, 0, len);
					monitor.worked(len);
				}
			}

			if (moveInsteadOfCopy) {
				source.delete();
			}
		}
	}

	public static void copyStoreToFile(IFileStore source, File target, boolean recursive, 
			FileOverwritePolicy overwritePolicy, IProgressMonitor monitor) throws IOException, CoreException {
		target = new File(target, source.getName());
		copyStoreToFile(source, target, recursive, false, overwritePolicy, monitor);
	}

	public static void moveStoreToFile(IFileStore source, File target, boolean recursive, 
			FileOverwritePolicy overwritePolicy, IProgressMonitor monitor) throws IOException, CoreException {
		target = new File(target, source.getName());
		copyStoreToFile(source, target, recursive, true, overwritePolicy, monitor);
	}

	private static void copyStoreToFile(IFileStore source, File target, boolean recursive, boolean moveInsteadOfCopy,
			FileOverwritePolicy overwritePolicy, IProgressMonitor monitor) throws IOException, CoreException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		else
			monitor.subTask("Transferring files.");

		if (monitor.isCanceled())
			return;

		IFileInfo info = source.fetchInfo();
		if (info.isDirectory()) {
			if (!recursive)
				return;

			if (!target.exists() && !target.mkdirs()) {
				throw new IOException("Failed to create target directory: " + target.getAbsolutePath());
			}

			var stores = source.childStores(EFS.NONE, monitor);
			
			monitor.beginTask((moveInsteadOfCopy ? "Moving" : "Copying") + ": " + source.getName(),
					stores.length);

			for (var child : stores) {
				File childTarget = new File(target, child.getName());
				copyStoreToFile(child, childTarget, true, moveInsteadOfCopy, overwritePolicy,
						monitor);
				monitor.worked(1);
				
			}

			if (moveInsteadOfCopy) {
				source.delete(EFS.NONE, monitor);
			}

		} else {
			if (target.exists()) {
				var decision = overwritePolicy.queryOverwrite(target.getAbsolutePath(), moveInsteadOfCopy);
				if (decision == FileOverwritePolicy.Decision.CANCEL)
					throw new OperationCanceledException();
				if (decision == FileOverwritePolicy.Decision.SKIP)
					return;
			}

			File parent = target.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
			}
			
			monitor.beginTask((moveInsteadOfCopy ? "Moving" : "Copying") + ": " + source.getName(),
					(int)source.fetchInfo().getLength());

			try (InputStream in = new BufferedInputStream(source.openInputStream(EFS.NONE, monitor));
					OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
				byte[] buffer = new byte[8192];
				int len;
				while ((len = in.read(buffer)) != -1) {
					if (monitor.isCanceled())
						return;
					out.write(buffer, 0, len);
					monitor.worked(len);
				}
			}

			if (moveInsteadOfCopy) {
				source.delete(EFS.NONE, monitor);
			}
		}

		monitor.done();
	}


	public static void copyStoreToStore(IFileStore source, IFileStore target, 
			boolean recursive, FileOverwritePolicy overwritePolicy, IProgressMonitor monitor)
			throws IOException, CoreException {
		target = target.getChild(source.getName());
		copyStoreToStore(source, target, recursive, false, overwritePolicy, monitor);		
	}


	public static void moveStoreToStore(IFileStore source, IFileStore target, 
			boolean recursive, FileOverwritePolicy overwritePolicy, IProgressMonitor monitor)
			throws IOException, CoreException {
		target = target.getChild(source.getName());
		copyStoreToStore(source, target, recursive, true, overwritePolicy, monitor);
	}

	private static void copyStoreToStore(IFileStore source, IFileStore target, boolean recursive,
			boolean moveInsteadOfCopy, FileOverwritePolicy overwritePolicy, IProgressMonitor monitor)
			throws IOException, CoreException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		else
			monitor.subTask("Transferring files.");

		if (monitor.isCanceled())
			return;

		IFileInfo info = source.fetchInfo();
		if (info.isDirectory()) {
			if (!recursive)
				return;

			if(!target.fetchInfo().exists()) {
				target = target.mkdir(EFS.NONE, monitor);
			}

			var stores = source.childStores(EFS.NONE, monitor);
			
			monitor.beginTask((moveInsteadOfCopy ? "Moving" : "Copying") + ": " + source.getName(),
					stores.length);

			for (IFileStore child : stores) {
				IFileStore childTarget = target.getChild(child.getName());
				copyStoreToStore(child, childTarget, true, moveInsteadOfCopy, overwritePolicy,
						null);
				monitor.worked(1);
			}

			if (moveInsteadOfCopy) {
				source.delete(EFS.NONE, monitor);
			}

		} else {
			if (target.fetchInfo().exists()) {
				var decision = overwritePolicy.queryOverwrite(target.toURI().getPath(), moveInsteadOfCopy);
				if (decision == FileOverwritePolicy.Decision.CANCEL)
					throw new OperationCanceledException();
				if (decision == FileOverwritePolicy.Decision.SKIP)
					return;
			}
			
			monitor.beginTask((moveInsteadOfCopy ? "Moving" : "Copying") + ": " + source.getName(),
					(int)source.fetchInfo().getLength());

			try (InputStream in = new BufferedInputStream(source.openInputStream(EFS.NONE, monitor));
					OutputStream out = new BufferedOutputStream(target.openOutputStream(0, monitor))) {
				byte[] buffer = new byte[8192];
				int len;
				while ((len = in.read(buffer)) != -1) {
					if (monitor.isCanceled())
						return;
					out.write(buffer, 0, len);
					monitor.worked(len);
				}
			}

			if (moveInsteadOfCopy) {
				source.delete(EFS.NONE, monitor);
			}
		}

		monitor.done();
	}

}
