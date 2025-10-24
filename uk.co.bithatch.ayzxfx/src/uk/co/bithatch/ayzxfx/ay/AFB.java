package uk.co.bithatch.ayzxfx.ay;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.co.bithatch.ayzxfx.AYFXUtil;

public interface AFB {

	public static AFB create() {
		return create(10, 10);
	}

	public static AFB create(int effects, int emptyFrames) {
		var afb = new Impl.AFBImpl();
		for (int i = 0; i < effects; i++) {
			var afx = AFX.named("Effect " + i);
			for (int j = 0; j < emptyFrames; j++) {
				afx.add(defaultFrame());
			}
			afb.add(afx);
		}
		return afb;
	}

	public static AFXFrame defaultFrame() {
		return new AFXFrame(true, false, 0x200, 0, 0xf);
	}

	public final static class Impl {
		public final static class AFBImpl implements AFB {

			private List<AFX> effects = new ArrayList<>();

			private AFBImpl() {

			}

			private AFBImpl(ReadableByteChannel in) throws IOException {
				var buf = ByteBuffer.allocate(65536);
				buf.order(ByteOrder.LITTLE_ENDIAN);

				while (in.read(buf) != -1)
					;

				buf.flip();

				if (!buf.hasRemaining()) {
					/* Empty file, add a single effect with a single frame */
					var effect = AFX.named("Effect 1");
					effect.add(defaultFrame());
					add(effect);
					return;
				}

				var len = buf.limit();

				var offsetCount = buf.get();
				var offsets = new int[offsetCount == 0 ? 256 : offsetCount];

				for (var i = 0; i < offsets.length; i++) {
					offsets[i] = Short.toUnsignedInt(buf.getShort());
					System.out.format("Effect: %d   Offset : %04x%n", i, offsets[i]);
				}

				var dataStartCheck = buf.position();
				System.out.format("Start of data %04x%n", dataStartCheck);
				if (dataStartCheck != offsets[0] + 2)
					throw new IOException("Doesn't appear to be an AFB file.");

				for (var i = 0; i < offsets.length; i++) {
					var limit = i == offsets.length - 1 ? len : 2 + offsets[i + 1];
					System.out.format("Reading Effect: %d at  Offset : %04x (%04x) to %04x (%04x)%n", i, offsets[i],
							buf.position(), limit, limit - 2);
					var effect = new AFX.Impl.AFXImpl(buf);
					if (buf.position() < limit) {
						var name = AYFXUtil.cString(buf);
						System.out.println("named " + name);
						var namedEffect = effect.named();
						namedEffect.name(name);
						add(namedEffect);
					} else {
						System.out.println("unnamed");
						add(effect);
					}
					if (i < offsets.length - 1) {
						buf.position(offsets[i + 1] + 2);
					}
				}

				System.out.println("Final order");
				effects.forEach(fix -> {
					System.out.println(fix);
				});

			}

			@Override
			public void save(WritableByteChannel wtr) throws IOException {
				if (effects.isEmpty())
					throw new IOException("Cannot write an empty AFB file.");

				var hdrbuf = ByteBuffer.allocate((size() * 2) + 1);
				hdrbuf.order(ByteOrder.LITTLE_ENDIAN);
				hdrbuf.put((byte) (size() == 256 ? 0 : size()));

				var buf = ByteBuffer.allocate(65536);
				buf.order(ByteOrder.LITTLE_ENDIAN);

				var dataStartOffset = (size() * 2) + 1;
				for (var effect : effects) {
					hdrbuf.putShort((short) dataStartOffset);
					dataStartOffset += effect.write(buf);
					if (effect instanceof NamedAFX nafx) {
						dataStartOffset += AYFXUtil.cString(nafx.name(), buf);
					}
				}

				hdrbuf.flip();
				wtr.write(hdrbuf);

				buf.flip();
				wtr.write(buf);
			}

			private void checkSize(int size) {
				if (size() + size > 256) {
					throw new IllegalStateException("Maximum number of effects in bank reached.");
				}
			}

			@Override
			public void add(AFX element) {
				checkSize(1);
				effects.add(element);
			}

			@Override
			public AFX get(int i) {
				return effects.get(i);
			}

			@Override
			public int indexOf(AFX effect) {
				return effects.indexOf(effect);
			}

			@Override
			public boolean remove(AFX effect) {
				return effects.remove(effect);
			}

			@Override
			public int size() {
				return effects.size();
			}

			@Override
			public void add(int index, AFX effect) {
				checkSize(1);
				effects.add(index, effect);
			}

			@Override
			public boolean contains(AFX afx) {
				return effects.contains(afx);
			}

			@Override
			public List<AFX> effects() {
				return Collections.unmodifiableList(effects);
			}
		}
	
	}
	
	
	public static AFB load(ReadableByteChannel in) {
		try {
			return new Impl.AFBImpl(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	void save(WritableByteChannel wtr) throws IOException;

	public static AFB load(Path path) {
		try (var in = Files.newByteChannel(path)) {
			return load(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	void add(AFX element);

	AFX get(int i);

	int indexOf(AFX effect);

	boolean remove(AFX effect);

	int size();

	void add(int index, AFX effect);

	boolean contains(AFX afx);

	List<AFX> effects();
}
