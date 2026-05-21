package uk.co.bithatch.zximgconv;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubMonitor;

/**
 * An incremental project builder that converts image files marked with the
 * "convert on build" persistent property to ZX Spectrum screen formats.
 */
public class ZXImageConversionBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "uk.co.bithatch.zximgconv.builder";

	public static final String PROP_CONVERT_ON_BUILD = "convertOnBuild";
	public static final String PROP_OUTPUT_FORMAT = "outputFormat";
	public static final String PROP_OUTPUT_FOLDER = "outputFolder";
	public static final String PROP_DITHER_MODE = "ditherMode";
	public static final String PROP_L2_RESOLUTION = "l2Resolution";
	public static final String PROP_PALETTE_FILE = "paletteFile";
	public static final String PROP_GENERATE_PALETTE = "generatePalette";
	public static final String PROP_EMBED_PALETTE = "embedPalette";

	private static final ILog LOG = Platform.getLog(ZXImageConversionBuilder.class);

	private static final Set<String> IMAGE_EXTENSIONS = Set.of(
			"png", "gif", "jpg", "jpeg", "bmp", "tiff", "tif", "webp");

	/** Extensions produced by this builder */
	private static final Set<String> OUTPUT_EXTENSIONS = Set.of(
			"scr", "shr", "shc", "slr", "sl2", "nxi", "npl");

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	private void fullBuild(IProgressMonitor monitor) throws CoreException {
		SubMonitor sub = SubMonitor.convert(monitor, "ZX Image Conversion - full build", IProgressMonitor.UNKNOWN);

		// First pass: collect all marked image files
		List<IFile> filesToConvert = new ArrayList<>();
		getProject().accept((IResourceVisitor) resource -> {
			if (isMarkedImage(resource)) {
				filesToConvert.add((IFile) resource);
			}
			return true;
		});

		if (filesToConvert.isEmpty()) {
			sub.done();
			return;
		}

		sub.beginTask("ZX Image Conversion - converting " + filesToConvert.size() + " image(s)",
				filesToConvert.size() * 100);

		for (IFile file : filesToConvert) {
			if (sub.isCanceled()) break;
			processResource(file, false, sub.split(100));
		}

		sub.done();
	}

	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		SubMonitor sub = SubMonitor.convert(monitor, "ZX Image Conversion - incremental build",
				IProgressMonitor.UNKNOWN);

		// Collect changed images and detect deleted outputs
		final List<IFile> changedImages = new ArrayList<>();
		final boolean[] outputDeleted = { false };

		delta.accept((IResourceDeltaVisitor) d -> {
			IResource resource = d.getResource();
			switch (d.getKind()) {
			case IResourceDelta.ADDED:
			case IResourceDelta.CHANGED:
				if (isMarkedImage(resource)) {
					changedImages.add((IFile) resource);
				}
				break;
			case IResourceDelta.REMOVED:
				if (resource.getType() == IResource.FILE) {
					String ext = resource.getFileExtension();
					if (ext != null && OUTPUT_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
						outputDeleted[0] = true;
					}
				}
				break;
			default:
				break;
			}
			return true;
		});

		// Process changed images
		int totalWork = changedImages.size() * 100;

		// If outputs were deleted, collect missing ones too
		List<IFile> missingOutputFiles = new ArrayList<>();
		if (outputDeleted[0]) {
			getProject().accept((IResourceVisitor) resource -> {
				if (isMarkedImage(resource)) {
					IFile file = (IFile) resource;
					if (!changedImages.contains(file) && isOutputMissing(file)) {
						missingOutputFiles.add(file);
					}
				}
				return true;
			});
			totalWork += missingOutputFiles.size() * 100;
		}

		if (totalWork == 0) {
			sub.done();
			return;
		}

		sub.beginTask("ZX Image Conversion - converting "
				+ (changedImages.size() + missingOutputFiles.size()) + " image(s)", totalWork);

		for (IFile file : changedImages) {
			if (sub.isCanceled()) break;
			processResource(file, false, sub.split(100));
		}

		for (IFile file : missingOutputFiles) {
			if (sub.isCanceled()) break;
			processResource(file, true, sub.split(100));
		}

		sub.done();
	}

	/**
	 * Check if a resource is an image file marked for conversion.
	 */
	private boolean isMarkedImage(IResource resource) throws CoreException {
		if (resource.getType() != IResource.FILE) return false;
		String ext = resource.getFileExtension();
		if (ext == null || !IMAGE_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) return false;
		IFile file = (IFile) resource;
		String convert = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_CONVERT_ON_BUILD));
		return "true".equals(convert);
	}

	/**
	 * Check if the output file for a marked image is missing.
	 */
	private boolean isOutputMissing(IFile file) throws CoreException {
		String formatStr = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_OUTPUT_FORMAT));
		int formatIndex = 0;
		if (formatStr != null) {
			try { formatIndex = Integer.parseInt(formatStr); } catch (NumberFormatException e) { }
		}
		if (formatIndex < 0 || formatIndex >= ZXImageConverter.OUTPUT_FORMATS.length) formatIndex = 0;
		String outputFolder = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_OUTPUT_FOLDER));
		File outputFile = resolveOutputFile(file, formatIndex, outputFolder);
		return outputFile == null || !outputFile.exists();
	}

	/**
	 * Process a resource, converting it if it's a marked image file.
	 *
	 * @param resource         the resource to check
	 * @param onlyIfMissing    if true, only convert if the output file doesn't exist
	 * @param monitor          progress monitor
	 */
	private void processResource(IResource resource, boolean onlyIfMissing, IProgressMonitor monitor)
			throws CoreException {
		if (resource.getType() != IResource.FILE) {
			return;
		}
		String ext = resource.getFileExtension();
		if (ext == null || !IMAGE_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
			return;
		}
		IFile file = (IFile) resource;
		String convert = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_CONVERT_ON_BUILD));
		if (!"true".equals(convert)) {
			return;
		}

		String formatStr = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_OUTPUT_FORMAT));
		int formatIndex = 0;
		if (formatStr != null) {
			try {
				formatIndex = Integer.parseInt(formatStr);
			} catch (NumberFormatException e) {
				// default to 0
			}
		}
		if (formatIndex < 0 || formatIndex >= ZXImageConverter.OUTPUT_FORMATS.length) {
			formatIndex = 0;
		}

		String outputFolder = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_OUTPUT_FOLDER));

		// If only rebuilding missing files, check if output already exists
		if (onlyIfMissing) {
			File outputFile = resolveOutputFile(file, formatIndex, outputFolder);
			if (outputFile != null && outputFile.exists()) {
				return;
			}
		}

		// Dither mode
		String ditherStr = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_DITHER_MODE));
		ZXImageConverter.DitherMode ditherMode = ZXImageConverter.DitherMode.FLOYD_STEINBERG;
		if (ditherStr != null) {
			try {
				int ditherIdx = Integer.parseInt(ditherStr);
				if (ditherIdx >= 0 && ditherIdx < ZXImageConverter.DitherMode.values().length) {
					ditherMode = ZXImageConverter.DitherMode.values()[ditherIdx];
				}
			} catch (NumberFormatException e) {
				// default
			}
		}

		// Layer 2 resolution
		String l2ResStr = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_L2_RESOLUTION));
		int l2Resolution = 0;
		if (l2ResStr != null) {
			try {
				l2Resolution = Integer.parseInt(l2ResStr);
			} catch (NumberFormatException e) {
				// default to 0
			}
		}

		// Custom palette file
		String paletteFile = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_PALETTE_FILE));

		// Generate palette
		String genPalStr = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_GENERATE_PALETTE));
		boolean generatePalette = "true".equals(genPalStr);

		// Embed palette
		String embedPalStr = file.getPersistentProperty(
				new QualifiedName(Activator.PLUGIN_ID, PROP_EMBED_PALETTE));
		boolean embedPalette = embedPalStr == null || "true".equals(embedPalStr); // default true

		ZXImageConverter.convert(file, formatIndex, outputFolder, ditherMode, l2Resolution, null, paletteFile, generatePalette, embedPalette, monitor);
	}

	/**
	 * Resolve the expected output file path for a source image, mirroring
	 * the logic in {@link ZXImageConverter#convert}.
	 */
	private File resolveOutputFile(IFile sourceFile, int formatIndex, String outputFolder) {
		try {
			ZXImageConverter.OutputFormat format = ZXImageConverter.OutputFormat.values()[formatIndex];
			String baseName = stripExtension(sourceFile.getName()) + "." + format.extension();

			File outDir;
			if (outputFolder == null || outputFolder.isBlank()) {
				IPath loc = sourceFile.getLocation();
				if (loc == null) return null;
				outDir = loc.removeLastSegments(1).toFile();
			} else {
				java.nio.file.Path p = java.nio.file.Path.of(outputFolder);
				if (p.isAbsolute()) {
					outDir = p.toFile();
				} else {
					IResource wsResource = sourceFile.getWorkspace().getRoot().findMember(outputFolder);
					if (wsResource != null && wsResource.getLocation() != null) {
						outDir = wsResource.getLocation().toFile();
					} else {
						IPath wsRoot = sourceFile.getWorkspace().getRoot().getLocation();
						outDir = wsRoot.toFile().toPath().resolve(outputFolder).toFile();
					}
				}
			}

			return new File(outDir, baseName);
		} catch (Exception e) {
			return null;
		}
	}

	private static String stripExtension(String name) {
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}
}
