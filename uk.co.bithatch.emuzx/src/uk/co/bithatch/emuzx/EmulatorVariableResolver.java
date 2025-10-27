package uk.co.bithatch.emuzx;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

import uk.co.bithatch.bitzx.LaunchContext;

public class EmulatorVariableResolver implements IDynamicVariableResolver {
	
	public final static String EMULATOR_CONFIG_FILE = "emulator_config_file";
	
	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		if(variable.getName().equals(EMULATOR_CONFIG_FILE)) {
			
			var lc = LaunchContext.get();
			if(lc != null) {

				var configFile = lc.config().getAttribute(ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_FILE, "");
				if(!configFile.equals("")) {
					return configFile;
				}
				
				var content = lc.config().getAttribute(ExternalEmulatorLaunchConfigurationAttributes.CONFIGURATION_CONTENT, "");
				var cfgfile = (Path)lc.attr(EMULATOR_CONFIG_FILE);
				if(cfgfile == null) {
					cfgfile = lc.tempFile(".cfg");
					try(var out = Files.newOutputStream(cfgfile)) {
						out.write(content.getBytes());
					}
					catch(IOException ioe) {
						throw new UncheckedIOException(ioe);
					}
					lc.attr(EMULATOR_CONFIG_FILE, cfgfile);
				}
				return cfgfile.toAbsolutePath().toString();
				
			}
		}
		return "";
	}

}
