package uk.co.bithatch.zxbasic.ui.library;

import java.io.File;
import java.util.List;

import uk.co.bithatch.bitzx.ISDK;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicArchitecture;

public record ZXSDK(String name, File location, List<ZXLibrary> libraries) implements ISDK {

	public File runtime(BorielZXBasicArchitecture architecture) {
		return new File(new File(new File(new File(new File(location, "src"), "lib"), "arch"),
				architecture.runtimeDir()), "runtime");
	}
}
