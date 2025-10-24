package uk.co.bithatch.zxbasic.ui.syntaxcoloring;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ide.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;

import uk.co.bithatch.zxbasic.basic.Group;
import uk.co.bithatch.zxbasic.basic.PPDefine;
import uk.co.bithatch.zxbasic.basic.PPElif;
import uk.co.bithatch.zxbasic.basic.PPElse;
import uk.co.bithatch.zxbasic.basic.PPEndIf;
import uk.co.bithatch.zxbasic.basic.PPExclude;
import uk.co.bithatch.zxbasic.basic.PPIfdef;
import uk.co.bithatch.zxbasic.basic.PPIfndef;
import uk.co.bithatch.zxbasic.basic.PPInclude;
import uk.co.bithatch.zxbasic.basic.PPUndef;
import uk.co.bithatch.zxbasic.scoping.ScopingUtils;

public class ZXBasicSemanticHighlightingCalculator implements ISemanticHighlightingCalculator {

    @Override
	public void provideHighlightingFor(XtextResource resource, IHighlightedPositionAcceptor acceptor,
			CancelIndicator cancelIndicator) { 
        if (resource == null || resource.getContents().isEmpty()) return; 

        var root = resource.getContents().get(0); 
 
        // Traverse the AST 
        var allContents = root.eAllContents(); 
        while (allContents.hasNext()) {
            EObject obj = allContents.next(); 
            if(obj instanceof PPInclude) {
                var node = NodeModelUtils.findActualNodeFor(obj);
                acceptor.addPosition(node.getOffset(), 8, ZXBasicHighlightingConfiguration.INCLUDE_ID);
            } 
            else if(obj instanceof PPExclude exclude) {
                var node = NodeModelUtils.findActualNodeFor(obj);
                acceptor.addPosition(node.getOffset(), exclude.getCharacters() + 8, ZXBasicHighlightingConfiguration.MACRO_ID_ID);
            }
            else if(obj instanceof PPElse || obj instanceof PPElif || obj instanceof PPEndIf) {
                var node = NodeModelUtils.findActualNodeFor(obj);
                acceptor.addPosition(node.getOffset(), 6, ZXBasicHighlightingConfiguration.MACRO_ID);
            }
            else if(obj instanceof PPUndef || obj instanceof PPIfdef) {
                var node = NodeModelUtils.findActualNodeFor(obj);
                acceptor.addPosition(node.getOffset(), 6, ZXBasicHighlightingConfiguration.MACRO_ID);
            }
            else if(obj instanceof PPIfndef || obj instanceof PPDefine) {
                var node = NodeModelUtils.findActualNodeFor(obj);
                acceptor.addPosition(node.getOffset(), 7, ZXBasicHighlightingConfiguration.MACRO_ID);
            }
            else if(obj instanceof PPElif) {
                var node = NodeModelUtils.findActualNodeFor(obj);
                acceptor.addPosition(node.getOffset(), 5, ZXBasicHighlightingConfiguration.MACRO_ID);
            }
            else if (obj instanceof Group group && ScopingUtils.hasNumber(group)) {
                var node = NodeModelUtils.findActualNodeFor(group);
                if(node != null) {
                    acceptor.addPosition(node.getOffset(), ScopingUtils.numberOrLabel(group).length(), ZXBasicHighlightingConfiguration.LINE_NUMBER_ID);
                }
            } 
            else if (obj instanceof Group group && ScopingUtils.hasLabel(group)) {
	            var node = NodeModelUtils.findActualNodeFor(group);
	            if(node != null) {
	                acceptor.addPosition(node.getOffset(), ScopingUtils.numberOrLabel(group).length(), ZXBasicHighlightingConfiguration.LABEL_ID);
	            }
            }
        }
    }
}
