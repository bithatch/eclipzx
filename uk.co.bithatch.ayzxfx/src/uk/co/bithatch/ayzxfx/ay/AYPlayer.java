package uk.co.bithatch.ayzxfx.ay;

import java.io.Closeable;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

import uk.co.bithatch.ayzxfx.jspeccy.AY8912;
import uk.co.bithatch.ayzxfx.jspeccy.MachineTypes;

public class AYPlayer implements Runnable, Closeable {
	
	public final static class Builder {
		private int frameRate = 50;
		private int frequency = 44100;
		private final AFX afx;
		private Optional<BiConsumer<Integer, AFXFrame>> onFrame = Optional.empty();
		private MachineTypes machineType = MachineTypes.SPECTRUM128K;
		
		public Builder(AFX afx) {
			this.afx = afx;
		}
		
		public Builder onFrame(BiConsumer<Integer, AFXFrame> onFrame) {
			this.onFrame = Optional.of(onFrame);
			return this;
		}
		
		public Builder withMachineType(MachineTypes machineType) {
			this.machineType = machineType;
			return this;
		}

		public Builder withFrequence(int frequency) {
			this.frequency = frequency;
			return this;
		}
		
		public Builder withFrameRate(int frameRate) {
			if(frameRate == 50 || frameRate == 60) {
				this.frameRate = frameRate;
				return this;
			}
			else
				throw new IllegalArgumentException("Must be 50 or 60.");
		}

		public AYPlayer build() {
			return new AYPlayer(this);
		}
	}

	private final AFX afx;

    private final AY8912 ay = new AY8912();
    private final int[] bufA;
    private final int[] bufB;
    private final int[] bufC;
	private final Optional<BiConsumer<Integer, AFXFrame>> onFrame;

	private AYPlayer(Builder bldr) {
		this.afx = bldr.afx;
		this.onFrame = bldr.onFrame;
	    
        ay.setSpectrumModel(bldr.machineType);
        ay.setFrameRate(bldr.frameRate);
        ay.setAudioFreq(bldr.frequency);

	    bufA = new int[ay.getSamplesPerFrame()];
	    bufB = new int[ay.getSamplesPerFrame()];
	    bufC = new int[ay.getSamplesPerFrame()];
	    
        ay.setBufferChannels(bufA, bufB, bufC);
	}

	public AFX afx() {
		return afx;
	}

	@Override
	public void close() {
	}

	@Override
	public void run() {

        var format = new AudioFormat(ay.getAudioFreq(), 8, 1, true, false);
		try {
			var line = AudioSystem.getSourceDataLine(format);
			try {
		        line.open(format);
		        line.start();
		
		        var index = 0;
		        for (var effect : afx.frames()) {
		            applyEffect(effect);
		            ay.updateAY(69888);
		            ay.endFrame();
		
		            byte[] pcm = new byte[ay.getSamplesPerFrame()];
		            for (int i = 0; i < ay.getSamplesPerFrame(); i++) {
		                int sample = bufA[i];
		                pcm[i] = (byte) Math.max(-128, Math.min(127, (sample >> 6) - 64));
		            }
		            line.write(pcm, 0, pcm.length);
		            var fi = index;
		            onFrame.ifPresent(of -> of.accept(fi, effect));
		            index++;
		        }
		
		        line.drain();
			}
			finally {
				line.close();
			}
		} catch (LineUnavailableException e) {
			throw new IllegalStateException(e);
		}
		
	}
	
	private void applyEffect(AFXFrame effect) {
        int period = effect.period();
        int noise = effect.noise();
        boolean t = effect.t();
        boolean n = effect.n();
        int volume = effect.volume();

        ay.setAddressLatch(0);
        ay.writeRegister(period & 0xFF);
        ay.setAddressLatch(1);
        ay.writeRegister((period >> 8) & 0x0F);

        ay.setAddressLatch(6);
        ay.writeRegister(noise & 0x1F);

        int mixer = 0xFF;
        if (t) mixer &= ~0x01;
        if (n) mixer &= ~0x08;
        ay.setAddressLatch(7);
        ay.writeRegister(mixer);

        ay.setAddressLatch(8);
        ay.writeRegister(volume & 0x0F);
    }

}
