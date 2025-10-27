package uk.co.bithatch.jspeccy.views;

import static java.lang.String.valueOf;

import org.eclipse.core.expressions.PropertyTester;

import uk.co.bithatch.jspeccy.Activator;

public class EmulatorViewPropertyTester extends PropertyTester {
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!(receiver instanceof EmulatorView view))
			return false;

		var expectedStr = expectedValue == null ? "true" : String.valueOf(expectedValue);
		var emulator = view.getSelectedEmulator();

		if ("microdrives".equals(property)) {
			return String.valueOf(Activator.getDefault().settings().jspeccy().getInterface1Settings().isConnectedIF1())
					.equals(expectedStr);
		} else if(emulator != null) {
			if ("activeMedia".equals(property)) {
				var media = emulator.getActiveMedia();
				if (expectedStr.equals("true")) {
					return media != null;
				}
				return expectedStr.equals(media);
			} else if ("tapeRunning".equals(property)) {
				return valueOf(emulator.isTapeRunning()).equals(expectedStr);
			} else if ("tapeInserted".equals(property)) {
				return valueOf(emulator.isTapeInserted()).equals(expectedStr);
			} else if ("tapePlaying".equals(property)) {
				return valueOf(emulator.isTapePlaying()).equals(expectedStr);
			} else if ("tapeRecording".equals(property)) {
				return valueOf(emulator.isTapeRecording()).equals(expectedStr);
			} else if ("tapeReady".equals(property)) {
				return valueOf(emulator.isTapeReady()).equals(expectedStr);
			}
		}
		return false;
	}
}
