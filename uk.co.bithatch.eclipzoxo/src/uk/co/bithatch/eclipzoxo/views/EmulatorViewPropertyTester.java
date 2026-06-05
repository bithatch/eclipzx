package uk.co.bithatch.eclipzoxo.views;

import static java.lang.String.valueOf;

import org.eclipse.core.expressions.PropertyTester;

import uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferencesAccess;
import uk.co.bithatch.zoxo.interface1.Interface1AddOn;
import uk.co.bithatch.zoxo.system.Tape;

public class EmulatorViewPropertyTester extends PropertyTester {
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!(receiver instanceof EmulatorView view))
			return false;

		var expectedStr = expectedValue == null ? "true" : String.valueOf(expectedValue);
		var emulator = view.getSelectedEmulator();

		if ("microdrives".equals(property)) {
			return String.valueOf(ZoxoPreferencesAccess.get().isEnabled(Interface1AddOn.class))
					.equals(expectedStr);
		} else if(emulator != null) {
			if ("activeMedia".equals(property)) {
				var media = emulator.getActiveMedia();
				if (expectedStr.equals("true")) {
					return media != null;
				}
				return expectedStr.equals(media);
			} else if ("tapeRunning".equals(property)) {
				return valueOf(emulator.tape().map(Tape::isTapeRunning).orElse(false)).equals(expectedStr);
			} else if ("tapeInserted".equals(property)) {
				return valueOf(emulator.tape().map(Tape::isTapeInserted).orElse(false)).equals(expectedStr);
			} else if ("tapePlaying".equals(property)) {
				return valueOf(emulator.tape().map(Tape::isTapePlaying).orElse(false)).equals(expectedStr);
			} else if ("tapeRecording".equals(property)) {
				return valueOf(emulator.tape().map(Tape::isTapeRecording).orElse(false)).equals(expectedStr);
			} else if ("tapeReady".equals(property)) {
				return valueOf(emulator.tape().map(Tape::isTapeReady).orElse(false)).equals(expectedStr);
			}
		}
		return false;
	}
}
