package uk.co.bithatch.eclipzpp.ui;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ide.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;

public abstract class PPSemanticHighlightingCalculator implements ISemanticHighlightingCalculator {
	
	private final String ppHighlightId;

	protected PPSemanticHighlightingCalculator(String ppHighlightId) {
		this.ppHighlightId = ppHighlightId;
	}

    @Override
    public final void provideHighlightingFor(XtextResource resource, IHighlightedPositionAcceptor acceptor,
            CancelIndicator cancelIndicator) {
        if (resource == null || resource.getContents().isEmpty())
            return;

        var root = resource.getContents().get(0);
        var allContents = root.eAllContents();
		var ppResource = (PPResource)resource;
		var map = ppResource.map();

        provideHighlights(ppResource, acceptor, cancelIndicator, allContents);

        map.hiddenOffsets().forEach((offset, text) -> {
            acceptor.addPosition(offset - 1, text.length() + 1, ppHighlightId);
        });
    }

	protected abstract void provideHighlights(PPResource resource, IHighlightedPositionAcceptor acceptor, CancelIndicator cancelIndicator,
			TreeIterator<EObject> allContents);

	/**
     * Highlights the entire node for a given EObject.
     */
	protected final void highlightNode(EObject obj, IHighlightedPositionAcceptor acceptor, String id) {
        INode node = NodeModelUtils.findActualNodeFor(obj);
        if (node != null) {
            acceptor.addPosition(node.getOffset(), node.getLength(), id);
        }
    }

    /**
     * Highlights only the first keyword token (non-hidden leaf node) of a statement or directive.
     * This ensures only the mnemonic/directive keyword is coloured, not its operands.
     */
	protected final void highlightKeyword(EObject obj, IHighlightedPositionAcceptor acceptor, String id) {
        INode node = NodeModelUtils.findActualNodeFor(obj);
        if (node != null) {
            for (ILeafNode leaf : node.getLeafNodes()) {
                if (!leaf.isHidden()) {
                    acceptor.addPosition(leaf.getOffset(), leaf.getLength(), id);
                    return;
                }
            }
        }
    }
}
