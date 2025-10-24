package uk.co.bithatch.drawzx.screens;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.commands.IParameterValues;

import uk.co.bithatch.drawzx.editor.EditorFileProperties.ScreenPaintMode;

public class ScreenPaintModeValues implements IParameterValues {

	@Override
	public Map<String, String> getParameterValues() {
		return Arrays.asList(ScreenPaintMode.values()).stream().collect(Collectors.toMap(ScreenPaintMode::name, ScreenPaintMode::description));
	}

}
