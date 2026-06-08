package uk.co.bithatch.bitzx.pp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import uk.co.bithatch.bitzx.pp.GenericPreprocessor.PreProcessorConfiguration;

public final class ReaderIterator implements Iterator<String> {
	private final BufferedReader br;
	private	String line;
	private PreProcessorConfiguration config;

	public ReaderIterator(Reader br, PreProcessorConfiguration config) {
		this.config = config;
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
			String previous = null;
			while(true) {
				try {
					line = br.readLine();
					if(line == null) {
						br.close();
						break;
					}
					else {
						if(previous != null) {
							line = previous + line;
						}
						previous = null;
					}
					
					if(config.lineContinuations().isPresent() && line.endsWith(config.lineContinuations().get().toString())) {
						previous = line.substring(0, line.length() - 1) + System.lineSeparator();
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
	}
}