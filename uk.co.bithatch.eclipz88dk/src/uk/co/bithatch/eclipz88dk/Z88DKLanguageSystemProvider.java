package uk.co.bithatch.eclipz88dk;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import uk.co.bithatch.bitzx.AbstractDescribable;
import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.ILanguageSystemProvider;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.WellKnownArchitecture;
import uk.co.bithatch.eclipz88dk.preferences.PreferenceConstants;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKConfigurationFile;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

public class Z88DKLanguageSystemProvider implements ILanguageSystemProvider {
	
	private final static class Z88DKOutputFormat extends AbstractDescribable implements IOutputFormat {

		private Z88DKOutputFormat(String name) {
			super(name);
		}
	}
	
	private final static class Z88DKArchitecture extends AbstractDescribable implements IArchitecture {

		private Z88DKConfigurationFile cfg;

		private Z88DKArchitecture(Z88DKConfigurationFile cfg) {
			super(cfg.name().toUpperCase());
			this.cfg = cfg;
		}

		@Override
		public List<? extends IOutputFormat> supportedFormats() {
			return cfg.subtypes().stream().map(stype -> new Z88DKOutputFormat(stype.toUpperCase())).toList();
		}

		@Override
		public Optional<WellKnownArchitecture> wellKnown() {
			if(name().equals("zxn")) {
				return Optional.of(WellKnownArchitecture.ZXNEXT);
			}
			else if(name().equals("zx")) {
				return Optional.of(WellKnownArchitecture.LEGACY);
			}
			else {
				return IArchitecture.super.wellKnown();
			}
		}
		
	}


	private static final String[] EXTENSIONS = new String[] { "c", "asm" };

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
				var sdk = sdkOr.get();
				return sdk.configurations().configurations().stream().map(cfg -> {
					return new Z88DKArchitecture(cfg);
				}).toList();
			}
		}
		return Collections.emptyList();
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
//					.equals(PreferencesAccess.get().getOutputFolder(res.getProject()).getFullPath());
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
}
