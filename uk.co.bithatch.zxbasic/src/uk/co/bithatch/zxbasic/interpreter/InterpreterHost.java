package uk.co.bithatch.zxbasic.interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import uk.co.bithatch.zxbasic.basic.AttributeModifier;

public interface InterpreterHost {
	
	public interface PrintInstruction {
	}
	
	public record PrintAt(int x, int y) implements PrintInstruction {}
	public record PrintText(String text) implements PrintInstruction {}
	public record PrintAttribute(AttributeModifier mod) implements PrintInstruction {}
	
	public interface InterpreterResource {
		Optional<Integer> size();
		
		ReadableByteChannel channel() throws IOException;
	}

	String inkey();
	
	void ink(int ink);
	
	void paper(int ink);
	
	void border(int ink);
	
	void print(PrintInstruction... parts);
	
	void plot(int x, int y, AttributeModifier... attrs);
	
	void draw(int x, int y, Optional<Float> arg, AttributeModifier... attrs);
	
	void circle(int x, int y, int radius, AttributeModifier... attrs);
	
	InputStream input();
	
	default InterpreterResource load(String filename) {
		var path = Paths.get(filename);
		return new InterpreterResource() {
			
			@Override
			public Optional<Integer> size() {
				try {
					return Optional.of((int)Files.size(path));
				} catch (IOException e) {
					return Optional.empty();
				}
			}
			
			@Override
			public ReadableByteChannel channel() throws IOException {
				return Files.newByteChannel(path, StandardOpenOption.READ);
			}
		};
	}

	default WritableByteChannel save(String filename) throws IOException {
		return Files.newByteChannel(Paths.get(filename), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}

	void beep(float duration, int freq);

	void attr(AttributeModifier attributes);

	void at(int line, int column);
	
}