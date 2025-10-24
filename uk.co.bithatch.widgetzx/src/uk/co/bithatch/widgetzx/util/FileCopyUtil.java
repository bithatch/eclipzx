package uk.co.bithatch.widgetzx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class FileCopyUtil {

    public static void copyToProject(File sourceFile, IContainer targetContainer, IProgressMonitor monitor)
            throws CoreException, IOException {
        if (!sourceFile.exists()) {
            throw new IOException("Source file does not exist: " + sourceFile.getAbsolutePath());
        }

        if (monitor != null) {
            monitor.subTask("Copying: " + sourceFile.getName());
        }

        if (sourceFile.isDirectory()) {
            IFolder folder = targetContainer.getFolder(new Path(sourceFile.getName()));
            if (!folder.exists()) {
                folder.create(true, true, monitor);
            }

            File[] children = sourceFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyToProject(child, folder, monitor);
                }
            }
        } else {
            IFile targetFile = targetContainer.getFile(new Path(sourceFile.getName()));
            try (InputStream inputStream = new FileInputStream(sourceFile)) {
                if (targetFile.exists()) {
                    targetFile.setContents(inputStream, IResource.FORCE, monitor);
                } else {
                    targetFile.create(inputStream, true, monitor);
                }
            }
        }

        if (monitor != null) {
            monitor.worked(1);
        }
    }

    public static void copyDirectoryToProject(File sourceDir, IProject project, IProgressMonitor monitor)
            throws CoreException, IOException {
        if (!sourceDir.isDirectory()) {
            throw new IllegalArgumentException("sourceDir must be a directory");
        }

        File[] children = sourceDir.listFiles();
        if (children != null && monitor != null) {
            monitor.beginTask("Copying project content...", children.length);
        }

        for (File child : children) {
            copyToProject(child, project, monitor);
        }

        if (monitor != null) {
            monitor.done();
        }
    }
}
