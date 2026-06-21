package uk.co.bithatch.eclipz80.ui.outline;

import org.eclipse.jface.action.Action;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.actions.AbstractFilterOutlineContribution;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;

import uk.co.bithatch.eclipzpp.ui.PPUiActivator;

public class FilterPreprocessorContribution extends AbstractFilterOutlineContribution {

	public static final String PREFERENCE_KEY = "asm.ui.outline.filterPreprocessor";

	@Override
	protected boolean apply(IOutlineNode node) {
		return !(node instanceof EObjectNode enode)
				|| !AsmOutlineModel.isPP(enode);
	}

	@Override
	public String getPreferenceKey() {
		return PREFERENCE_KEY;
	}

	@Override
	protected void configureAction(Action action) {
		action.setText("Hide preprocess instructions");
		action.setDescription("Hide instructions that are interpreted by the preprocessor");
		action.setToolTipText("Hide instructions that are interpreted by the preprocessor");
		action.setImageDescriptor(PPUiActivator.getDefault().getImageRegistry().getDescriptor(PPUiActivator.PP_PATH));
	}
}