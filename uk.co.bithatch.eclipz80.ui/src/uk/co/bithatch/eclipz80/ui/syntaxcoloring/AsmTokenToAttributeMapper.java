package uk.co.bithatch.eclipz80.ui.syntaxcoloring;

import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultAntlrTokenToAttributeIdMapper;

public class AsmTokenToAttributeMapper extends DefaultAntlrTokenToAttributeIdMapper {

    @Override
    protected String calculateId(String tokenName, int tokenType) {
        if ("RULE_NUMERIC_LABEL".equals(tokenName)) {
            return AsmHighlightingConfiguration.NUMERIC_LABEL_ID;
        }
        else if ("RULE_HEX_LITERAL".equals(tokenName) || "RULE_BIN_LITERAL".equals(tokenName)) {
            return AsmHighlightingConfiguration.NUMERIC_LITERAL_ID;
        }
        return super.calculateId(tokenName, tokenType);
    }
}
