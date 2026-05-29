package uk.co.bithatch.eclipz80.ui.syntaxcoloring;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ide.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;

import uk.co.bithatch.eclipz80.asm.*;

public class AsmSemanticHighlightingCalculator implements ISemanticHighlightingCalculator {

    @Override
    public void provideHighlightingFor(XtextResource resource, IHighlightedPositionAcceptor acceptor,
            CancelIndicator cancelIndicator) {
        if (resource == null || resource.getContents().isEmpty())
            return;

        var root = resource.getContents().get(0);
        var allContents = root.eAllContents();

        while (allContents.hasNext()) {
            if (cancelIndicator.isCanceled())
                return;

            EObject obj = allContents.next();

            // Label definitions
            if (obj instanceof AsmLabelDef) {
                highlightNode(obj, acceptor, AsmHighlightingConfiguration.LABEL_DEF_ID);
            }
            // Label references
            else if (obj instanceof AsmLabel) {
                highlightNode(obj, acceptor, AsmHighlightingConfiguration.LABEL_REF_ID);
            }
            // Numeric labels
            else if (obj instanceof AsmNumericLabelLine) {
                highlightNode(obj, acceptor, AsmHighlightingConfiguration.NUMERIC_LABEL_ID);
            }
            // Register names
            else if (obj instanceof AsmRegisterName) {
                highlightNode(obj, acceptor, AsmHighlightingConfiguration.REGISTER_ID);
            }
            // Condition flags
            else if (obj instanceof AsmCondition) {
                highlightNode(obj, acceptor, AsmHighlightingConfiguration.CONDITION_ID);
            }
            // Include directive
            else if (obj instanceof AsmInclude) {
                highlightKeyword(obj, acceptor, AsmHighlightingConfiguration.INCLUDE_ID);
            }
            // Copper directives
            else if (obj instanceof AsmCopperWaitDirective || obj instanceof AsmCopperMoveDirective
                    || obj instanceof AsmCopperStopDirective || obj instanceof AsmCopperNopDirective) {
                highlightKeyword(obj, acceptor, AsmHighlightingConfiguration.COPPER_DMA_ID);
            }
            // DMA directives
            else if (obj instanceof AsmDMAWR0Directive || obj instanceof AsmDMAWR1Directive
                    || obj instanceof AsmDMAWR2Directive || obj instanceof AsmDMAWR3Directive
                    || obj instanceof AsmDMAWR4Directive || obj instanceof AsmDMAWR5Directive
                    || obj instanceof AsmDMAWR6Directive) {
                highlightKeyword(obj, acceptor, AsmHighlightingConfiguration.COPPER_DMA_ID);
            }
            // Assembler directives: ORG, ALIGN, DEF*, EXTERN, MODULE, SECTION, BINARY/INCBIN,
            // EQU, DEFC, LOCAL, DEFGROUP, DEFVARS, CALL_OZ, CALL_PKG, .ASSUME, C_LINE
            else if (obj instanceof AsmOrg || obj instanceof AsmAlignDirective
                    || obj instanceof AsmDefByteDirective || obj instanceof AsmDefWordDirective
                    || obj instanceof AsmDefWordBEDirective || obj instanceof AsmDefPointerDirective
                    || obj instanceof AsmDefDWordDirective || obj instanceof AsmDefTermStringDirective
                    || obj instanceof AsmDefSpaceDirective || obj instanceof AsmDataDefineGroup
                    || obj instanceof AsmDataDefineVars
                    || obj instanceof AsmExternDirective || obj instanceof AsmModule
                    || obj instanceof AsmSection || obj instanceof AsmBinaryDirective
                    || obj instanceof AsmLabelEQULine || obj instanceof AsmDefcLine
                    || obj instanceof AsmLocalLine
                    || obj instanceof AsmCallOzDirective || obj instanceof AsmCallPkgDirective
                    || obj instanceof AsmAssumeDirective || obj instanceof AsmCLINE
                    || obj instanceof AsmProcStatement || obj instanceof AsmImStatement) {
                highlightKeyword(obj, acceptor, AsmHighlightingConfiguration.DIRECTIVE_ID);
            }
            // Z80N (Next) instructions
            else if (obj instanceof AsmNextReg || obj instanceof AsmSwapnibStatement
                    || obj instanceof AsmMirrorStatement || obj instanceof AsmMulStatement
                    || obj instanceof AsmPixeldnStatement || obj instanceof AsmPixeladStatement
                    || obj instanceof AsmSetaeStatement || obj instanceof AsmOutinbStatement
                    || obj instanceof AsmLdixStatement || obj instanceof AsmLddxStatement
                    || obj instanceof AsmLdirxStatement || obj instanceof AsmLddrxStatement
                    || obj instanceof AsmLdpirxStatement || obj instanceof AsmLdwsStatement
                    || obj instanceof AsmBslaStatement || obj instanceof AsmBsraStatement
                    || obj instanceof AsmBsrlStatement || obj instanceof AsmBsrfStatement
                    || obj instanceof AsmBrlcStatement || obj instanceof AsmTestStatement
                    || obj instanceof AsmBrkStatement || obj instanceof AsmMmuStatement) {
                highlightKeyword(obj, acceptor, AsmHighlightingConfiguration.Z80N_INSTRUCTION_ID);
            }
            // Standard Z80 instructions (everything else that is an AsmStatement)
            else if (obj instanceof AsmStatement) {
                highlightKeyword(obj, acceptor, AsmHighlightingConfiguration.INSTRUCTION_ID);
            }
        }
    }

    /**
     * Highlights the entire node for a given EObject.
     */
    private void highlightNode(EObject obj, IHighlightedPositionAcceptor acceptor, String id) {
        INode node = NodeModelUtils.findActualNodeFor(obj);
        if (node != null) {
            acceptor.addPosition(node.getOffset(), node.getLength(), id);
        }
    }

    /**
     * Highlights only the first keyword token (non-hidden leaf node) of a statement or directive.
     * This ensures only the mnemonic/directive keyword is coloured, not its operands.
     */
    private void highlightKeyword(EObject obj, IHighlightedPositionAcceptor acceptor, String id) {
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
