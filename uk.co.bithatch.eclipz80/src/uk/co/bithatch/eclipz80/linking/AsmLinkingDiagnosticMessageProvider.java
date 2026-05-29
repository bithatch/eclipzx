package uk.co.bithatch.eclipz80.linking;

import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;

/**
 * Suppresses "Couldn't resolve reference" errors for known external symbols
 * such as ZX Basic core runtime labels (.core.__*) and built-in identifiers.
 */
public class AsmLinkingDiagnosticMessageProvider extends LinkingDiagnosticMessageProvider {

	@Override
	public DiagnosticMessage getUnresolvedProxyMessage(ILinkingDiagnosticContext context) {
		String linkText = context.getLinkText();
		if (linkText != null && isKnownExternal(linkText)) {
			return null; // suppress the error
		}
		return super.getUnresolvedProxyMessage(context);
	}

	private boolean isKnownExternal(String name) {
		// ZX Basic core runtime symbols
		if (name.startsWith(".core.") || name.startsWith("core.__")) {
			return true;
		}
		// ZX Basic built-in identifiers
		if ("namespace".equals(name)) {
			return true;
		}
		// ZX Basic defines
		if(name.equalsIgnoreCase("zxbasic_mem_heap") || 
		   name.equalsIgnoreCase("zxbasic_heap_size")) {
			return true;
		}
		return false;
	}
}
