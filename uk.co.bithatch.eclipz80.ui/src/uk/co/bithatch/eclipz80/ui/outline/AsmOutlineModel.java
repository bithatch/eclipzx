package uk.co.bithatch.eclipz80.ui.outline;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;

import uk.co.bithatch.eclipz80.asm.AsmAlignDirective;
import uk.co.bithatch.eclipz80.asm.AsmAssumeDirective;
import uk.co.bithatch.eclipz80.asm.AsmCallOzDirective;
import uk.co.bithatch.eclipz80.asm.AsmCallPkgDirective;
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
import uk.co.bithatch.eclipz80.asm.AsmDefcDirective;
import uk.co.bithatch.eclipz80.asm.AsmDefine;
import uk.co.bithatch.eclipz80.asm.AsmExternDirective;
import uk.co.bithatch.eclipz80.asm.AsmGroupedDefine;
import uk.co.bithatch.eclipz80.asm.AsmIncBin;
import uk.co.bithatch.eclipz80.asm.AsmInclude;
import uk.co.bithatch.eclipz80.asm.AsmMmuStatement;
import uk.co.bithatch.eclipz80.asm.AsmModule;
import uk.co.bithatch.eclipz80.asm.AsmNextReg;
import uk.co.bithatch.eclipz80.asm.AsmOrg;
import uk.co.bithatch.eclipz80.asm.AsmProcStatement;
import uk.co.bithatch.eclipz80.asm.AsmSection;

public class AsmOutlineModel  {

	public static boolean isConstant(EObject stmt) {
		return stmt instanceof AsmDefcDirective
			|| stmt instanceof AsmGroupedDefine
			|| stmt instanceof AsmDefine
			|| stmt instanceof AsmDataDefineGroup;
	}

	public static boolean isConstant(EObjectNode node) {
		return isAssignableFrom(AsmDefcDirective.class, node) ||
				isAssignableFrom(AsmGroupedDefine.class, node) ||
				isAssignableFrom(AsmDataDefineGroup.class, node);
	}
	
	public static boolean isData(EObjectNode node) {
		return isAssignableFrom(AsmDefByteDirective.class, node) ||
				isAssignableFrom(AsmDefWordDirective.class, node) ||
				isAssignableFrom(AsmDefWordBEDirective.class, node) ||
				isAssignableFrom(AsmDefPointerDirective.class, node) ||
				isAssignableFrom(AsmDefDWordDirective.class, node) ||
				isAssignableFrom(AsmDefTermStringDirective.class, node) ||
				isAssignableFrom(AsmDefSpaceDirective.class, node);
	}
	
	private static boolean isAssignableFrom(Class<?> clazz, EObjectNode node) {
		return clazz.isAssignableFrom(((EObjectNode) node).getEClass().getInstanceClass());
	}

	public static boolean isData(EObject stmt) {

		return     stmt instanceof AsmDefByteDirective
				|| stmt instanceof AsmDefWordDirective
				|| stmt instanceof AsmDefWordBEDirective
				|| stmt instanceof AsmDefPointerDirective
				|| stmt instanceof AsmDefDWordDirective
				|| stmt instanceof AsmDefTermStringDirective
				|| stmt instanceof AsmDefSpaceDirective;
	}
	
	public static boolean isDirective(EObject stmt) {
		return stmt instanceof AsmOrg
				|| stmt instanceof AsmInclude
				|| stmt instanceof AsmIncBin
				|| stmt instanceof AsmDefine
				|| stmt instanceof AsmModule
				|| stmt instanceof AsmSection
				|| stmt instanceof AsmAlignDirective
				|| stmt instanceof AsmAssumeDirective
				|| stmt instanceof AsmCallOzDirective
				|| stmt instanceof AsmCallPkgDirective
				|| stmt instanceof AsmCopperWaitDirective
				|| stmt instanceof AsmCopperMoveDirective
				|| stmt instanceof AsmCopperStopDirective
				|| stmt instanceof AsmCopperNopDirective
				|| stmt instanceof AsmDataDefineGroup
				|| stmt instanceof AsmDataDefineVars
				|| stmt instanceof AsmDMAWR0Directive
				|| stmt instanceof AsmDMAWR1Directive
				|| stmt instanceof AsmDMAWR2Directive
				|| stmt instanceof AsmDMAWR3Directive
				|| stmt instanceof AsmDMAWR4Directive
				|| stmt instanceof AsmDMAWR5Directive
				|| stmt instanceof AsmDMAWR6Directive
				|| stmt instanceof AsmExternDirective
				|| stmt instanceof AsmNextReg
				|| stmt instanceof AsmMmuStatement
				|| stmt instanceof AsmProcStatement
				|| isData(stmt)
				|| isConstant(stmt);
	}
}
