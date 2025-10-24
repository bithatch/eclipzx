package uk.co.bithatch.zxbasic.ui.launch;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;

import uk.co.bithatch.emuzx.ui.ConsoleUtil;
import uk.co.bithatch.zxbasic.basic.AttributeModifier;
import uk.co.bithatch.zxbasic.interpreter.InterpreterHost;

public class InterpreterConsoleHost implements InterpreterHost, Closeable {
	
	private IOConsoleOutputStream out;
	private IOConsoleInputStream in;
	private Synthesizer synth;
	private boolean noMidi;
	private MidiChannel[] channels;
	private int instrument = 0; // 0 is a piano, 9 is percussion, other channels are for other instruments
	private int volume = 127; // between 0 et 127
	
	public InterpreterConsoleHost(IOConsole console) {
		out = console.newOutputStream();
		in = console.getInputStream();
	}

	@Override
	public String inkey() {
		try {
			if(in.available() > 0) {
				var c = in.read();
				if(c != -1) {
					return String.valueOf((char)c);
				}
			}
		}
		catch(IOException ioe) {
		}
		return "";
	}

	public int getInstrument() {
		return instrument;
	}

	public void setInstrument(int instrument) {
		this.instrument = instrument;
	}

	public int getVolume() {
		return volume;
	}

	public void setVolume(int volume) {
		this.volume = volume;
	}

	@Override
	public void border(int ink) {
	}

	@Override
	public void ink(int ink) {
		Display.getDefault().syncExec(() -> {
		    out.setColor(ConsoleUtil.getColor(ink));
		});
	}

	@Override
	public InputStream input() {
		return new FilterInputStream(in) {
			
			private boolean closed;

			@Override
			public int available() throws IOException {
				checkClosed();
				return super.available();
			}

			private void checkClosed() throws IOException {
				if(closed)
					throw new IOException("Already closed.");
			}

			@Override
			public void close() throws IOException {
				if(!closed) {
					closed = true;
				}
			}

			@Override
			public int read() throws IOException {
				checkClosed();
				return super.read();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				checkClosed();
				return super.read(b, off, len);
			}

			@Override
			public long skip(long n) throws IOException {
				checkClosed();
				return super.skip(n);
			}
		};
	}

	@Override
	public void paper(int ink) {
	}

	@Override
	public void print(PrintInstruction... parts) {
		try {
			for(var p : parts) {
				if(p instanceof PrintText text) {
					out.write(text.text());
				}
			}
			out.write(System.lineSeparator());
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	@Override
	public void plot(int x, int y, AttributeModifier... attrs) {
	}

	@Override
	public void draw(int x, int y, Optional<Float> arg, AttributeModifier... attrs) {
	}

	@Override
	public void circle(int x, int y, int radius, AttributeModifier... attrs) {
	}

	@Override
	public void beep(float duration, int note) {
		if(synth == null) {
			try {
				synth = MidiSystem.getSynthesizer();
				channels = synth.getChannels();
			} catch (Exception e) {
				e.printStackTrace();
				noMidi = true;
			}
		}
		if(!noMidi) {
			channels[0].noteOn(note, volume );
			try {
				Thread.sleep((long)( duration * 1000f));
			} catch (InterruptedException e) {
			}
			channels[0].noteOff(note);
		}
		
	}

	@Override
	public void close() {
		if(synth != null) {
			synth.close();
		}
		
	}

	@Override
	public void attr(AttributeModifier attributes) {
	}

	@Override
	public void at(int line, int column) {
	}

}