package uk.co.bithatch.zxbasic;

import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.eclipse.xtext.nodemodel.SyntaxErrorMessage;
import org.eclipse.xtext.parser.antlr.SyntaxErrorMessageProvider;

import com.google.inject.Inject;


public class ZxbasicSyntaxErrorMessageProvider extends SyntaxErrorMessageProvider {
    @Inject
    private IReferenceIndex referenceIndex;

    @Override
    public SyntaxErrorMessage getSyntaxErrorMessage(IParserErrorContext context) {
        RecognitionException rex = context.getRecognitionException();
		if (rex instanceof NoViableAltException nvae) {
            String offending = nvae.token.getText();
            if(offending == null)
            	return null;
            offending = offending.trim();
            if(offending.equals(""))
            	return null;
            System.out.println(">> " + offending + " : " + nvae.node + " : " + nvae.token);
            ;
            if (offending != null && referenceIndex.isDefined(offending)) {
                return null;
            }
            else {
                return new SyntaxErrorMessage("Undefined macro `" + offending + "`", "macro.undefined");
            }
        }
        return super.getSyntaxErrorMessage(context);
    }
}
