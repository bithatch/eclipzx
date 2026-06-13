package uk.co.bithatch.eclipz80.linking;

import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;

/**
 * Suppresses "Couldn't resolve reference" errors for known external symbols
 * such as ZX Basic core runtime labels (.core.__*) and built-in identifiers.
 */
public class AsmLinkingDiagnosticMessageProvider extends LinkingDiagnosticMessageProvider {

//	
}
