package uk.co.bithatch.eclipz80.ui.syntaxcoloring;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ide.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.util.CancelIndicator;

import uk.co.bithatch.eclipz80.asm.AsmAlignDirective;
import uk.co.bithatch.eclipz80.asm.AsmAssumeDirective;
import uk.co.bithatch.eclipz80.asm.AsmBrkStatement;
import uk.co.bithatch.eclipz80.asm.AsmBrlcStatement;
import uk.co.bithatch.eclipz80.asm.AsmBslaStatement;
import uk.co.bithatch.eclipz80.asm.AsmBsraStatement;
import uk.co.bithatch.eclipz80.asm.AsmBsrfStatement;
import uk.co.bithatch.eclipz80.asm.AsmBsrlStatement;
import uk.co.bithatch.eclipz80.asm.AsmCallOzDirective;
import uk.co.bithatch.eclipz80.asm.AsmCallPkgDirective;
import uk.co.bithatch.eclipz80.asm.AsmCondition;
import uk.co.bithatch.eclipz80.asm.AsmCopperMoveDirective;
import uk.co.bithatch.eclipz80.asm.AsmCopperNopDirective;
import uk.co.bithatch.eclipz80.asm.AsmCopperStopDirective;
import uk.co.bithatch.eclipz80.asm.AsmCopperWaitDirective;
import uk.co.bithatch.eclipz80.asm.AsmDMAWR0Directive;
import uk.co.bithatch.eclipz80.asm.AsmDMAWR1Directive;
import uk.co.bithatch.eclipz80.asm.AsmDMAWR2Directive;
import uk.co.bithatch.eclipz80.asm.AsmDMAWR3Directive;
import uk.co.bithatch.eclipz80.asm.AsmDMAWR4Directive;
import uk.co.bithatch.eclipz80.asm.AsmDMAWR5Directive;
import uk.co.bithatch.eclipz80.asm.AsmDMAWR6Directive;
import uk.co.bithatch.eclipz80.asm.AsmDataDefineGroup;
import uk.co.bithatch.eclipz80.asm.AsmDataDefineVars;
import uk.co.bithatch.eclipz80.asm.AsmDefByteDirective;
import uk.co.bithatch.eclipz80.asm.AsmDefDWordDirective;
import uk.co.bithatch.eclipz80.asm.AsmDefPointerDirective;
import uk.co.bithatch.eclipz80.asm.AsmDefSpaceDirective;
import uk.co.bithatch.eclipz80.asm.AsmDefTermStringDirective;
import uk.co.bithatch.eclipz80.asm.AsmDefWordBEDirective;
import uk.co.bithatch.eclipz80.asm.AsmDefWordDirective;
import uk.co.bithatch.eclipz80.asm.AsmExternDirective;
import uk.co.bithatch.eclipz80.asm.AsmImStatement;
import uk.co.bithatch.eclipz80.asm.AsmInclude;
import uk.co.bithatch.eclipz80.asm.AsmLabel;
import uk.co.bithatch.eclipz80.asm.AsmLabelDef;
import uk.co.bithatch.eclipz80.asm.AsmLddrxStatement;
import uk.co.bithatch.eclipz80.asm.AsmLddxStatement;
import uk.co.bithatch.eclipz80.asm.AsmLdirxStatement;
import uk.co.bithatch.eclipz80.asm.AsmLdixStatement;
import uk.co.bithatch.eclipz80.asm.AsmLdpirxStatement;
import uk.co.bithatch.eclipz80.asm.AsmLdwsStatement;
import uk.co.bithatch.eclipz80.asm.AsmMirrorStatement;
import uk.co.bithatch.eclipz80.asm.AsmMmuStatement;
import uk.co.bithatch.eclipz80.asm.AsmModule;
import uk.co.bithatch.eclipz80.asm.AsmMulStatement;
import uk.co.bithatch.eclipz80.asm.AsmNextReg;
import uk.co.bithatch.eclipz80.asm.AsmNumericLabelLine;
import uk.co.bithatch.eclipz80.asm.AsmOrg;
import uk.co.bithatch.eclipz80.asm.AsmOutinbStatement;
import uk.co.bithatch.eclipz80.asm.AsmPixeladStatement;
import uk.co.bithatch.eclipz80.asm.AsmPixeldnStatement;
import uk.co.bithatch.eclipz80.asm.AsmProcStatement;
import uk.co.bithatch.eclipz80.asm.AsmRegisterName;
import uk.co.bithatch.eclipz80.asm.AsmSection;
import uk.co.bithatch.eclipz80.asm.AsmSetaeStatement;
import uk.co.bithatch.eclipz80.asm.AsmStatement;
import uk.co.bithatch.eclipz80.asm.AsmSwapnibStatement;
import uk.co.bithatch.eclipz80.asm.AsmTestStatement;
import uk.co.bithatch.eclipzpp.ui.PPResource;
import uk.co.bithatch.eclipzpp.ui.PPSemanticHighlightingCalculator;

public class AsmSemanticHighlightingCalculator extends PPSemanticHighlightingCalculator {

	public AsmSemanticHighlightingCalculator() {
		super(AsmHighlightingConfiguration.PREPROCESSING_ID);
	}
	@Override
	protected void provideHighlights(PPResource resource, IHighlightedPositionAcceptor acceptor,
			CancelIndicator cancelIndicator, TreeIterator<EObject> allContents) {
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
//                    || obj instanceof AsmLabelEQULine || obj instanceof AsmDefcLine
                    || obj instanceof AsmAssumeDirective
                    || obj instanceof AsmSection 
//                    || obj instanceof AsmLocalLine
//                    || obj instanceof AsmCLINE 
//                    || obj instanceof AsmBinaryDirective
                    || obj instanceof AsmCallOzDirective || obj instanceof AsmCallPkgDirective
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
}
