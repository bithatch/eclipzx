package uk.co.bithatch.eclipzoxo.commands;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.eclipzoxo.views.EmulatorView;

public class NMIHandler extends AbstractEmulatorHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, EmulatorView emulator) {
		emulator.getOrCreateEmulator().nmi();
		return null;
	}

}
