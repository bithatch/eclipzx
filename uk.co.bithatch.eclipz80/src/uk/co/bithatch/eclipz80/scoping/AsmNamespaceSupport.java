package uk.co.bithatch.eclipz80.scoping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.EObject;

import uk.co.bithatch.eclipz80.asm.AsmLine;
import uk.co.bithatch.eclipz80.asm.AsmProgram;
import uk.co.bithatch.eclipz80.asm.AsmPushNamespace;
import uk.co.bithatch.eclipz80.asm.AsmStatement;
import uk.co.bithatch.eclipz80.asm.LabelledLine;
import uk.co.bithatch.eclipz80.asm.NumberedLine;
import uk.co.bithatch.eclipz80.asm.Pop;
import uk.co.bithatch.eclipz80.asm.Push;

/**
 * Utilities for computing active PUSH/POP NAMESPACE state from line order.
 */
public final class AsmNamespaceSupport {

	private AsmNamespaceSupport() {
	}

	public static List<String> namespacesBefore(EObject context) {
		AsmProgram program = findProgram(context);
		AsmLine topLevelLine = findTopLevelLine(context);
		if (program == null || topLevelLine == null) {
			return Collections.emptyList();
		}

		List<String> namespaces = new ArrayList<>();
		for (AsmLine line : program.getLines()) {
			if (line == topLevelLine) {
				break;
			}
			applyLineNamespaceOps(line, namespaces, null);
		}
		return List.copyOf(namespaces);
	}

	public static void forEachUnmatchedPopNamespace(AsmProgram program, Consumer<Pop> consumer) {
		if (program == null || consumer == null) {
			return;
		}
		List<String> namespaces = new ArrayList<>();
		for (AsmLine line : program.getLines()) {
			applyLineNamespaceOps(line, namespaces, consumer);
		}
	}

	public static String qualifyRelativeName(EObject context, String normalizedName) {
		if (normalizedName == null || normalizedName.isEmpty()) {
			return normalizedName;
		}
		// A dotted label is treated as already qualified.
		if (normalizedName.indexOf('.') >= 0) {
			return normalizedName;
		}
		List<String> namespaces = namespacesBefore(context);
		if (namespaces.isEmpty()) {
			return normalizedName;
		}
		return String.join(".", namespaces) + '.' + normalizedName;
	}

	private static void applyLineNamespaceOps(AsmLine line, List<String> namespaces, Consumer<Pop> unmatched) {
		if (line instanceof NumberedLine numbered) {
			if (numbered.getLine() != null) {
				applyLineNamespaceOps(numbered.getLine(), namespaces, unmatched);
			}
			return;
		}
		if (!(line instanceof LabelledLine labelled)) {
			return;
		}
		for (AsmStatement statement : labelled.getStatements()) {
			if (statement instanceof Push push) {
				AsmPushNamespace ns = push.getNamespace();
				if (ns != null && ns.getImportedNamespace() != null && !ns.getImportedNamespace().isBlank()) {
					namespaces.add(ns.getImportedNamespace());
				}
			}
			else if (statement instanceof Pop pop && pop.getRegister() == null) {
				if (namespaces.isEmpty()) {
					if (unmatched != null) {
						unmatched.accept(pop);
					}
				}
				else {
					namespaces.remove(namespaces.size() - 1);
				}
			}
		}
	}

	private static AsmProgram findProgram(EObject context) {
		for (EObject cursor = context; cursor != null; cursor = cursor.eContainer()) {
			if (cursor instanceof AsmProgram p) {
				return p;
			}
		}
		return null;
	}

	private static AsmLine findTopLevelLine(EObject context) {
		for (EObject cursor = context; cursor != null; cursor = cursor.eContainer()) {
			if (cursor instanceof AsmLine line && line.eContainer() instanceof AsmProgram) {
				return line;
			}
		}
		return null;
	}
}
