package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.File;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILog;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;

/**
 * Supplies the {@code PATH} environment variable with the Z88DK {@code bin/}
 * directory prepended. This ensures that when {@code zcc} invokes sub-tools
 * like {@code z88dk-z80asm}, they can be found — especially important on
 * Windows where the SDK bin directory may not be on the system PATH.
 */
public class Z88DKEnvironmentVariableSupplier implements IConfigurationEnvironmentVariableSupplier {

	private static final ILog LOG = ILog.of(Z88DKEnvironmentVariableSupplier.class);

	private static final String PATH_VAR = "PATH";
	private static final String ZCCCFG_VAR = "ZCCCFG";

	@Override
	public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
			IEnvironmentVariableProvider provider) {
		if (PATH_VAR.equals(variableName)) {
			String binDir = getBinDir(configuration);
			if (binDir != null) {
				return new Z88DKEnvVar(PATH_VAR, binDir, IBuildEnvironmentVariable.ENVVAR_PREPEND,
						File.pathSeparator);
			}
		}
		if (ZCCCFG_VAR.equals(variableName)) {
			String cfgDir = getCfgDir(configuration);
			if (cfgDir != null) {
				return new Z88DKEnvVar(ZCCCFG_VAR, cfgDir, IBuildEnvironmentVariable.ENVVAR_REPLACE, null);
			}
		}
		return null;
	}

	@Override
	public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration,
			IEnvironmentVariableProvider provider) {
		String binDir = getBinDir(configuration);
		String cfgDir = getCfgDir(configuration);
		if (binDir != null && cfgDir != null) {
			return new IBuildEnvironmentVariable[] {
				new Z88DKEnvVar(PATH_VAR, binDir, IBuildEnvironmentVariable.ENVVAR_PREPEND, File.pathSeparator),
				new Z88DKEnvVar(ZCCCFG_VAR, cfgDir, IBuildEnvironmentVariable.ENVVAR_REPLACE, null)
			};
		}
		if (binDir != null) {
			return new IBuildEnvironmentVariable[] {
				new Z88DKEnvVar(PATH_VAR, binDir, IBuildEnvironmentVariable.ENVVAR_PREPEND, File.pathSeparator)
			};
		}
		if (cfgDir != null) {
			return new IBuildEnvironmentVariable[] {
				new Z88DKEnvVar(ZCCCFG_VAR, cfgDir, IBuildEnvironmentVariable.ENVVAR_REPLACE, null)
			};
		}
		return new IBuildEnvironmentVariable[0];
	}

	private String getBinDir(IConfiguration configuration) {
		try {
			IProject project = getProject(configuration);
			if (project == null) return null;
			var sdk = Z88DKPreferencesAccess.get().getSDK(project);
			if (sdk.isPresent()) {
				File binDir = new File(sdk.get().location(), "bin");
				if (binDir.isDirectory()) {
					return binDir.getAbsolutePath();
				}
			}
		} catch (Exception e) {
			LOG.warn("Z88DK: failed to determine bin directory for PATH", e);
		}
		return null;
	}

	private String getCfgDir(IConfiguration configuration) {
		try {
			IProject project = getProject(configuration);
			if (project == null) return null;
			var sdk = Z88DKPreferencesAccess.get().getSDK(project);
			if (sdk.isPresent()) {
				File cfgDir = new File(new File(sdk.get().location(), "lib"), "config");
				if (cfgDir.isDirectory()) {
					return cfgDir.getAbsolutePath();
				}
			}
		} catch (Exception e) {
			LOG.warn("Z88DK: failed to determine ZCCCFG directory", e);
		}
		return null;
	}

	private static IProject getProject(IConfiguration configuration) {
		if (configuration == null) return null;
		IManagedProject mp = configuration.getManagedProject();
		if (mp == null) return null;
		return (IProject) mp.getOwner();
	}

	/**
	 * Simple implementation of {@link IBuildEnvironmentVariable}.
	 */
	private static class Z88DKEnvVar implements IBuildEnvironmentVariable {

		private final String name;
		private final String value;
		private final int operation;
		private final String delimiter;

		Z88DKEnvVar(String name, String value, int operation, String delimiter) {
			this.name = name;
			this.value = value;
			this.operation = operation;
			this.delimiter = delimiter;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public int getOperation() {
			return operation;
		}

		@Override
		public String getDelimiter() {
			return delimiter;
		}
	}
}
