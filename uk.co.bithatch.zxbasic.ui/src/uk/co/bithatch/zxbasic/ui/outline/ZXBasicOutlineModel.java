package uk.co.bithatch.zxbasic.ui.outline;

import uk.co.bithatch.eclipzpp.ui.AbstractPPOutlineModel;
import uk.co.bithatch.eclipzpp.ui.PPUiActivator;
import uk.co.bithatch.zxbasic.basic.impl.PPIncludeImpl;

public class ZXBasicOutlineModel extends AbstractPPOutlineModel {

	public ZXBasicOutlineModel() {

		add(PPIncludeImpl.class, 
			inc -> "INCLUDE " + inc.getImportURI(), 
			PPUiActivator.getDefault().getImageRegistry().getDescriptor(PPUiActivator.INCLUDE_PATH)
		);
		
//		add(LabelledLine.class,
//			"LABEL",
//			PPUiActivator.getDefault().getImageRegistry().getDescriptor(PPUiActivator.LABEL_PATH),
//			LabelledLine::getStatements);
	}
}
