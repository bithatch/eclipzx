package uk.co.bithatch.zxbasic.ui.builder;

import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_CORE;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_OVERRIDE_PROJECT;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_PC;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_SP;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_SYSVARS;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.NEX_SYSVARS_LOCATION;
import static uk.co.bithatch.zxbasic.ui.builder.ResourceProperties.getProperty;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.zxbasic.tools.NexConverter;
import uk.co.bithatch.zxbasic.ui.api.INEXConfigurer;
import uk.co.bithatch.zxbasic.ui.api.IProgramBuildOptionsFactory;
import uk.co.bithatch.zxbasic.ui.api.IWritablePreparationContext;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicOutputFormat;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.tools.ToolMessage;
import uk.co.bithatch.zxbasic.ui.tools.ToolMessageLevel;
import uk.co.bithatch.zxbasic.ui.tools.ZXBC;
import uk.co.bithatch.zxbasic.ui.tools.ZXBC.Builder;
import uk.co.bithatch.zxbasic.ui.tools.ZXBC.Warning;

public class ZXBasicBuilder extends IncrementalProjectBuilder {
	
	public enum BuildResult {
		COUNT, SKIP, CONTINUE
	}
	
	public final static ILog LOG = ILog.of(ZXBasicBuilder.class);

	public final static Consumer<ToolMessage> DEFAULT_REPORTER = err -> {
		switch(err.getLevel()) {
		case ERROR:
			LOG.error(err.getMessage());
			break;
		case WARNING:
			LOG.warn(err.getMessage());
			break;
		case INFO:
			LOG.info(err.getMessage());
			break;
		}
	};
	

	public static final String BUILDER_ID = "uk.co.bithatch.zxbasic.ZXBasicBuilder";
	public static final String[] EXTENSIONS = new String[] { "bas", "asm" };


	public static void compileForLaunch(IWritablePreparationContext prepCtx, String mode, Consumer<ToolMessage> errHandler) throws CoreException {
		
		var file = prepCtx.programFile();
		var project = file.getProject();
		var sourceFile = file.getLocation().toFile();	
		var fullProjectDir = project.getLocation().toFile().getAbsoluteFile();
		var fmt = (BorielZXBasicOutputFormat)prepCtx.outputFormat();
		
		if (FileNames.hasExtensions(sourceFile, ZXBasicBuilder.EXTENSIONS)) {
			if (project != null) {
				var outputDir = ZXBasicPreferencesAccess.get().getOutputFolder(project).getLocation().toFile();
				var relFile = fullProjectDir.toPath().relativize(sourceFile.toPath()).toFile();
				
				prepCtx.buildOptions(IProgramBuildOptionsFactory.accumulate(file));
				
				var zxbc = builderForProject(project)
						.withWorkingdir(fullProjectDir)
						.withOutputFormat(fmt.firstPass())
						.withAutorun(fmt.snapshot())
						.withBasicLoader(fmt.snapshot())
						.withErrorHandler(errHandler)
						.withMemoryMap(false).build();
				
				File firstPassOutput = null;

				if (zxbc.isNeedsProcessing(sourceFile)) {
					try {
						firstPassOutput = zxbc.compile(sourceFile);
					} catch (IOException ioe) {
						throw new UncheckedIOException(ioe);
					}
				}
				else {
					firstPassOutput = zxbc.targetFile(sourceFile);
				}
				
				prepCtx.binaryFile(firstPassOutput);
				
				var outputFile = FileNames.changeExtension(new File(outputDir, relFile.getPath()),
						fmt.name().toLowerCase());
				
				if(fmt.requiresSecondPass()) {
					switch(fmt) {
					case BorielZXBasicOutputFormat.NEX:
						var org = prepCtx.buildOptions().orgOrDefault();
						var cfg = new NexConverter.NexConfiguration();
						var pc = getPc(file, org); 
						var sp = getSp(file, org - 2);//
						
						/* Basic Program and project configuration */
						cfg.core(getCore(file));
						getSysVars(file).ifPresent(sv -> {
							cfg.mmu(sv, 10, 0x1c00);
						});
						cfg.pcsp(pc, Optional.of(sp));
						
						/* Add all other the contributions */
						INEXConfigurer.accumulate(file, cfg);
						
						/* Finally the actual binary */
						LOG.info(String.format("Adding output %s to NEX", firstPassOutput));
						cfg.addFile(firstPassOutput.toPath(), Optional.of(prepCtx.buildOptions().bankForOrg()), Optional.of(prepCtx.buildOptions().codestart()));
						
						var gen = new NexConverter.NexGenerator(cfg);
						gen.reporting(msg -> {
							errHandler.accept(new ToolMessage(file.toString(), 0, ToolMessageLevel.INFO, msg));
						});
						gen.generate(outputFile.toPath());
						break;
					default:
						throw new UnsupportedOperationException("Output format declared it needs a second pass, but I don't know how!");
					}
				}

				sourceFile = outputFile;
				prepCtx.binaryFile(outputFile);
			}
		}
	}

	private final static int getPc(IFile file, int defaultPc) {
		var val = getProperty(file, NEX_PC, -1);
		return val == -1 ? defaultPc : val;
	}

	private final static int getSp(IFile file, int defaultSp) {
		var val = getProperty(file, NEX_SP, -1);
		return val == -1 ? defaultSp: val;
	}
	
	private final static String getCore(IFile file) {
		String val = "";
		if(getProperty(file, NEX_OVERRIDE_PROJECT, false)) {
			val = getProperty(file, NEX_CORE, "");
		}
		if(val.equals("")) {
			val = ZXBasicPreferencesAccess.get().getNEXCore(file.getProject());
		}
		return val;
	}
	
	private final static Optional<Path> getSysVars(IFile file) {
		boolean includeSysVars;
		String sysvarLocation;
		if(getProperty(file, NEX_OVERRIDE_PROJECT, false)) {
			includeSysVars = getProperty(file, NEX_SYSVARS, true);
			sysvarLocation = getProperty(file, NEX_SYSVARS_LOCATION, "");
		}
		else {
			includeSysVars = ZXBasicPreferencesAccess.get().isNEXIncludeSysvar(file.getProject());
			sysvarLocation = ZXBasicPreferencesAccess.get().getNEXSysvarLocation(file.getProject());
		}
		if(includeSysVars) {
			if(sysvarLocation.equals("")) {
				var sysvarsFile = ZXBasicPreferencesAccess.get().getOutputFolder(file.getProject()).getLocation().toPath().resolve("sysvars.bin");
				if(!Files.exists(sysvarsFile)) {
					try(var in = ZXBasicBuilder.class.getResourceAsStream("/META-INF/sysvars.bin")) {
						try(var out = Files.newOutputStream(sysvarsFile)) {
							in.transferTo(out);
						}
					}
					catch(IOException ioe) {
						throw new UncheckedIOException(ioe);
					}
				}
				return Optional.of(sysvarsFile);
			}
			else {
				return Optional.of(Paths.get(sysvarLocation));
			}
		}
		else {
			return Optional.empty();
		}
	}

	public static Builder builderForProject(IProject project) {
		var pax = ZXBasicPreferencesAccess.get();
		var bldr = new ZXBC.Builder(pax.getSDK(project).location().toPath()).
				withWorkingdir(project.getLocation().toFile()).
				withOutputFormat(((BorielZXBasicOutputFormat)pax.getOutputFormat(project)).firstPass()).
				withOutdir(pax.getOutputFolder(project).getLocation().toFile()).
				withOptimizationLevel(pax.getOptimizationLevel(project)).
				withVerbosity(pax.getDebugLevel(project)).
				withDebugArrays(pax.isDebugArrays(project)).
				withDebugMemory(pax.isDebugMemory(project)).
				withStrict(pax.isStrict(project)).
				withBreakDetection(pax.isBreakDetection(project)).
				withBasicLoader(pax.isBasicLoader(project)).
				withStrictBoolean(pax.isStrictBoolean(project)).
				withExplicitDeclaration(pax.isExplicitDeclaration(project)).
				withStringBase(pax.getStringBase(project)).
				withArrayBase(pax.getArrayBase(project)).
				withLegacyInstructions(pax.isLegacyInstructions(project)).
				withIgnoreVariableCase(pax.isIgnoreVariableCase(project)).
				withIncludeDirs(pax.getAllLibs(project)).
				withSuppressedWarnings(pax.getSuppressedWarnings(project)).
				withDefines(pax.getDefines(project));
		
		var heapSize = pax.getHeapSize(project);
		if(heapSize > 0)
			bldr.withHeapSize(heapSize);

		var heapAddress = pax.getHeapAddress(project);
		if(heapAddress > 0)
			bldr.withHeapAddress(heapAddress);
		
		return bldr;
	}

	public ZXBasicBuilder() {
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		var delta = getDelta(getProject());
		if (delta == null) {
			fullBuild(monitor);
		} else {
			incrementalBuild(delta, monitor);
		}
		return null;
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		var bin = ZXBasicPreferencesAccess.get().getOutputFolder(getProject());
		if (bin.exists()) {
			bin.delete(true, monitor);
		}
	}

	private void fullBuild(IProgressMonitor monitor) throws CoreException {
	    var totalFiles = countBuildableFiles(getProject());
	    var sub = SubMonitor.convert(monitor, "Building ZX Basic project", totalFiles);
	    getProject().accept(new ZXBasicResourceVisitor(sub));
	    monitor.done();
	}


	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
	    var totalFiles = countChangedFilesInDelta(delta);
	    var sub = SubMonitor.convert(monitor, "Building ZX Basic project (incremental)", totalFiles);
	    delta.accept(new ZXBasicDeltaVisitor(sub));
	    monitor.done();
	}

	
	private int countBuildableFiles(IResource resource) throws CoreException {
	    var count = new AtomicInteger();
	    resource.accept(res -> {
	        if (isCompilable(res) && !isInOutputFolder((IFile)res)) {
                count.incrementAndGet();
	        }
	        return true;
	    });
	    return count.get();
	}
	
	private int countChangedFilesInDelta(IResourceDelta delta) throws CoreException {
	    var count = new AtomicInteger();

	    delta.accept(d -> {
	        IResource res = d.getResource();
	        if (res instanceof IFile file &&
	            (d.getKind() == IResourceDelta.ADDED || d.getKind() == IResourceDelta.CHANGED) &&
	            isCompilable(res) &&
	            !isInOutputFolder(file)) {
                count.incrementAndGet();
	        }
	        return true;
	    });

	    return count.get();
	}


	private static boolean isCompilable(IResource res) {
        return res instanceof IFile file && FileNames.hasExtensions(file.getName(), ZXBasicBuilder.EXTENSIONS);
	}

	private static boolean isInOutputFolder(IFile file) {
		var outdir = ZXBasicPreferencesAccess.get().getOutputFolder(file.getProject());
		return outdir.getLocation().isPrefixOf(file.getLocation());
	}

	class ZXBasicResourceVisitor implements IResourceVisitor {

		private final SubMonitor monitor;

	    ZXBasicResourceVisitor(SubMonitor monitor) {
	        this.monitor = monitor;
	    }

		@Override
		public boolean visit(IResource resource) {
			var result = buildIfZXBasic(resource, monitor, true);
			if(result == BuildResult.COUNT) {
	            monitor.split(1).subTask("Compiled: " + resource.getName());
			}
			return result == BuildResult.CONTINUE;
		}
	}

	class ZXBasicDeltaVisitor implements IResourceDeltaVisitor {

		private final SubMonitor monitor;

		ZXBasicDeltaVisitor(SubMonitor monitor) {
			this.monitor = monitor;
		}

		@Override
		public boolean visit(IResourceDelta delta) {

			var result = buildIfZXBasic(delta.getResource(), monitor, true);
			if(result == BuildResult.COUNT) {
	            monitor.split(1).subTask("Compiled: " + delta.getResource().getName());
			}
			return result == BuildResult.CONTINUE;
		}
	}

	private BuildResult buildIfZXBasic(IResource resource, IProgressMonitor progress, boolean force) {
		if(progress.isCanceled()) {
			throw new IllegalStateException("Cancelled by user.");
		}
		
		/* Just ignore folders entirely, but continue looking */
		if(resource instanceof IFolder || resource instanceof IProject) {
			return BuildResult.CONTINUE;
		}
		
		if(isCompilable(resource)) {
			var file = (IFile)resource;
			var project = getProject();
			
			/* If the file is in the output folder, or one of the library folders, ignore it */
			var outdirFile = ZXBasicPreferencesAccess.get().getOutputFolder(file.getProject());
			if(isInOutputFolder(file)) {
				/* Skip the entire directory */
				return BuildResult.SKIP;
			}

			var allLibs = ZXBasicPreferencesAccess.get().getAllLibs(project);
			var isLibrary = allLibs.stream().
					filter(f -> file.getLocation().toFile().getParentFile().getAbsolutePath().startsWith(f.getAbsolutePath())).
					findFirst().isPresent();
			
			var thisErrors = new AtomicLong();
			var buildOpts = IProgramBuildOptionsFactory.accumulate(file);
			if(!buildOpts.build()) {
				/* Just build the next thing */
				return BuildResult.COUNT;
			}

			var zxbcBldr = builderForProject(project).
					withMemoryMap(true).
					withErrorHandler(err -> {
						
				try {
					if(err.getPath().equals(file.getLocation().toString())) {
						if (err.getLevel() == ToolMessageLevel.ERROR) {
							thisErrors.incrementAndGet();
						}
						IMarker marker = file.createMarker(IMarker.PROBLEM);
						marker.setAttribute(IMarker.SEVERITY, toMarkerSeverity(err.getLevel()));
						marker.setAttribute(IMarker.MESSAGE, err.getMessage());
						marker.setAttribute(IMarker.LINE_NUMBER, err.getLine());
					}
				} catch (CoreException ce) {
				}
				
			});
			
			if(isLibrary) {
				zxbcBldr.withSuppressedWarnings(Warning.UNUSED_FUNCTION, Warning.UNUSED_VARIABLE);
				zxbcBldr.withOutputFormat(BorielZXBasicOutputFormat.BIN);
			}
			
			var zxbc = zxbcBldr.build();
			

			try {
				var localFile = file.getLocation().toFile();
				if(force || zxbc.isNeedsProcessing(localFile)) {
					file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
					if (localFile.exists()) {
						var outputFile = zxbc.compile(localFile.getAbsoluteFile());
						
						if(isLibrary) {
							outputFile.delete();
						}
					}
				}
				
				return BuildResult.COUNT;

			} catch (Exception e) {
				e.printStackTrace();
				if (thisErrors.get() == 0) {
					try {
						IMarker marker = file.createMarker(IMarker.PROBLEM);
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						marker.setAttribute(IMarker.MESSAGE, e.getMessage());
						marker.setAttribute(IMarker.LINE_NUMBER, 1);
					} catch (CoreException ce) {
						ce.printStackTrace();
					}
				}
				return BuildResult.COUNT;
			} finally {
				try {
					outdirFile.setDerived(true, new NullProgressMonitor());
				} catch (CoreException e) {
				} finally {
					try {
						outdirFile.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					} catch (CoreException e) {
					} 
				}
			}
		}
		else
			return BuildResult.CONTINUE;

	}



	public int toMarkerSeverity(ToolMessageLevel tml) {
		switch(tml) {
		case ERROR:
			return IMarker.SEVERITY_ERROR;
		case WARNING:
			return IMarker.SEVERITY_WARNING;
		default:
			return IMarker.SEVERITY_INFO;
		}
	}
}
