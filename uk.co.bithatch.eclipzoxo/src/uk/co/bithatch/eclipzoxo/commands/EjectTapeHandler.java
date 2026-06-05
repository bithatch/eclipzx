package uk.co.bithatch.eclipzoxo.commands;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.eclipzoxo.views.EmulatorView;

public class EjectTapeHandler extends AbstractEmulatorHandler {
	private final static ILog LOG = ILog.of(EjectTapeHandler.class);

	@Override
	protected Object onHandle(ExecutionEvent event, EmulatorView emulator) {
		try {
			emulator.selectedEmulator().tapePlayer().get().getTape().eject();
		} catch (IOException e) {
			LOG.error("Failed to eject tape.", e);
		}
		return null;
	}

}
