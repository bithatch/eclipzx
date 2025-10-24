package uk.co.bithatch.zxbasic.ui.library;

import java.io.File;
import java.util.List;

import uk.co.bithatch.bitzx.IArchitecture;

public record ZXSDK(String name, File location, List<ZXLibrary> libraries) {

	public File runtime(IArchitecture architecture) {
		return new File(new File(new File(new File(new File(location, "src"), "lib"), "arch"),
				architecture.name().toLowerCase()), "runtime");
	}
}
