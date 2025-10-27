package uk.co.bithatch.jspeccy.commands;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.jspeccy.views.EmulatorView;

public class InsertTapeFromFileSystemHandler extends AbstractEmulatorHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, EmulatorView emulator) {
		try {
			emulator.getEmulator().insertTapeFromFileSystem();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return null;
	}

}
