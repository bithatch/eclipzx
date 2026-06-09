package uk.co.bithatch.eclipzpp;

import java.nio.file.Paths;
import java.util.Map;

import uk.co.bithatch.eclipzpp.GenericPreprocessor.Format;



public class PPTest2 {

	public static void main(String[] args) throws Exception {
		var smap = new SourceMap();
		
		/* Build the preprocessor */
		var ppfsBldr = new FileSystemResourceResolver.Builder().
				addIncludePaths(Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.eclipzpp/samples/lib"));
		
		var pp = new GenericPreprocessor.Builder().
				withSourceMap(smap).
				withMode(Mode.COMPILER).
				withFormat(Format.Z88DK).
				withDefines(Map.of("SMALL_NUMBER", "1")).
				withResourceResolver(ppfsBldr.build()).build();
		
		System.out.print(pp.process(Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.eclipzpp/samples/main.asm")));
		
		pp.defines().forEach((k,v) -> {
			System.out.println("XX define " + k + "=" + v);
		});
		
	}
}
