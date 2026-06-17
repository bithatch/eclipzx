package uk.co.bithatch.eclipz80.ui.outline;

import uk.co.bithatch.eclipz80.asm.AsmInclude;
import uk.co.bithatch.eclipz80.asm.LabelledLine;
import uk.co.bithatch.eclipzpp.ui.AbstractPPOutlineModel;
import uk.co.bithatch.eclipzpp.ui.PPUiActivator;

public class AsmOutlineModel extends AbstractPPOutlineModel {

	public AsmOutlineModel() {

		
		add(AsmInclude.class, 
			inc -> "INCLUDE " + inc.getImportURI(), 
			PPUiActivator.getDefault().getImageRegistry().getDescriptor(PPUiActivator.INCLUDE_PATH)
		);
		
		add(LabelledLine.class,
			"LABEL",
			PPUiActivator.getDefault().getImageRegistry().getDescriptor(PPUiActivator.LABEL_PATH),
			LabelledLine::getStatements);
		
//		add(LabelledLine.class,
//			"LABEL",
//			PPUiActivator.getDefault().getImageRegistry().getDescriptor(PPUiActivator.LABEL_PATH),
//			LabelledLine::getStatements);
	}
}
