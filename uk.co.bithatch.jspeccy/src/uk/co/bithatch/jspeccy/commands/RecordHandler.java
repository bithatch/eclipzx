package uk.co.bithatch.jspeccy.commands;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.jspeccy.editor.TapeBrowser;

public class RecordHandler extends AbstractTapeHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, TapeBrowser ase) {
		ase.startRecord();
		return null;
	}
}
