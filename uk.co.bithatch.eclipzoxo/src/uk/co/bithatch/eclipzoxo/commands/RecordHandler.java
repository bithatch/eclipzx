package uk.co.bithatch.eclipzoxo.commands;

import org.eclipse.core.commands.ExecutionEvent;

import uk.co.bithatch.eclipzoxo.editor.TapeBrowser;

public class RecordHandler extends AbstractTapeHandler {

	@Override
	protected Object onHandle(ExecutionEvent event, TapeBrowser ase) {
		ase.startRecord();
		return null;
	}
}
