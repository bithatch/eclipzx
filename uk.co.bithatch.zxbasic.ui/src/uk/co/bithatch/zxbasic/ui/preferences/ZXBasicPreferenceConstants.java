package uk.co.bithatch.zxbasic.ui.preferences;

import uk.co.bithatch.bitzx.LanguageSystemPreferenceConstants;

public interface ZXBasicPreferenceConstants extends LanguageSystemPreferenceConstants {
    String PROJECT_SPECIFIC_SETTINGS = "projectSpecificSettings";

	String COMPLIANCE = "compliance";
	String CONTRIBUTED = "contributed";
	String PREPROCESSOR = "preprocessor";
	String DEBUG = "debug";
	String ERRORS_AND_WARNINGS = "errorsAndWarnings";
	String SDKS = "sdks";
	String USER_LIBRARIES = "userLibraries";
//	String EXTERNAL_EMULATORS = "externalEmulators";
	
	String DEPENDENCIES = "contributed.dependencies";

    String DEFINES = "preprocessor.defines";
    
	String SDK_PATHS = "sdks.sdkPaths";
    String SDK_PYTHON_LOCATION = "sdks.python";
	
	String LIB_PATHS = "userLibraries.paths";
	
    String OUTPUT_PATH = "compiler.outputPath";
    String BASIC_LOADER = "compiler.basicLoader";
    String AUTORUN = "compiler.autorun";
    String HEAP_SIZE = "compiler.heapSize";
    String HEAP_ADDRESS = "compiler.heapAddress";
    String BREAK_DETECTION = "compiler.breakDetection";
    String OPTIMIZATION_LEVEL = "compiler.optimizationLevel";
	String SDK = "compiler.sdk";
	String STRICT = "compliance.strict";
	String STRICT_BOOLEAN = "compliance.strictBooleans";
	String IGNORE_VARIABLE_CASE = "compliance.ignoreVariableCase";
	String EXPLICIT_DECLARATION = "compliance.explicitDeclaration";
	String LEGACY_INSTRUCTIONS = "compliance.legacyInstructions";
	String ARRAY_BASE = "compliance.arrayBase";
	String STRING_BASE = "compliance.stringBase";

	String DEBUG_ARRAYS = "debug.arrays";
	String DEBUG_MEMORY = "debug.memory";
	String DEBUG_LEVEL = "debug.level";

    String NEX_CORE = "nex.core";
    String NEX_INCLUDE_SYSVAR = "nex.includeSysVar";
    String NEX_SYSVAR_LOCATION = "nex.sysVarLocation";
    
    
//    @Deprecated
//	String EXTERNAL_EMULATOR_HOME = "externalEmulator.home";
//    @Deprecated
//	String EXTERNAL_EMULATOR_EXECUTABLE = "externalEmulator.executable";
//    @Deprecated
//	String EXTERNAL_EMULATOR_LEADING_OPTIONS = "externalEmulator.leadingOptions";
//    @Deprecated
//	String EXTERNAL_EMULATOR_TRAILING_OPTIONS = "externalEmulator.trailingOptions";


}
