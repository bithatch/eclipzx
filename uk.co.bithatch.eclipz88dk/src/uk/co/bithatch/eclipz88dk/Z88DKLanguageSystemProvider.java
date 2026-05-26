package uk.co.bithatch.eclipz88dk;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.bitzx.AbstractDescribable;
import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.LanguageSystemPreferencesAccess;
import uk.co.bithatch.bitzx.WellKnownArchitecture;
import uk.co.bithatch.bitzx.WellKnownOutputFormat;
import uk.co.bithatch.eclipz88dk.preferences.PreferenceConstants;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKConfigurationFile;

public class Z88DKLanguageSystemProvider implements ILanguageSystemProvider {
	
	private final static class Z88DKOutputFormat extends AbstractDescribable implements IOutputFormat {

		private String extension;

		private Z88DKOutputFormat(String name, String extension) {
			super(name);
			this.extension = extension;
		}

		@Override
		public String extension() {
			return extension;
		}

		@Override
		public Optional<WellKnownOutputFormat> wellKnown() {
			// TODO Auto-generated method stub
			return IOutputFormat.super.wellKnown();
		}
	}
	
	private final static Properties extensionMap = new Properties();
	
	static {
		try(var in = Z88DKLanguageSystemProvider.class.getResourceAsStream("/output-format-extensions.properties")) {
			extensionMap.load(in);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load output format extensions properties", e);
		}
	}
	
	public final static class Z88DKArchitecture extends AbstractDescribable implements IArchitecture {

		private Z88DKConfigurationFile cfg;

		private Z88DKArchitecture(Z88DKConfigurationFile cfg) {
			super(cfg.name().toUpperCase());
			this.cfg = cfg;
		}
		
		public Z88DKConfigurationFile configuration() {
			return cfg;
		}

		@Override
		public List<? extends IOutputFormat> supportedFormats() {
			return cfg.subtypes().
					stream().
					map(stype -> new Z88DKOutputFormat(stype.toUpperCase(), extensionMap.getProperty(name().toLowerCase() + "." + stype, stype.toLowerCase()))).
					toList();
		}

		@Override
		public Optional<WellKnownArchitecture> wellKnown() {
			if(name().equalsIgnoreCase("ZXN")) {
				return Optional.of(WellKnownArchitecture.ZXNEXT);
			}
			else if(name().equalsIgnoreCase("ZN")) {
				return Optional.of(WellKnownArchitecture.ZX);
			}
			else {
				return IArchitecture.super.wellKnown();
			}
		}
		
	}


	private static final String[] EXTENSIONS = new String[] { "c", "asm" };
	private static final String[] SOURCE_FILE_EXTENSIONS = new String[] { "c", "asm", "h" };

	@Override
	public List<? extends IArchitecture> architectures(IResource resource) {
		if(resource == null || hasCNature(resource.getProject())) {

			var pax = Z88DKPreferencesAccess.get();
			var sdks = pax.getPathListPreference(null, PreferenceConstants.SDK_PATHS);
			if (sdks.isEmpty())
				throw new IllegalStateException("No Z88DK home!");

			var project = resource == null ? null : resource.getProject();
			var sdkOr = pax.getSDK(project);
			if (sdkOr.isPresent()) {
				return architectures(project, sdkOr.get().name());
			}
		}
		return Collections.emptyList();
	}

	@Override
	public LanguageSystemPreferencesAccess preferenceAccess() {
		return Z88DKPreferencesAccess.get();
	}

	@Override
	public List<? extends IArchitecture> architectures(IProject project, String sdkName) {
		var pax = Z88DKPreferencesAccess.get();
		var sdk = pax.getSDKByName(sdkName).orElseThrow(() -> new IllegalArgumentException("No such SDK as " + sdkName));
		var allArchs = pax.isAllArchitectures(project);
		var configs = sdk.configurations();
		var configItems = configs.configurations();
		return configItems.stream().map(cfg -> {
			return new Z88DKArchitecture(cfg);
		}).filter(a -> allArchs || a.wellKnown().isPresent()).toList();
	}

	@Override
	public boolean isCompatible(IResource resource) {
		return hasCNature(resource.getProject());
	}

	@Override
	public boolean isLaunchable(IResource res) {
		if (res instanceof IFile file) {
			return FileNames.hasExtensions(file.getName(), EXTENSIONS);
		} else if (res instanceof IContainer container) {
			return false;
//			return !container.getFullPath()
//					.equals(EmuZXPreferencesAccess.get().getOutputFolder(res.getProject()).getFullPath());
		} else {
			return false;
		}
	}

	
	public static boolean hasCNature(IProject p) {
		try {
			return p.hasNature(CProjectNature.C_NATURE_ID);
		} catch (CoreException e) {
			//throws exception if the project is not open.
		}
		return false;
	}

	@Override
	public File prepareForLaunch(IOutputFormat fmt, IFile file, ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		return file.getLocation().toFile();
	}

	@Override
	public String[] sourceFileExtensions() {
		return SOURCE_FILE_EXTENSIONS;
	}
}
