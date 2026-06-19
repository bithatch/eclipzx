package uk.co.bithatch.eclipz80.ui.outline;

import org.eclipse.jface.action.Action;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.actions.AbstractFilterOutlineContribution;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;

import uk.co.bithatch.eclipzpp.ui.PPUiActivator;

public class FilterLocalSymbolsContribution extends AbstractFilterOutlineContribution {

	public static final String PREFERENCE_KEY = "asm.ui.outline.filterPublicSymbols";

	@Override
	protected boolean apply(IOutlineNode node) {
		return !(node instanceof EObjectNode enode)
				|| !AsmOutlineModel.isConstant(enode);
	}

	@Override
	public String getPreferenceKey() {
		return PREFERENCE_KEY;
	}

	@Override
	protected void configureAction(Action action) {
		action.setText("Hide public symbols");
		action.setDescription("Hide symbols that are exported to other modules");
		action.setToolTipText("Hide symbols that are exported to other modules");
		action.setImageDescriptor(PPUiActivator.getDefault().getImageRegistry().getDescriptor(PPUiActivator.PUBLIC_PATH));
	}
}