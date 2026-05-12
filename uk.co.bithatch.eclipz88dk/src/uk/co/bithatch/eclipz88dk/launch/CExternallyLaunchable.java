package uk.co.bithatch.eclipz88dk.launch;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.emuzx.DefaultPreparationContext;
import uk.co.bithatch.emuzx.api.IExternallyLaunchable;
import uk.co.bithatch.emuzx.api.IProgramBuildOptionsFactory;
import uk.co.bithatch.emuzx.debug.DezogDebugTarget;
import uk.co.bithatch.emuzx.ui.ExternalEmulatorDebugTarget;

public class CExternallyLaunchable implements IExternallyLaunchable {
	
	public final static ILog LOG = ILog.of(CExternallyLaunchable.class);

	@Override
	public IDebugTarget createRemoteDebugTarget(ILaunchConfiguration configuration, ILaunch launch,
			DefaultPreparationContext prepCtx, IProcess eclipseProcess) throws CoreException {
		return new DezogDebugTarget(launch, configuration, eclipseProcess);
	}

	@Override
	public void compileForLaunch(String mode, DefaultPreparationContext prepCtx) throws CoreException {
		var file = prepCtx.programFile();
		var project = file.getProject();
		var sourceFile = file.getLocation().toFile();	
		var fullProjectDir = project.getLocation().toFile().getAbsoluteFile();
		var fmt = prepCtx.outputFormat();
		
		if (FileNames.hasExtensions(sourceFile, "c", "asm")) {
			if (project != null) {
//				var outputDir = Z88DKPreferencesAccess.get().getOutputFolder(project).getLocation().toFile();
				var relFile = fullProjectDir.toPath().relativize(sourceFile.toPath()).toFile();
				
				prepCtx.buildOptions(IProgramBuildOptionsFactory.accumulate(file));
				
				// XXXXX TEMP XXXXX
				var outputDir = project.getFullPath().toFile();
				var outputFile = FileNames.changeExtension(new File(outputDir, relFile.getPath()),
						fmt.name().toLowerCase());
				prepCtx.binaryFile(outputFile);
				
//				var zxbc = builderForProject(project)
//						.withWorkingdir(fullProjectDir)
//						.withOutputFormat(fmt.firstPass())
//						.withAutorun(fmt.snapshot())
//						.withBasicLoader(fmt.snapshot())
//						.withErrorHandler(errHandler)
//						.withMemoryMap(false).build();
//				
//				File firstPassOutput = null;
//
//				if (zxbc.isNeedsProcessing(sourceFile)) {
//					try {
//						firstPassOutput = zxbc.compile(sourceFile);
//					} catch (IOException ioe) {
//						throw new UncheckedIOException(ioe);
//					}
//				}
//				else {
//					firstPassOutput = zxbc.targetFile(sourceFile);
//				}
//				
//				prepCtx.binaryFile(firstPassOutput);
//				
//				var outputFile = FileNames.changeExtension(new File(outputDir, relFile.getPath()),
//						fmt.name().toLowerCase());
				
//				if(fmt.requiresSecondPass()) {
//					switch(fmt) {
//					case BorielZXBasicOutputFormat.NEX:
//						var org = prepCtx.buildOptions().orgOrDefault();
//						var cfg = new NexConverter.NexConfiguration();
//						var pc = getPc(file, org); 
//						var sp = getSp(file, org - 2);//
//						
//						/* Basic Program and project configuration */
//						cfg.core(getCore(file));
//						getSysVars(file).ifPresent(sv -> {
//							cfg.mmu(sv, 10, 0x1c00);
//						});
//						cfg.pcsp(pc, Optional.of(sp));
//						
//						/* Add all other the contributions */
//						INEXConfigurer.accumulate(file, cfg);
//						
//						/* Finally the actual binary */
//						LOG.info(String.format("Adding output %s to NEX", firstPassOutput));
//						cfg.addFile(firstPassOutput.toPath(), Optional.of(prepCtx.buildOptions().bankForOrg()), Optional.of(prepCtx.buildOptions().codestart()));
//						
//						var gen = new NexConverter.NexGenerator(cfg);
//						gen.reporting(msg -> {
//							errHandler.accept(new ToolMessage(file.toString(), 0, ToolMessageLevel.INFO, msg));
//						});
//						gen.generate(outputFile.toPath());
//						break;
//					default:
//						throw new UnsupportedOperationException("Output format declared it needs a second pass, but I don't know how!");
//					}
//				}

//				sourceFile = outputFile;
//				prepCtx.binaryFile(outputFile);
			}
		}
	}

	@Override
	public IOutputFormat getOutputFormat(IProject prj) {
		return Z88DKPreferencesAccess.get().getOutputFormat(prj);
	}

	@Override
	public IDebugTarget createDefaultDebugTarget(ILaunch launch, DefaultPreparationContext prepCtx,
			IProcess eclipseProcess) {
		return new ExternalEmulatorDebugTarget(launch, eclipseProcess);
	}

	@Override
	public IArchitecture getArchitecture(IProject proj) {
		return Z88DKPreferencesAccess.get().getArchitecture(proj);
	}

	

}