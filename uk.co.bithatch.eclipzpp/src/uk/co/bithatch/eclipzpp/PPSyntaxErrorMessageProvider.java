package uk.co.bithatch.eclipzpp;

import org.antlr.runtime.NoViableAltException;
import org.eclipse.xtext.nodemodel.SyntaxErrorMessage;
import org.eclipse.xtext.parser.antlr.SyntaxErrorMessageProvider;

import com.google.inject.Inject;

public class PPSyntaxErrorMessageProvider extends SyntaxErrorMessageProvider {
	@Inject
	private IReferenceIndex referenceIndex;

	@Override
	public SyntaxErrorMessage getSyntaxErrorMessage(IParserErrorContext context) {
		var rex = context.getRecognitionException();
		if (rex instanceof NoViableAltException nvae) {
			var offending = nvae.token.getText();
			if (offending == null) {
				return null;
			}
			offending = offending.trim();
			if (offending.equals("")) {
				return null;
			}
			if (offending != null && referenceIndex.isDefined(offending)) {
				return null;
			} else {
				return new SyntaxErrorMessage("Undefined macro `" + offending + "`", "macro.undefined");
			}
		}
		return super.getSyntaxErrorMessage(context);
	}
}
