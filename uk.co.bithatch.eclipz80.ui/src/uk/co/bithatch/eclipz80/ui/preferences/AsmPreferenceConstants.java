package uk.co.bithatch.eclipz80.ui.preferences;

public interface AsmPreferenceConstants {
	String COMPILER = "compiler";
	String OUTPUT_PATH = "compiler.outputPath";
	String ASSEMBLER_MODE = "compiler.assemblerMode";
	String EXTERNAL_COMMAND = "compiler.externalCommand";
	String GENERATE_MAP = "compiler.generateMap";
	String DEFINES = "compiler.defines";

	String ASSEMBLER_MODE_BUILTIN = "builtin";
	String ASSEMBLER_MODE_EXTERNAL = "external";

	/** Separator used to store multiple defines in a single preference string. */
	String DEFINES_SEPARATOR = "\n";
}
