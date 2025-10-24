package uk.co.bithatch.ayzxfx.ay;

public record AFXFrame(boolean t, boolean n, int period, int noise, int volume) {
	
	
	public AFXFrame() {
		this(false, false, 0, 0, 0);
	}

	public AFXFrame update(int period, int noise, int volume) {
		return new AFXFrame(t, n, period, noise, volume);
	}

	public AFXFrame update(boolean t, Boolean n) {
		return new AFXFrame(t, n, period, noise, volume);
	}

	public AFXFrame copy() {
		return new AFXFrame(t, n, period, noise, volume);
	}
}
