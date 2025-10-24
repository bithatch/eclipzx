package uk.co.bithatch.eclipz88dk.lang;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** GCC C variant that tolerates Z88DK #asm ... #endasm in source. */
public class Z88DKCLanguage extends GCCLanguage implements ILanguage {
	
	private final static char[] LINESEPS = System.lineSeparator().toCharArray(); 

	@Override
	public String getId() {
		return "uk.co.bithatch.eclipz88dk.language.c";
	}

	@Override
	public String getName() {
		return "Z88DK C";
	}

	@Override
	public IASTTranslationUnit getASTTranslationUnit(FileContent fc, IScannerInfo scanInfo,
			IncludeFileContentProvider fileCreator, IIndex index, int options, IParserLogService log)
			throws CoreException {
		FileContent filtered = fc;
		try {
			filtered = tryFilterAsmBlocks(fc, StandardCharsets.UTF_8);
		} catch (Exception ignore) {
			// Fallback to original content on any issue
		}
		return super.getASTTranslationUnit(filtered, scanInfo, fileCreator, index, options, log);
	}

	/**
	 * Rebuilds a FileContent with #asm blocks stripped (line numbers preserved).
	 */
	private static FileContent tryFilterAsmBlocks(FileContent fc, Charset cs) throws IOException {
		if (fc == null)
			return null;
		final String path = fc.getFileLocation();
		if (path == null)
			return fc;

		// Read the actual file bytes; public FileContent doesn’t expose contents in CDT
		// 12.1
		final char[] in = new String(Files.readAllBytes(Paths.get(path)), cs).toCharArray();
		if (in.length == 0)
			return fc;

		final String out = rewriteAsmAsInactiveBlocks(in);
//		final String out = stripAsmBlocks(in);
		System.out.println("----->");
		System.out.print(out);
		System.out.println("<-----");
		// Build a brand-new top-level FileContent with the filtered text
		return FileContent.create(path, out.toCharArray());
	}
	
	/**
	 * Remove lines from #asm (at line-start, ignoring spaces) up to and including
	 * #endasm.
	 */
	private static String stripAsmBlocks(char[] c) {
		StringBuilder sb = new StringBuilder(c.length);
		int i = 0, n = c.length;
		while (i < n) {
			int lineStart = findLineStart(c, i);
			int p = skipSpaces(c, lineStart);
			if (p < n && c[p] == '#' && matchWord(c, p + 1, "asm")) {
				// consume '#asm' line (write only its EOL), then skip until '#endasm'
				// (preserving EOLs)
				i = consumeLineEOL(c, p); // writes EOL(s) for the #asm line
				while (i < n) {
					int ls = findLineStart(c, i);
					int q = skipSpaces(c, ls);
					if (q < n && c[q] == '#' && matchWord(c, q + 1, "endasm")) {
						i = consumeLineEOL(c, q); // also preserve the endasm line EOL(s)
						break;
					}
					i = consumeLineEOL(c, i); // skip inner line (write only EOL)
				}
				continue;
			}
			var ch = c[i++];
			if(ch == LINESEPS[0])
				sb.append(LINESEPS);
			else
				sb.append(ch);
		}
		return sb.toString();
	}

	private static int findLineStart(char[] c, int i) {
		int j = i;
		while (j > 0 && c[j - 1] != '\n' && c[j - 1] != '\r')
			j--;
		return j;
	}

	private static int skipSpaces(char[] c, int i) {
		while (i < c.length && (c[i] == ' ' || c[i] == '\t' || c[i] == '\f'))
			i++;
		return i;
	}

	private static boolean matchWord(char[] c, int i, String w) {
		for (int k = 0; k < w.length(); k++, i++) {
			if (i >= c.length || c[i] != w.charAt(k))
				return false;
		}
		// allow EOL or whitespace after the word
		return i >= c.length || c[i] == '\r' || c[i] == '\n' || c[i] == ' ' || c[i] == '\t' || c[i] == '\f';
	}

	/**
	 * Append only the line ending(s) for the line starting at index s; return index
	 * after EOL(s).
	 */
	private static int consumeLineEOL(char[] c, int s) {
		int i = s;
		while (i < c.length && c[i] != '\r' && c[i] != '\n')
			i++;
		while (i < c.length && (c[i] == '\r' || c[i] == '\n'))
			i++;
		return i;
	}

	/**
	 * Rewrite '#asm'...'#endasm' into an inactive PP region: '#asm' -> '#ifdef
	 * ___ECLIPZX_ASM_BLOCK' '#endasm' -> '#endif' Preserves all original line
	 * endings and indentation.
	 */
	private static String rewriteAsmAsInactiveBlocks(char[] c) {
		StringBuilder out = new StringBuilder(c.length);
		int n = c.length, i = 0;
		int depth = 0; // supports (rare) nesting

		while (i < n) {
			int lineStart = i;
			// scan to end-of-line, capturing EOL(s)
			int lineEnd = i;
			while (lineEnd < n && c[lineEnd] != '\r' && c[lineEnd] != '\n')
				lineEnd++;
			int eolStart = lineEnd;
			// capture one or two EOL chars (\r?\n)
			if (lineEnd < n && (c[lineEnd] == '\r' || c[lineEnd] == '\n')) {
				lineEnd++;
				if (lineEnd < n && c[lineEnd - 1] == '\r' && c[lineEnd] == '\n')
					lineEnd++;
			}
			String eol = new String(c, eolStart, lineEnd - eolStart);

			// leading whitespace (indent kept)
			int p = lineStart;
			while (p < eolStart && (c[p] == ' ' || c[p] == '\t' || c[p] == '\f'))
				p++;
			// directive start?
			boolean isHash = (p < eolStart && c[p] == '#');

			if (isHash && wordEq(c, p + 1, eolStart, "asm")) {
				// indent + '#ifdef ___ECLIPZX_ASM_BLOCK___'
				out.append(c, lineStart, p - lineStart).append("#ifdef ___ECLIPZX_ASM_BLOCK___").append(eol);
				depth++;
			} else if (isHash && wordEq(c, p + 1, eolStart, "endasm")) {
				// indent + '#endif'
				out.append(c, lineStart, p - lineStart).append("#endif").append(eol);
				if (depth > 0)
					depth--;
			} else {
				if(depth > 0) {
					int len = lineEnd - lineStart;
					for(int ii = 0 ; ii < len ; ii++) {
						if(c[ii + lineStart] == '\r' || c[ii + lineStart] == '\n')
							out.append(c[ii + lineStart]);
						else
							out.append(' ');
					}
				}
				else
				// passthrough unchanged
					out.append(c, lineStart, lineEnd - lineStart);
			}

			i = lineEnd;
		}
		return out.toString();
	}

	/**
	 * Match a word immediately after '#', allowing only whitespace/EOL afterwards.
	 */
	private static boolean wordEq(char[] c, int i, int eol, String w) {
		int j = i;
		for (int k = 0; k < w.length(); k++, j++) {
			if (j >= eol || c[j] != w.charAt(k))
				return false;
		}
		// allow trailing whitespace only
		while (j < eol) {
			char ch = c[j++];
			if (!(ch == ' ' || ch == '\t' || ch == '\f'))
				return false;
		}
		return true;
	}
}
