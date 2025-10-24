package uk.co.bithatch.zxbasic.preprocessor;

import java.nio.file.Paths;
import java.util.Map;

import uk.co.bithatch.zxbasic.preprocessor.ZXPreprocessor.Mode;



public class PPTest {

	public static void main(String[] args) throws Exception {
		var smap = new SourceMap();

		/* Build the preprocessor */
		var ppfsBldr = new ZXPreprocessor.FileSystemResourceResolver.Builder().
				withRuntimeDir(Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.zxbasic.borielsdk/META-INF/zxbasic-1.18.1/src/lib/arch/zx48k/runtime")).
				addIncludes("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.zxbasic.borielsdk/META-INF/zxbasic-1.18.1/src/lib/arch/zx48k/stdlib").
				addIncludePaths(Paths.get("tests/lib"));
		
		var pp = new ZXPreprocessor.Builder().
				withSourceMap(smap).
				withMode(Mode.EDITOR).
				withDefines(Map.of("SMALL_NUMBER", "1")).
				withResourceResolver(ppfsBldr.build()).build();
		
//		pp.process(Paths.get("tests/inctest.bas"));
//		System.out.print(pp.process(Paths.get("tests/definetest.bas")));
//		System.out.print(pp.process(Paths.get("tests/macros.bas")));
//		System.out.print(pp.process(Paths.get("/home/SOUTHPARK/tanktarta/Workspaces/ZXTNFS/ttlib/Theme.bas")));
		System.out.print(pp.process(Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.eclipzx/test-workspaces/maindev/next-build-examples/nextlib.bas")));
//		System.out.print(pp.process(Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.zxbasic/tests/asmtest.bas")));
		
		pp.defines().forEach((k,v) -> {
			System.out.println("define " + k + "=" + v);
		});
		
//		System.out.print(pp.process(Paths.get("tests/inclinetest.bas")));
//		System.out.print(pp.process(Paths.get("tests/inctest.bas")));
//		System.out.print(pp.process(Paths.get("tests/goto.bas")));
//		System.out.print(pp.process(Paths.get("tests/macros.bas")));
//		System.out.print(pp.process(Paths.get("/home/SOUTHPARK/tanktarta/Workspaces/runtime-eclipzx.product/zxbasic-examples/spfill.bas")));
		
//		pp.process(Paths.get("tests/goto.bas")).forEach(l -> System.out.println(l));
//		pp.process(Paths.get("/home/SOUTHPARK/tanktarta/Workspaces/runtime-eclipzx.product/zxbasic-examples/spfill.bas")).forEach(l -> {
//		 System.out.println(l); });
		

//		pp.process(Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.zxbasic/tests/inclinetest.bas")).forEach(l -> {
//		 System.out.println(l); });
		
	}
}
