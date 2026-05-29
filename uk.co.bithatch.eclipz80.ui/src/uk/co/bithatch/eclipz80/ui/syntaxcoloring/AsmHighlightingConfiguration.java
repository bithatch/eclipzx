package uk.co.bithatch.eclipz80.ui.syntaxcoloring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor;
import org.eclipse.xtext.ui.editor.utils.TextStyle;

public class AsmHighlightingConfiguration extends DefaultHighlightingConfiguration {

    // Label definitions (e.g. "myLabel:")
    public static final String LABEL_DEF_ID = "asm-label-def";
    public static final String LABEL_DEF_DISPLAY_NAME = "Label Definition";

    // Label references
    public static final String LABEL_REF_ID = "asm-label-ref";
    public static final String LABEL_REF_DISPLAY_NAME = "Label Reference";

    // Numeric labels (e.g. "10:")
    public static final String NUMERIC_LABEL_ID = "asm-numeric-label";
    public static final String NUMERIC_LABEL_DISPLAY_NAME = "Numeric Label";

    // CPU registers (A, B, C, HL, IX, etc.)
    public static final String REGISTER_ID = "asm-register";
    public static final String REGISTER_DISPLAY_NAME = "Register";

    // Conditions (NZ, Z, NC, C, PO, PE, P, M)
    public static final String CONDITION_ID = "asm-condition";
    public static final String CONDITION_DISPLAY_NAME = "Condition Flag";

    // Standard Z80 instructions (LD, ADD, JP, CALL, RET, PUSH, POP, etc.)
    public static final String INSTRUCTION_ID = "asm-instruction";
    public static final String INSTRUCTION_DISPLAY_NAME = "Z80 Instruction";

    // Z80N (ZX Spectrum Next) extended instructions (NEXTREG, SWAPNIB, LDIRX, etc.)
    public static final String Z80N_INSTRUCTION_ID = "asm-z80n-instruction";
    public static final String Z80N_INSTRUCTION_DISPLAY_NAME = "Z80N Instruction";

    // Assembler directives (ORG, DEFB, DEFW, ALIGN, EXTERN, MODULE, SECTION, etc.)
    public static final String DIRECTIVE_ID = "asm-directive";
    public static final String DIRECTIVE_DISPLAY_NAME = "Assembler Directive";

    // Include directive
    public static final String INCLUDE_ID = "asm-include";
    public static final String INCLUDE_DISPLAY_NAME = "Include";

    // Copper/DMA directives (CU.WAIT, CU.MOVE, DMA.WR0, etc.)
    public static final String COPPER_DMA_ID = "asm-copper-dma";
    public static final String COPPER_DMA_DISPLAY_NAME = "Copper / DMA Directive";

    // Hex and binary numeric literals
    public static final String NUMERIC_LITERAL_ID = "asm-numeric-literal";
    public static final String NUMERIC_LITERAL_DISPLAY_NAME = "Numeric Literal";

    @Override
    public void configure(IHighlightingConfigurationAcceptor acceptor) {
        super.configure(acceptor);
        acceptor.acceptDefaultHighlighting(LABEL_DEF_ID, LABEL_DEF_DISPLAY_NAME, labelDefStyle());
        acceptor.acceptDefaultHighlighting(LABEL_REF_ID, LABEL_REF_DISPLAY_NAME, labelRefStyle());
        acceptor.acceptDefaultHighlighting(NUMERIC_LABEL_ID, NUMERIC_LABEL_DISPLAY_NAME, numericLabelStyle());
        acceptor.acceptDefaultHighlighting(REGISTER_ID, REGISTER_DISPLAY_NAME, registerStyle());
        acceptor.acceptDefaultHighlighting(CONDITION_ID, CONDITION_DISPLAY_NAME, conditionStyle());
        acceptor.acceptDefaultHighlighting(INSTRUCTION_ID, INSTRUCTION_DISPLAY_NAME, instructionStyle());
        acceptor.acceptDefaultHighlighting(Z80N_INSTRUCTION_ID, Z80N_INSTRUCTION_DISPLAY_NAME, z80nInstructionStyle());
        acceptor.acceptDefaultHighlighting(DIRECTIVE_ID, DIRECTIVE_DISPLAY_NAME, directiveStyle());
        acceptor.acceptDefaultHighlighting(INCLUDE_ID, INCLUDE_DISPLAY_NAME, includeStyle());
        acceptor.acceptDefaultHighlighting(COPPER_DMA_ID, COPPER_DMA_DISPLAY_NAME, copperDmaStyle());
        acceptor.acceptDefaultHighlighting(NUMERIC_LITERAL_ID, NUMERIC_LITERAL_DISPLAY_NAME, numericLiteralStyle());
    }

    private TextStyle labelDefStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 100, 200)); // Blue
        textStyle.setStyle(SWT.BOLD);
        return textStyle;
    }

    private TextStyle labelRefStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 140, 255)); // Light blue
        textStyle.setStyle(SWT.UNDERLINE_LINK);
        return textStyle;
    }

    private TextStyle numericLabelStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(128, 128, 128)); // Gray
        textStyle.setStyle(SWT.ITALIC);
        return textStyle;
    }

    private TextStyle registerStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(150, 0, 0)); // Dark red
        textStyle.setStyle(SWT.BOLD);
        return textStyle;
    }

    private TextStyle conditionStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(180, 100, 0)); // Dark orange
        textStyle.setStyle(SWT.NONE);
        return textStyle;
    }

    private TextStyle instructionStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 0, 180)); // Bold blue
        textStyle.setStyle(SWT.BOLD);
        return textStyle;
    }

    private TextStyle z80nInstructionStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 110, 110)); // Dark cyan/teal
        textStyle.setStyle(SWT.BOLD);
        return textStyle;
    }

    private TextStyle directiveStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(128, 0, 128)); // Magenta
        textStyle.setStyle(SWT.BOLD);
        return textStyle;
    }

    private TextStyle includeStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(128, 0, 128)); // Dark magenta
        textStyle.setStyle(SWT.BOLD | SWT.ITALIC);
        return textStyle;
    }

    private TextStyle copperDmaStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 80, 120)); // Dark blue-cyan
        textStyle.setStyle(SWT.BOLD);
        return textStyle;
    }

    private TextStyle numericLiteralStyle() {
        TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0, 128, 0)); // Dark green
        textStyle.setStyle(SWT.NONE);
        return textStyle;
    }
}
