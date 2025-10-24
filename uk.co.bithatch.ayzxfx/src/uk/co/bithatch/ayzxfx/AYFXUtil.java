package uk.co.bithatch.ayzxfx;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

public class AYFXUtil {
	private static final double A4_FREQ = 440.0;
	private static final int A4_INDEX = 57; // MIDI note number for A4

	public static int frequencyToAYPeriod(double freq) {
		return (int) Math.round(1_750_000.0 / (16 * freq));
	}

	public static double noteToFrequency(int noteIndex) {
		return A4_FREQ * Math.pow(2, (noteIndex - A4_INDEX) / 12.0);
	}
	
	public static int snapToClosestNotePeriod(int approxPeriod) {
	    double bestDistance = Double.MAX_VALUE;
	    int bestPeriod = approxPeriod;

	    for (int i = 21; i <= 108; i++) { // MIDI notes from A0 to C8
	        double freq = noteToFrequency(i);
	        int p = frequencyToAYPeriod(freq);
	        double distance = Math.abs(approxPeriod - p);
	        if (distance < bestDistance) {
	            bestDistance = distance;
	            bestPeriod = p;
	        }
	    }
	    return bestPeriod;
	}


	public static int cString(String str, ByteBuffer buf) {
		try {
			byte[] bytes = str.getBytes("US-ASCII");
			buf.put(bytes);
			buf.put((byte) 0);
			return bytes.length + 1;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String cString(ByteBuffer data) {
		var nextString = data.slice();
		int i;
		for (i = 0; data.hasRemaining() && data.get() != 0x00; i++) {
		}
		nextString.limit(i);
		return StandardCharsets.US_ASCII.decode(nextString).toString();
	}
	
	public static void refreshFileInExplorer(IFile file) {
		var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		var projectExplorer = (CommonNavigator) page.findView("org.eclipse.ui.navigator.ProjectExplorer");
		if (projectExplorer != null) {
		    var viewer = projectExplorer.getCommonViewer();
		    viewer.refresh(file);
		}
	}
}
