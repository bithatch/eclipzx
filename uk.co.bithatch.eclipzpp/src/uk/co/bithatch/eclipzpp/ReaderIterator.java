package uk.co.bithatch.eclipzpp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class ReaderIterator implements Iterator<String> {
	private final PushbackReader br;
	String line;
	private boolean eof;
	private String lineSeparator = System.lineSeparator();
	private boolean lineSeparatorDetected;

	public ReaderIterator(Reader br) {
		var rdr = br instanceof BufferedReader brr ? brr : new BufferedReader(br);
		this.br = new PushbackReader(rdr, 1);
	}

	public String lineSeparator() {
		return lineSeparator;
	}

	@Override
	public boolean hasNext() {
		checkNext();
		return line != null;
	}

	@Override
	public String next() {
		checkNext();
		try {
			if(line == null)
				throw new NoSuchElementException();
			
			return line;
		}
		finally {
			line = null;
		}
	}

	private void checkNext() {
		if(line == null && !eof) {
			try {
				line = readLine();
				if(line == null) {
					eof = true;
					br.close();
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private String readLine() throws IOException {
		var b = new StringBuilder();
		while(true) {
			var c = br.read();
			if(c == -1) {
				return b.length() == 0 ? null : b.toString();
			}
			if(c == '\n') {
				detectLineSeparator("\n");
				return b.toString();
			}
			if(c == '\r') {
				var nc = br.read();
				if(nc == '\n') {
					detectLineSeparator("\r\n");
				}
				else {
					detectLineSeparator("\r");
					if(nc != -1) {
						br.unread(nc);
					}
				}
				return b.toString();
			}
			b.append((char)c);
		}
	}

	private void detectLineSeparator(String sep) {
		if(!lineSeparatorDetected) {
			lineSeparator = sep;
			lineSeparatorDetected = true;
		}
	}
}
