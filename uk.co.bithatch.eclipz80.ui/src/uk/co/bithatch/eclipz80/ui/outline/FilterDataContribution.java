package uk.co.bithatch.eclipz80.ui.outline;

import org.eclipse.jface.action.Action;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.actions.AbstractFilterOutlineContribution;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;

import uk.co.bithatch.eclipzpp.ui.PPUiActivator;

public class FilterDataContribution extends AbstractFilterOutlineContribution {

	public static final String PREFERENCE_KEY = "asm.ui.outline.filterData";

	@Override
	protected boolean apply(IOutlineNode node) {
		return !(node instanceof EObjectNode enode)
				|| !AsmOutlineModel.isData(enode);
	}

	@Override
	public String getPreferenceKey() {
		return PREFERENCE_KEY;
	}

	@Override
	protected void configureAction(Action action) {
		action.setText("Hide data definitions");
		action.setDescription("Hide elements that inject data at the current address");
		action.setToolTipText("Hide elements that inject data at the current address");
		action.setImageDescriptor(PPUiActivator.getDefault().getImageRegistry().getDescriptor(PPUiActivator.DATA_PATH));
	}
}