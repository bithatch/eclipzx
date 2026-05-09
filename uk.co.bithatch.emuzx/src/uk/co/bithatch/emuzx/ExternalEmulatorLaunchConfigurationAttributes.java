package uk.co.bithatch.emuzx;
public interface ExternalEmulatorLaunchConfigurationAttributes {

    public static final String ID = "uk.co.bithatch.zxbasic.ui.launch.externalEmulatorLaunch";

    public static final String EMULATOR_EXECUTABLE = ID + ".emulator";
    public static final String CONFIGURATION_FILE = ID + ".configurationFile";
    public static final String CONFIGURATION_CONTENT = ID + ".configurationContent";
    public static final String EMULATOR_ARGS = ID + ".emulatorArguments";
    public static final String PROGRAM = ID + ".program";
    public static final String OUTPUT_FORMAT = ID + ".outputFormat";
    public static final String PROJECT = ID + ".project";
    public static final String CUSTOM_WORKING_DIRECTORY = ID + ".customWorkingDirectory";
    public static final String WORKING_DIRECTORY_LOCATION = ID + ".workingDirectoryLocation";
    public static final String PREPARATION = ID + ".preparation";
    public static final String PREPARATION_OTHER_FILES = PREPARATION + ".otherFiles";
    public static final String PREPARATION_TARGET = PREPARATION + ".target";
    public static final String PREPARATION_TARGET_LOCATION = PREPARATION + ".targetLocation";
    public static final String PREPARATION_CLEAR_BEFORE_USE = PREPARATION + ".clearBeforeUse";
    public static final String AUTOCONFIGURED = PREPARATION + ".autoConfigured";
    public static final String PREPARATION_SOURCE_IDS = PREPARATION + ".sourceIds";
    
}