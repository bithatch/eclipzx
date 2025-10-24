package uk.co.bithatch.jspeccy.editor;

import org.eclipse.core.expressions.PropertyTester;

public class TapeBrowserPropertyTester extends PropertyTester {
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof TapeBrowser view)) 
        	return false;
		var expectedStr = expectedValue == null ? "true" : String.valueOf(expectedValue);

        if ("running".equals(property)) {
        	return String.valueOf(view.isRunning()).equals(expectedStr);
        }
        else if ("inserted".equals(property)) {
        	return String.valueOf(view.isInserted()).equals(expectedStr);
        }
        else if ("playing".equals(property)) {
        	return String.valueOf(view.isPlaying()).equals(expectedStr);
        }
        else if ("recording".equals(property)) {
        	return String.valueOf(view.isRecording()).equals(expectedStr);
        }
        else if ("ready".equals(property)) {
        	return String.valueOf(view.isReady()).equals(expectedStr);
        }
        return false;
    }
}
