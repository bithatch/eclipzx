package uk.co.bithatch.zxbasic.ui.syntaxcoloring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor;
import org.eclipse.xtext.ui.editor.utils.TextStyle;

public class ZXBasicHighlightingConfiguration extends DefaultHighlightingConfiguration {

    public static final String BASIC_COMMENT_ID = "basic-comment";
    public static final String BASIC_COMMENT_DISPLAY_NAME = "BASIC Comment";
    public static final String LINE_NUMBER_ID = "line-number";
    public static final String LINE_NUMBER_DISPLAY_NAME = "Line Number";
    public static final String LABEL_ID = "label";
    public static final String LABEL_DISPLAY_NAME = "Label";
    public static final String PP_ID = "pp";
    public static final String PP_DISPLAY_NAME = "Other Preprocessor Instructions";
    public static final String INCLUDE_ID = "include";
    public static final String INCLUDE_DISPLAY_NAME = "Include";
    public static final String MACRO_ID = "macro";
    public static final String MACRO_DISPLAY_NAME = "Macros";
    public static final String ASSEMBLY_ID = "assembly";
    public static final String ASSEMBLY_DISPLAY_NAME = "Assembly";
    public static final String MACRO_ID_ID = "excluded";
    public static final String MACRO_ID_DISPLAY_NAME = "Excluded Macro Content";

    @Override
    public void configure(IHighlightingConfigurationAcceptor acceptor) {
        super.configure(acceptor);
        acceptor.acceptDefaultHighlighting(BASIC_COMMENT_ID, BASIC_COMMENT_DISPLAY_NAME, basicCommentStyle());
        acceptor.acceptDefaultHighlighting(LINE_NUMBER_ID, LINE_NUMBER_DISPLAY_NAME, lineNumberStyle());
        acceptor.acceptDefaultHighlighting(LABEL_ID, LABEL_DISPLAY_NAME, labelStyle());
        acceptor.acceptDefaultHighlighting(PP_ID, PP_DISPLAY_NAME, ppStyle());
        acceptor.acceptDefaultHighlighting(INCLUDE_ID, INCLUDE_DISPLAY_NAME, includeStyle());
        acceptor.acceptDefaultHighlighting(ASSEMBLY_ID, ASSEMBLY_DISPLAY_NAME, assemblyStyle());
        acceptor.acceptDefaultHighlighting(MACRO_ID, MACRO_DISPLAY_NAME, macroStyle());
        acceptor.acceptDefaultHighlighting(MACRO_ID_ID, MACRO_ID_DISPLAY_NAME, macroIdStyle());
    }

    private TextStyle basicCommentStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 128, 0)); // Dark green
        textStyle.setStyle(SWT.ITALIC);
        return textStyle;
    }

    private TextStyle lineNumberStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(128, 128, 128)); // Gray
        textStyle.setStyle(SWT.ITALIC);
        return textStyle;
    }
    
    private TextStyle labelStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 140, 255));
        textStyle.setStyle(SWT.UNDERLINE_LINK);
        return textStyle;
    }
    
    private TextStyle ppStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(128, 0, 128)); // Magenta
        textStyle.setStyle(SWT.ITALIC);
        return textStyle;
    }
    
    private TextStyle macroStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(128, 0, 128)); // Magenta
        textStyle.setStyle(SWT.BOLD);
        return textStyle;
    }
    
    private TextStyle includeStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(128, 0, 128)); // Dark Magenta
        textStyle.setStyle(SWT.BOLD | SWT.ITALIC);
        return textStyle;
    }
    
    private TextStyle assemblyStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 110, 110)); // Dark cyan
        textStyle.setStyle(SWT.BOLD);
        return textStyle;
    }
    
    private TextStyle macroIdStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(255, 0, 255)); // Magenta
        textStyle.setStyle(SWT.ITALIC);
        return textStyle;
    }
}
