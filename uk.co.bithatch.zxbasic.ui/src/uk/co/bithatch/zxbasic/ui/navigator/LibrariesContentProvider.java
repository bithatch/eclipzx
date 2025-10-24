package uk.co.bithatch.zxbasic.ui.navigator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;

import uk.co.bithatch.zxbasic.ui.library.ContributedLibraryRegistry;

public class LibrariesContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IProject project && project.isOpen()) {
            return new Object[] { new LibrariesNode(project) };
        }
        else if (parentElement instanceof LibrariesNode node) {
            return resolveLibraryFolders(node);
        }
        else if (parentElement instanceof ILibraryContentsNode node) {
            return resolveLibraryContents(node);
        }
        return new Object[0];
    }

	@Override
    public boolean hasChildren(Object element) {
        return ( element instanceof IProject prj && prj.isOpen()) || 
        	   ( element instanceof LibrariesNode ) || 
        	   ( element instanceof LibraryNode ) || 
        	   ( element instanceof LibraryFileNode lfn && lfn.getFile().isDirectory() );
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    private Object[] resolveLibraryContents(ILibraryContentsNode node) {
    	var al = new ArrayList<ILibraryContentsNode>();
    	try(var str = Files.list(node.getFile().toPath())) {
        	al.addAll(str.
        			filter(p -> Files.isDirectory(p)).
        			map(p -> new LibraryFileNode(node, p.toFile())).toList());
    	} 
    	catch(IOException ioe) {
    		throw new UncheckedIOException(ioe);
    	}
    	
    	try(var str = Files.list(node.getFile().toPath())) {
        	al.addAll(str.
        			filter(p -> p.getFileName().toString().endsWith(".bas") || p.getFileName().toString().endsWith(".asm")).
        			map(p -> new LibraryFileNode(node, p.toFile())).toList());
    	} 
    	catch(IOException ioe) {
    		throw new UncheckedIOException(ioe);
    	}
    	
    	return al.toArray();
	}

    private Object[] resolveLibraryFolders(LibrariesNode libraries) {
        return ContributedLibraryRegistry.getActiveLibraries(libraries.getProject()).stream().map(lib -> new LibraryNode(libraries, lib)).toArray();
    }

	@Override
	public Object getParent(Object element) {
		if (element instanceof LibrariesNode node) {
            return node.getProject();
        }
        else if (element instanceof LibraryNode node) {
            return node.getLibraries();
        }
        else if (element instanceof LibraryFileNode node) {
            return node.getParent();
        }
        else {
        	return null;
        }
	}
}
