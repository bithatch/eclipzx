package uk.co.bithatch.eclipzpp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class PPAssertDeflTest {

	private record ErrorEvent(Error error, String message) {
	}

	public static void main(String[] args) throws Exception {
		testDeflForms();
		testAssertSuccess();
		testAssertFailureWithMessage();
		testMacroLocalDirective();
		testExitmDirective();
		testReptDirective();
		testReptcDirective();
		testReptiDirective();
		testBinaryDirective();
		testEditorLineSyncWithContinuations();
		System.out.println("PPAssertDeflTest: OK");
	}

	private static void testDeflForms() throws Exception {
		var errors = new ArrayList<ErrorEvent>();
		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.COMPILER)
				.onError((e, ln, m) -> errors.add(new ErrorEvent(e, m)))
				.build();

		var input = String.join("\n",
				"DEFL var = var+1",
				"DEFL var = var+1",
				"next DEFL next+2",
				"db var",
				"db next",
				"");

		var out = pp.process(input);
		expect(errors.isEmpty(), "DEFL should not produce errors.");
		expect(out.contains("db +1+1"), "DEFL redefinition should expand to '+1+1'. Output was: " + out);
		expect(out.contains("db +2"), "Alternative DEFL syntax should expand to '+2'. Output was: " + out);
	}

	private static void testAssertSuccess() throws Exception {
		var errors = new ArrayList<ErrorEvent>();
		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.COMPILER)
				.onError((e, ln, m) -> errors.add(new ErrorEvent(e, m)))
				.build();

		var out = pp.process("ASSERT 1\ndb 9\n");
		expect(errors.isEmpty(), "ASSERT 1 should not produce errors.");
		expect(out.contains("db 9"), "ASSERT 1 should allow following lines to pass through.");
	}

	private static void testAssertFailureWithMessage() throws Exception {
		var errors = new ArrayList<ErrorEvent>();
		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.COMPILER)
				.onError((e, ln, m) -> errors.add(new ErrorEvent(e, m)))
				.build();

		pp.process("ASSERT 0, \"boom\"\ndb 1\n");
		expect(errors.size() == 1, "ASSERT 0 should produce exactly one error event.");
		expect(errors.get(0).error == Error.ASSERT_FAILED, "ASSERT 0 should emit ASSERT_FAILED.");
		expect("boom".equals(errors.get(0).message), "ASSERT message should be propagated.");
	}

	private static void testMacroLocalDirective() throws Exception {
		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.COMPILER)
				.build();

		var out = pp.process("MACRO mk\nLOCAL lbl\nlbl: db 1\nENDM\nmk\n");
		expect(out.contains("lbl__1: db 1"), "LOCAL should remap symbol to unique form. Output was: " + out);
	}

	private static void testExitmDirective() throws Exception {
		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.COMPILER)
				.build();

		var out = pp.process("MACRO once a\ndb a\nEXITM\ndb 9\nENDM\nonce 3\n");
		expect(out.contains("db 3"), "EXITM macro should output lines before EXITM.");
		expect(!out.contains("db 9"), "EXITM should stop remaining macro body lines.");
	}

	private static void testReptDirective() throws Exception {
		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.COMPILER)
				.build();

		var out = pp.process("REPT 3\ndb 7\nENDR\n");
		expect(count(out, "db 7") == 3, "REPT 3 should emit block 3 times. Output was: " + out);
	}

	private static void testReptcDirective() throws Exception {
		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.COMPILER)
				.build();

		var out = pp.process("REPTC ch, \"ab\"\ndb ch\nENDR\n");
		expect(out.contains("db a"), "REPTC should substitute first character.");
		expect(out.contains("db b"), "REPTC should substitute second character.");
	}

	private static void testReptiDirective() throws Exception {
		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.COMPILER)
				.build();

		var out = pp.process("REPTI reg, bc, de\npush reg\nENDR\n");
		expect(out.contains("push bc"), "REPTI should substitute first argument.");
		expect(out.contains("push de"), "REPTI should substitute second argument.");
	}

	private static void testBinaryDirective() throws Exception {
		Path dir = Files.createTempDirectory("pp-binary-");
		Path srcDir = dir.resolve("src");
		Path incDir = dir.resolve("inc");
		try {
			Files.createDirectories(srcDir);
			Files.createDirectories(incDir);
			Files.write(incDir.resolve("blob.bin"), new byte[] { 0, 10, 13, (byte)255 });
			var resolver = new FileSystemResourceResolver.Builder()
					.withWorkingDir(srcDir)
					.addIncludePaths(incDir)
					.build();
			var pp = new GenericPreprocessor.Builder()
					.withMode(Mode.COMPILER)
					.withResourceResolver(resolver)
					.build();

			var out = pp.process("BINARY \"blob.bin\"\nINCBIN \"blob.bin\"\n", srcDir.resolve("test.asm"));
			expect(count(out, "defb 0,10,13,255") == 2, "BINARY/INCBIN should emit defb rows for file bytes. Output was: " + out);
		}
		finally {
			Files.deleteIfExists(incDir.resolve("blob.bin"));
			Files.deleteIfExists(srcDir);
			Files.deleteIfExists(incDir);
			Files.deleteIfExists(dir);
		}
	}

	private static void testEditorLineSyncWithContinuations() throws Exception {
		var source = Files.readString(Path.of("samples", "bas.bas"));
		var expected = Files.readString(Path.of("samples", "bas.expected"));

		var pp = new GenericPreprocessor.Builder()
				.withMode(Mode.EDITOR)
				.withFormat(GenericPreprocessor.Format.BORIEL)
				.build();

		var actual = pp.process(source);
		var srcLines = splitLines(source);
		var actualLines = splitLines(actual);
		var expectedLines = splitLines(expected);

		expect(actualLines.length == srcLines.length,
				"EDITOR output line count should match source line count.");
		expect(actualLines.length == expectedLines.length,
				"EDITOR output line count should match expected sample line count.");

		for(int i = 0; i < actualLines.length; i++) {
			expect(actualLines[i].length() == srcLines[i].length(),
					"EDITOR output width mismatch at line " + (i + 1) + ".");
		}

		expect(actual.equals(expected), "EDITOR output must match bas.expected exactly.");
	}

	private static String[] splitLines(String text) {
		return text.replace("\r\n", "\n").split("\n", -1);
	}

	private static int count(String text, String token) {
		int c = 0;
		int idx = 0;
		while((idx = text.indexOf(token, idx)) != -1) {
			c++;
			idx += token.length();
		}
		return c;
	}

	private static void expect(boolean condition, String message) {
		if(!condition) {
			throw new IllegalStateException(message);
		}
	}
}
