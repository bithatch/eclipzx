package uk.co.bithatch.eclipzoxo.commands;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.eclipzoxo.views.EmulatorView;

public class InsertROMFromFileSystemHandler extends AbstractEmulatorHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, EmulatorView emulator) {
		try {
			emulator.getOrCreateEmulator().insertROMFromFileSystem();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return null;
	}

}
