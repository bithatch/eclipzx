package uk.co.bithatch.jspeccy.commands;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.jspeccy.views.EmulatorInstance;
import uk.co.bithatch.jspeccy.views.EmulatorView;

public class EjectTapeHandler extends AbstractEmulatorHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, EmulatorView emulator) {
		emulator.selectedEmulator().ifPresent(EmulatorInstance::ejectTape);
		return null;
	}

}
