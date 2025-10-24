package uk.co.bithatch.ayzxfx.ay;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface AFX  {

	public final static class Impl {
		private final static class NamedAFXImpl implements NamedAFX {
			
			private String name;
			private final AFX delegate;
			
			private NamedAFXImpl(AFX delegate) {
				this.delegate = delegate; 
			}

			@Override
			public void add(AFXFrame frame) {
				delegate.add(frame);
			}

			@Override
			public List<AFXFrame> frames() {
				return delegate.frames();
			}

			@Override
			public int write(ByteBuffer buf) {
				return delegate.write(buf);
			}

			@Override
			public void add(int index, AFXFrame frame) {
				delegate.add(index, frame);
			}

			@Override
			public boolean remove(AFXFrame frame) {
				return delegate.remove(frame);
			}

			@Override
			public AFXFrame remove(int index) {
				return delegate.remove(index);
			}

			@Override
			public AFXFrame set(int row, AFXFrame next) {
				return delegate.set(row, next);
			}

			@Override
			public AFXFrame get(int row) {
				return delegate.get(row);
			}

			@Override
			public int size() {
				return delegate.size();
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public String name(String name) {
				var was = this.name;
				this.name = name;
				return was;
			}
			
		}

		public final static class AFXImpl implements AFX {
			
			private final List<AFXFrame> frames = new ArrayList<>();

			AFXImpl() { }
			
			AFXImpl(ByteBuffer buf) throws IOException {
				var tone = 0;
				var noise = 0;
				var maybeEnd = false;
				
				if(!buf.hasRemaining()) {
					add(AFB.defaultFrame());
					return;
				}
				
				while(buf.hasRemaining()) {
					var flags = Byte.toUnsignedInt(buf.get());
					if(!maybeEnd && flags == 0xd0) {
						maybeEnd = true;
						continue;
					}
					else if(maybeEnd && flags == 0x20) {
						break;
					}
					else if(maybeEnd) {
						maybeEnd = false;
						flags = 0xd0;
					}
					var vol = flags & 0xF;
					var noTone = (flags & 0x10) != 0;
					var changeTone = (flags & 0x20 ) != 0;
					var changeNoise = (flags & 0x40) != 0;
					var noNoise = (flags & 0x80) != 0;
					if(changeTone) {
						tone = Short.toUnsignedInt(buf.getShort());
					}
					if(changeNoise) {
						noise = Byte.toUnsignedInt(buf.get());
					}
					
					var effect = new AFXFrame(!noTone, !noNoise, tone,  noise, vol);
//					System.out.println("Adding effect frame: " + effect);
					add(effect);
				}
				
			}

			@Override
			public int write(ByteBuffer buffer) {
				var w =  buffer.position();
				var changeTone = false;
				var changeNoise = false;
				var lastTone = 0;
				var lastNoise = 0;
				for(var frame : frames) {
					
					if(frame.period() !=  lastTone)
						changeTone = true;

					if(frame.noise() !=  lastNoise)
						changeNoise = true;
					
					buffer.put((byte)(
						( frame.volume() & 0xf ) |
						( frame.t() ? 0 : 0x10  ) |
						( changeTone ? 0x20 : 0 ) |
						( changeNoise ? 0x40 : 0 ) |
						( frame.n() ? 0 : 0x80  )
					));
					
					if(changeTone) {
						buffer.putShort((short)frame.period());
					}
					
					if(changeNoise) {
						buffer.put((byte)frame.noise());
					}
					
					changeTone = false;
					changeNoise = false;
					lastTone = frame.period();
					lastNoise = frame.noise();
				}
				buffer.putShort((short)(0xd020));
				return buffer.position() - w;
			}

			@Override
			public void add(AFXFrame frame) {
				frames.add(frame);
			}

			@Override
			public List<AFXFrame> frames() {
				return Collections.unmodifiableList(frames);
			}

			@Override
			public void add(int index, AFXFrame frame) {
				frames.add(index, frame);
			}

			@Override
			public boolean remove(AFXFrame frame) {
				return frames.remove(frame);
			}

			@Override
			public AFXFrame remove(int index) {
				return frames.remove(index);
			}

			@Override
			public AFXFrame set(int row, AFXFrame next) {
				return frames.set(row, next);
			}

			@Override
			public AFXFrame get(int row) {
				return frames.get(row);
			}

			@Override
			public int size() {
				return frames.size();
			}
		}
	}

	public static AFX create() {
		var afxImpl = new Impl.AFXImpl();
		afxImpl.add(AFB.defaultFrame());
		return afxImpl;
	}
	
	

	public static AFX load(ReadableByteChannel in) {
		try {
			var buf = ByteBuffer.allocate(65536);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			in.read(buf);
			buf.flip();
			var afx = new Impl.AFXImpl(buf);
			return afx;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	void add(AFXFrame frame);
	
	List<AFXFrame> frames();

	int write(ByteBuffer buf);

	default void save(SeekableByteChannel wtr) throws IOException {
		var buf = ByteBuffer.allocate(65536);
		write(buf);
		buf.flip();
		wtr.write(buf);
	}

	void add(int index, AFXFrame frame);

	boolean remove(AFXFrame frame);

	AFXFrame remove(int index);

	AFXFrame set(int row, AFXFrame next);

	AFXFrame get(int row);
	
	int size();

	default NamedAFX named() {
		if(this instanceof NamedAFX nafx) {
			return nafx;
		}
		else {
			return new Impl.NamedAFXImpl(this);  
		}
	}

	public static NamedAFX named(String string) {
		return new Impl.NamedAFXImpl(new Impl.AFXImpl());
	}
}
