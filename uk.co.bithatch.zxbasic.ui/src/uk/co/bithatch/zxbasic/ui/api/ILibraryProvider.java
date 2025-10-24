package uk.co.bithatch.zxbasic.ui.api;

import java.util.List;

import org.eclipse.core.resources.IProject;

import uk.co.bithatch.zxbasic.ui.library.ZXLibrary;

public interface ILibraryProvider {
    List<ZXLibrary> getLibraries(IProject project);
}
