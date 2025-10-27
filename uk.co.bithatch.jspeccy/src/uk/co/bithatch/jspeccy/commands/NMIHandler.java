package uk.co.bithatch.jspeccy.commands;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.jspeccy.views.EmulatorView;

public class NMIHandler extends AbstractEmulatorHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, EmulatorView emulator) {
		emulator.getEmulator().nmi();
		return null;
	}

}
