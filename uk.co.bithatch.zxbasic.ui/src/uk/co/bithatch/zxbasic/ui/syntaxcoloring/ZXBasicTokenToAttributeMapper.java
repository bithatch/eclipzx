package uk.co.bithatch.zxbasic.ui.syntaxcoloring;

import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultAntlrTokenToAttributeIdMapper;

public class ZXBasicTokenToAttributeMapper extends DefaultAntlrTokenToAttributeIdMapper {

    @Override
    protected String calculateId(String tokenName, int tokenType) {
        if ("RULE_WS".equals(tokenName) || "RULE_REM_LINE".equals(tokenName) || "RULE_BASIC_COMMENT_LINE".equals(tokenName)) {
            return ZXBasicHighlightingConfiguration.BASIC_COMMENT_ID;
        }
        else if ("RULE_ASM".equals(tokenName)) {
            return ZXBasicHighlightingConfiguration.ASSEMBLY_ID;
        }
        else if ("RULE_PP_DEFINE".equals(tokenName) || "RULE_PP_IF".equals(tokenName)) {
            return ZXBasicHighlightingConfiguration.MACRO_ID;
        }
        else if (tokenName.startsWith("RULE_MACRO_ID")) {
            return ZXBasicHighlightingConfiguration.MACRO_ID_ID;
        }
        else if (tokenName.startsWith("RULE_PP_")) {
            return ZXBasicHighlightingConfiguration.PP_ID;
        }
        return super.calculateId(tokenName, tokenType);
    }
}
