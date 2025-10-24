package uk.co.bithatch.zxbasic.preprocessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class ReaderIterator implements Iterator<String> {
	private final BufferedReader br;
	String line;

	public ReaderIterator(Reader br) {
		this.br = br instanceof BufferedReader brr ? brr : new BufferedReader(br);
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
		if(line == null) {
			try {
				line = br.readLine();
				if(line == null) {
					br.close();
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}