package uk.co.bithatch.jspeccy.commands;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.jspeccy.editor.TapeBrowser;

public class StopHandler extends AbstractTapeHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, TapeBrowser ase) {
		ase.stop();
		return null;
	}
}
