package uk.co.bithatch.eclipz80.ui.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import uk.co.bithatch.bitzx.FileNames;
import uk.co.bithatch.bitzx.IOutputFormat;
import uk.co.bithatch.bitzx.TAPBuilder;
import uk.co.bithatch.bitzx.TZXBuilder;
import uk.co.bithatch.eclipz80.asm.AsmProgram;
import uk.co.bithatch.eclipz80.generator.Z80Assembler;
import uk.co.bithatch.eclipz80.ui.internal.Eclipz80Activator;
import uk.co.bithatch.eclipz80.ui.preferences.AsmPreferencesAccess;
import uk.co.bithatch.emuzx.ui.ResourceProperties;

public class AsmBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "uk.co.bithatch.eclipz80.ui.AsmBuilder";
	public static final String MARKER_TYPE = "uk.co.bithatch.eclipz80.ui.asmProblem";
	public static final String[] EXTENSIONS = new String[] { "asm" };

	public static final ILog LOG = ILog.of(AsmBuilder.class);

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();

		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(project);
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	private void fullBuild(IProgressMonitor monitor) throws CoreException {
		getProject().accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) throws CoreException {
				if (resource instanceof IFile file && isAsmFile(file)) {
					buildFile(file, monitor);
				}
				return true;
			}
		});
	}

	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		delta.accept(new IResourceDeltaVisitor() {
			@Override
			public boolean visit(IResourceDelta d) throws CoreException {
				IResource resource = d.getResource();
				if (resource instanceof IFile file && isAsmFile(file)) {
					switch (d.getKind()) {
					case IResourceDelta.ADDED:
					case IResourceDelta.CHANGED:
						buildFile(file, monitor);
						break;
					case IResourceDelta.REMOVED:
						removeOutput(file);
						break;
					}
				}
				return true;
			}
		});
	}

	private void buildFile(IFile file, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();
		var prefs = AsmPreferencesAccess.get();

		// Clear previous markers for this file
		file.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);

		// Only use built-in assembler
		if (!prefs.isBuiltinAssembler(project)) {
			throw new CoreException(Status.error("External assembler support not yet implemented."));
		}

		// Parse the .asm file via Xtext
		AsmProgram program;
		try {
			var resource = parseFile(file);

			// Report parse errors as markers
			if (resource.getErrors() != null && !resource.getErrors().isEmpty()) {
				for (Resource.Diagnostic diag : resource.getErrors()) {
					addMarker(file, diag.getMessage(), diag.getLine(), IMarker.SEVERITY_ERROR);
				}
				return;
			}

			program = (AsmProgram) resource.getContents().get(0);
		} catch (Exception e) {
			addMarker(file, "Failed to parse: " + e.getMessage(), 1, IMarker.SEVERITY_ERROR);
			LOG.error("Failed to parse " + file.getFullPath(), e);
			return;
		}

		// Configure the assembler with project defines
		var defines = prefs.getDefines(project);
		var assembler = Z80Assembler.builder()
				.withDefines(defines)
				.withIncludePaths(prefs.getAllIncludePaths(project))
				.withLibPaths(prefs.getAllIncludePaths(project))
				.withZ80N() /* TODO temporarily always enable this, need arch property on projects */
				.withOutputDir(prefs.getOutputFolder(project).getLocation().toPath())
				.withMap(prefs.isGenerateMap(project))
				.withWarningCallback((filename, line, warning) -> {
					try {
						addMarker(file, warning, line, IMarker.SEVERITY_WARNING);
					} catch (CoreException e) {
						LOG.error("Failed to create warning marker", e);
					}
				})
				.build();

		// Assemble
		var out = new ByteArrayOutputStream();
		try {
			assembler.assemble(file.getName(), program, out);
		} catch (Z80Assembler.AssemblyException e) {
			addMarker(file, e.getMessage(), e.getLine(), IMarker.SEVERITY_ERROR);
			return;
		} catch (Exception e) {
			addMarker(file, "Assembly failed: " + e.getMessage(), 1, IMarker.SEVERITY_ERROR);
			LOG.error("Assembly failed for " + file.getFullPath(), e);
			return;
		}

		// Write output
		IFolder outputFolder = prefs.getOutputFolder(project);
		ensureFolder(outputFolder, monitor);

		String baseName = file.getName();
		int dot = baseName.lastIndexOf('.');
		if (dot >= 0) baseName = baseName.substring(0, dot);
		IFile outputFile = outputFolder.getFile(baseName + ".bin");

		try (var bais = new ByteArrayInputStream(out.toByteArray())) {
			if (outputFile.exists()) {
				outputFile.setContents(bais, IResource.FORCE, monitor);
			} else {
				outputFile.create(bais, IResource.FORCE, monitor);
			}
		} catch (Exception e) {
			addMarker(file, "Failed to write output: " + e.getMessage(), 1, IMarker.SEVERITY_ERROR);
			LOG.error("Failed to write output for " + file.getFullPath(), e);
		}
	}

	public static Resource parseFile(IFile file) throws CoreException {
		var injector = Eclipz80Activator.getInstance()
				.getInjector(Eclipz80Activator.UK_CO_BITHATCH_ECLIPZ80_ASM);
		var resourceSet = injector.getInstance(ResourceSet.class);
		return resourceSet.getResource(
				URI.createPlatformResourceURI(file.getFullPath().toString(), true), true);
	}

	private void removeOutput(IFile file) throws CoreException {
		IProject project = getProject();
		var prefs = AsmPreferencesAccess.get();
		IFolder outputFolder = prefs.getOutputFolder(project);

		String baseName = file.getName();
		int dot = baseName.lastIndexOf('.');
		if (dot >= 0) baseName = baseName.substring(0, dot);
		IFile outputFile = outputFolder.getFile(baseName + ".bin");

		if (outputFile.exists()) {
			outputFile.delete(true, null);
		}
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();

		// Delete all assembly markers
		project.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);

		// Delete output folder contents
		var prefs = AsmPreferencesAccess.get();
		IFolder outputFolder = prefs.getOutputFolder(project);
		if (outputFolder.exists()) {
			for (IResource member : outputFolder.members()) {
				member.delete(true, monitor);
			}
		}
	}

	private boolean isAsmFile(IFile file) {
		String ext = file.getFileExtension();
		if (ext == null) return false;
		for (String e : EXTENSIONS) {
			if (e.equalsIgnoreCase(ext)) return true;
		}
		return false;
	}

	private void addMarker(IFile file, String message, int lineNumber, int severity) throws CoreException {
		IMarker marker = file.createMarker(MARKER_TYPE);
		marker.setAttribute(IMarker.MESSAGE, message);
		marker.setAttribute(IMarker.SEVERITY, severity);
		if (lineNumber >= 1) {
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		}
	}

	private void ensureFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
		if (!folder.exists()) {
			if (folder.getParent() instanceof IFolder parent) {
				ensureFolder(parent, monitor);
			}
			folder.create(true, true, monitor);
		}
	}
	
	public static Path prepareForEmulatorLaunch(IOutputFormat fmt, IFile file, Path binFile) throws CoreException {
		var wellKnownFormat = fmt.wellKnown().orElseThrow(() -> new IllegalArgumentException("Must be a well known output format."));
		var start = ResourceProperties.getProperty(file, ResourceProperties.ORG_ADDRESS, 32768);
		var clear = ResourceProperties.getProperty(file, ResourceProperties.ORG_ADDRESS, start - 1);
		var baseName = FileNames.stripExtension(file.getName());
		
		try {
		switch(wellKnownFormat) {
			case TZX:

				var tzxbldr = new TZXBuilder();
				tzxbldr.addBasicLoader("LD" + baseName, clear, start);
				tzxbldr.addCode(baseName, Files.readAllBytes(binFile), start);
				
				var tzxFile = FileNames.changeExtension(binFile, wellKnownFormat.extension());
				try(var out = Files.newOutputStream(tzxFile)) {
					tzxbldr.writeTo(out);
				} 
				
				return tzxFile;
				
			case TAP:
				var tapbldr = new TAPBuilder();
				tapbldr.addBasicLoader("LD" + baseName, clear, start);
				tapbldr.addCode(baseName, Files.readAllBytes(binFile), start);
				
				var tapFile = FileNames.changeExtension(binFile, wellKnownFormat.extension());
				try(var out = Files.newOutputStream(tapFile)) {
					tapbldr.writeTo(out);
				} 
				
				return tapFile;
			default:
				throw new CoreException(Status.error("Unsupported output format " + wellKnownFormat));
			
			}
		}
		catch (IOException e) {
			throw new CoreException(Status.error("Failed to generate " + wellKnownFormat, e));
		}
	}

	public static Path prepareForGenericLaunch(IFile file, String mode) throws CoreException {
		var project = file.getProject();
		var prefs = AsmPreferencesAccess.get();
		var outputFolder = prefs.getOutputFolder(project).getLocation().toPath();
		
		// TODO just BIN at the moment
		var sourceFileName = file.getLocation().toPath().getFileName();
		var mapFile =  outputFolder.resolve(FileNames.changeExtension(sourceFileName, "zmap"));
		var outputBin = outputFolder.resolve(FileNames.changeExtension(sourceFileName, "bin"));
		var needMap = mode.equals("debug") || prefs.isGenerateMap(project);
		
		if(!Files.exists(outputBin) || (needMap && !Files.exists(mapFile))) {
			var resource = parseFile(file);
	
			// Report parse errors as markers
			if (resource.getErrors() != null && !resource.getErrors().isEmpty()) {
				throw new CoreException(Status.error("Failed to prepare for launch, parser reported " + resource.getErrors().size() + " errors"));
			}
	
			var program = (AsmProgram) resource.getContents().get(0);
			
			var defines = prefs.getDefines(project);
			var assembler = Z80Assembler.builder()
					.withDefines(defines)
					.withIncludePaths(prefs.getAllIncludePaths(project))
					.withLibPaths(prefs.getAllIncludePaths(project))
					.withMap(mapFile)
					.build();
	
			try(var outstr = Files.newOutputStream(outputBin)) {
				assembler.assemble(file.getName(), program, outstr);
			}
			catch(IOException ioe) {
				throw new CoreException(Status.error("Failed to prepare for launch, failed to assemble.", ioe));
			}
		}
		return outputBin;
	}
}