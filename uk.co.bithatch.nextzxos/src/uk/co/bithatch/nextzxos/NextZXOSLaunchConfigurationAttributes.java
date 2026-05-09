package uk.co.bithatch.nextzxos;

import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;

public interface NextZXOSLaunchConfigurationAttributes extends ExternalEmulatorLaunchConfigurationAttributes {

    public static final String PREPARATION_RESET_IMAGE_STATE = PREPARATION + ".resetImageState";
    public static final String PREPARATION_BASE_ON_NEXT_ZXOS = PREPARATION + ".baseOnNextZXOS";
    
}