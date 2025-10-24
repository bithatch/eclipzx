package uk.co.bithatch.zxbasic.ui.library;

import java.io.File;

import uk.co.bithatch.bitzx.IArchitecture;

public record ZXLibrary(String name, String plugin, String icon, File location, IArchitecture arch, boolean isReadOnly, boolean builtIn) {

}
