package uk.co.bithatch.eclipz88dk.toolchain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.IBinaryParser;
import org.eclipse.cdt.core.IBinaryParser.IBinaryFile;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICOutputEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.executables.Executable;
import org.eclipse.cdt.debug.core.executables.IProjectExecutablesProvider;
import org.eclipse.cdt.debug.core.executables.ISourceFileRemapping;
import org.eclipse.cdt.internal.core.model.BinaryParserConfig;
import org.eclipse.cdt.internal.core.model.CModelManager;
import org.eclipse.cdt.internal.core.settings.model.CProjectDescriptionManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;

/**
 * @since 7.0
 */
public class Z88DKExecutableProvider implements IProjectExecutablesProvider {

	private List<String> supportedNatureIds = new ArrayList<>();
	@SuppressWarnings("restriction")
	private BinaryParserConfig[] binConfigs;

	public Z88DKExecutableProvider() {
		/* By matching all 3 natures we take precedence over standard CDT executable provider */
		supportedNatureIds.add(Z88DKNature.NATURE_ID);
		supportedNatureIds.add(CProjectNature.C_NATURE_ID);
		supportedNatureIds.add(CCProjectNature.CC_NATURE_ID);
	}

	@Override
	public List<String> getProjectNatures() {
		return supportedNatureIds;
	}

	@SuppressWarnings("restriction")
	@Override
	public List<Executable> getExecutables(IProject project, IProgressMonitor monitor) {
		List<Executable> executables = new ArrayList<>();

		if (binConfigs == null) {
			binConfigs = CModelManager.getDefault().getBinaryParser(project);
		}

		Map<IFile, IBinaryFile> binaries = new HashMap<>();
		ICProjectDescription projDesc = CProjectDescriptionManager.getInstance().getProjectDescription(project, false);
		IPath rootLoc = project.getWorkspace().getRoot().getLocation();
		if (projDesc != null) {
			ICConfigurationDescription cfg = projDesc.getActiveConfiguration();
			if (cfg != null) {
				ICOutputEntry[] cfgOutDirs = cfg.getBuildSetting().getOutputDirectories();
				for (ICOutputEntry outdir : cfgOutDirs) {
					IResource outdirRes = project.getWorkspace().getRoot()
							.getFolder(outdir.getLocation().makeRelativeTo(rootLoc));
					if (outdirRes instanceof IFolder fldr) {
						try {
							fldr.accept(chld -> {

								for (BinaryParserConfig binConfig : binConfigs) {
									IBinaryFile bin;
									try {
										IBinaryParser parser = binConfig.getBinaryParser();
										bin = parser.getBinary(chld.getFullPath());
										if (bin != null) {
											binaries.put((IFile)chld, bin);
										}
									} catch (IOException | CoreException e) {
									}
								}

								return true;
							});
						} catch (CoreException ce) {
							ce.printStackTrace();
						}
					}
				}
			}
		}


		SubMonitor progress = SubMonitor.convert(monitor, binaries.size());
		
		binaries.forEach((file, binary) -> {
			if (!progress.isCanceled()) {
//				if (binary.isExecutable() || binary.isSharedLib()) {
//					IPath exePath = binary.getResource().getLocation();
//					if (exePath == null)
//						exePath = binary.getPath();
					List<ISourceFileRemapping> srcRemappers = new ArrayList<>(2);
	//				ISourceFileRemappingFactory[] factories = ExecutablesManager.getExecutablesManager()
	//						.getSourceFileRemappingFactories();
	//				for (ISourceFileRemappingFactory factory : factories) {
	//					ISourceFileRemapping remapper = factory.createRemapper(binary);
	//					if (remapper != null) {
	//						srcRemappers.add(remapper);
	//					}
	//				}
					executables.add(new Executable(file.getLocation().makeRelativeTo(project.getLocation()), project, file,
							srcRemappers.toArray(new ISourceFileRemapping[srcRemappers.size()])));
				}
	
				progress.worked(1);
//			}
		});

		return executables;
	}

	@Override
	public IStatus removeExecutable(Executable executable, IProgressMonitor monitor) {
		IResource exeResource = executable.getResource();
		if (exeResource != null) {
			try {
				exeResource.delete(true, monitor);
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
			return Status.OK_STATUS;
		}
		return new Status(IStatus.WARNING, CDebugCorePlugin.PLUGIN_ID, "Can't remove " + executable.getName() //$NON-NLS-1$
				+ ": it is built by project \"" + executable.getProject().getName() + "\""); //$NON-NLS-1$//$NON-NLS-2$
	}
}
