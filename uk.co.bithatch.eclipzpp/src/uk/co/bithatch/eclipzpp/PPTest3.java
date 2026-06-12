package uk.co.bithatch.eclipzpp;

import java.nio.file.Paths;

import uk.co.bithatch.eclipzpp.GenericPreprocessor.Format;



public class PPTest3 {

	public static void main(String[] args) throws Exception {
		var smap = new SourceMap();
		
		/* Build the preprocessor */
		var ppfsBldr = new FileSystemResourceResolver.Builder().
				addIncludePaths(Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.eclipzpp/samples/lib"));
		
		var pp = new GenericPreprocessor.Builder().
				withSourceMap(smap).
				withMode(Mode.EDITOR).
				withFormat(Format.BORIEL).
				withResourceResolver(ppfsBldr.build()).build();
		
		System.out.print(pp.process(Paths.get("/home/SOUTHPARK/tanktarta/Documents/Git/eclipzx/uk.co.bithatch.eclipzpp/samples/bas.bas")));
	}
}
