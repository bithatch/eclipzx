package uk.co.bithatch.zximgconv;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import uk.co.bithatch.zyxy.graphics.AttributedVideoMemory;
import uk.co.bithatch.zyxy.graphics.Palette;
import uk.co.bithatch.zyxy.graphics.Palette.Entry;
import uk.co.bithatch.zyxy.graphics.StandardAttributedVideoMemory;
import uk.co.bithatch.zyxy.graphics.VideoMemory;
import uk.co.bithatch.zyxy.graphics.VideoMode;

/**
 * Converts standard image files (PNG, JPEG, etc.) to ZX Spectrum / ZX Spectrum
 * Next screen formats using the zyxy graphics library.
 */
public class ZXImageConverter {

	private static final ILog LOG = Platform.getLog(ZXImageConverter.class);

	/**
	 * Dithering algorithms available for conversion.
	 */
	public enum DitherMode {
		NONE("None"),
		FLOYD_STEINBERG("Floyd-Steinberg"),
		ORDERED("Ordered (Bayer 4×4)");

		private final String label;

		DitherMode(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}

		public static String[] labels() {
			return Arrays.stream(values()).map(DitherMode::label).toArray(String[]::new);
		}
	}

	/**
	 * Output format descriptors mapping to {@link VideoMode}.
	 */
	public enum OutputFormat {
		SCR("SCR - Standard ZX Screen (.scr)", "scr", VideoMode.STANDARD),
		SHR("SHR - Hi-Res Screen (.shr)", "shr", VideoMode.HIRES),
		SHC("SHC - Hi-Colour Screen (.shc)", "shc", VideoMode.HICOLOR),
		SLR("SLR - Lo-Res Screen (.slr)", "slr", VideoMode.LORES_256),
		SL2("SL2 - Layer 2 Screen (.sl2)", "sl2", VideoMode.L2_LORES),
		NXI("NXI - Next Image (.nxi)", "nxi", VideoMode.L2_LORES);

		private final String label;
		private final String extension;
		private final VideoMode defaultVideoMode;

		OutputFormat(String label, String extension, VideoMode defaultVideoMode) {
			this.label = label;
			this.extension = extension;
			this.defaultVideoMode = defaultVideoMode;
		}

		public String label() {
			return label;
		}

		public String extension() {
			return extension;
		}

		public VideoMode defaultVideoMode() {
			return defaultVideoMode;
		}

		public static String[] labels() {
			return Arrays.stream(values()).map(OutputFormat::label).toArray(String[]::new);
		}

		/**
		 * Whether this format supports Layer 2 resolution selection.
		 */
		public boolean supportsL2Resolution() {
			return this == SL2 || this == NXI;
		}

		/**
		 * Whether this format supports an embedded palette in the output file.
		 */
		public boolean supportsEmbeddedPalette() {
			return this == NXI;
		}

		/**
		 * Whether this format supports a transparency index.
		 */
		public boolean supportsTransparency() {
			return this == NXI || this == SL2 || this == SLR;
		}
	}

	/**
	 * Layer 2 resolution options for SL2 and NXI formats.
	 */
	public enum L2Resolution {
		RES_256x192("256 × 192", VideoMode.L2_LORES),
		RES_320x256("320 × 256", VideoMode.L2_MEDRES),
		RES_640x256("640 × 256 (16 colours)", VideoMode.L2_HIGHRES);

		private final String label;
		private final VideoMode videoMode;

		L2Resolution(String label, VideoMode videoMode) {
			this.label = label;
			this.videoMode = videoMode;
		}

		public String label() {
			return label;
		}

		public VideoMode videoMode() {
			return videoMode;
		}

		public static String[] labels() {
			return Arrays.stream(values()).map(L2Resolution::label).toArray(String[]::new);
		}
	}

	// Keep backward-compatible arrays for existing references
	public static final String[] OUTPUT_FORMATS = OutputFormat.labels();
	public static final String[] OUTPUT_EXTENSIONS = Arrays.stream(OutputFormat.values())
			.map(OutputFormat::extension).toArray(String[]::new);

	// Bayer 4x4 ordered dither matrix (threshold values normalised to 0-255 range)
	private static final int[][] BAYER_4X4 = {
		{   0, 128,  32, 160 },
		{ 192,  64, 224,  96 },
		{  48, 176,  16, 144 },
		{ 240, 112, 208,  80 },
	};

	/**
	 * Convert {@code sourceFile} to the given ZX output format and write the result
	 * into {@code outputFolder}.
	 *
	 * @param sourceFile    the workspace image file to convert
	 * @param formatIndex   index into {@link OutputFormat} values
	 * @param outputFolder  absolute or workspace-relative path for the output file
	 * @param ditherMode    dithering algorithm to use
	 * @param l2Resolution  Layer 2 resolution index (only used for SL2/NXI formats)
	 * @param palette       optional custom palette (null = use default for mode)
	 * @param paletteFile   optional path to a .pal/.npl file (null = use default)
	 * @param generatePalette if true, generate a palette from the source image and save it
	 * @param embedPalette  if true and format supports it, embed palette in output file
	 * @param transparency  if true, transparent source pixels are mapped to the transparency index
	 * @param transparencyIndex the output palette index to write for transparent pixels (typically 227)
	 * @param alphaThreshold alpha values below this are considered transparent (0-255, default 128)
	 * @param monitor       progress monitor (may be null)
	 */
	public static void convert(IFile sourceFile, int formatIndex, String outputFolder,
			DitherMode ditherMode, int l2Resolution, Palette palette, String paletteFile,
			boolean generatePalette, boolean embedPalette, boolean transparency, int transparencyIndex,
			int alphaThreshold, IProgressMonitor monitor) {

		SubMonitor sub = SubMonitor.convert(monitor, "Converting " + sourceFile.getName(), 100);

		OutputFormat format = OutputFormat.values()[formatIndex];
		String ext = format.extension();
		String baseName = stripExtension(sourceFile.getName()) + "." + ext;

		File outDir;
		if (outputFolder == null || outputFolder.isBlank()) {
			outDir = sourceFile.getLocation().removeLastSegments(1).toFile();
		} else {
			Path resolved = resolvePath(outputFolder, sourceFile);
			outDir = resolved.toFile();
		}

		File outputFile = new File(outDir, baseName);

		try {
			// 1. Read source image
			sub.subTask("Reading " + sourceFile.getName());
			BufferedImage sourceImage = ImageIO.read(sourceFile.getLocation().toFile());
			if (sourceImage == null) {
				throw new IOException("Failed to read image: " + sourceFile.getLocation());
			}
			sub.worked(10);
			
			int sourceTransparencyIndex = sourceImage.getColorModel() instanceof IndexColorModel ? 
					((IndexColorModel) sourceImage.getColorModel()).getTransparentPixel() : -1;

			// 2. Determine video mode
			VideoMode videoMode;
			if (format.supportsL2Resolution() && l2Resolution >= 0 && l2Resolution < L2Resolution.values().length) {
				videoMode = L2Resolution.values()[l2Resolution].videoMode();
			} else {
				videoMode = format.defaultVideoMode();
			}

			// 3. Scale image to target resolution
			sub.subTask("Scaling to " + videoMode.width() + "×" + videoMode.height());
			int w = videoMode.width();
			int h = videoMode.height();
			BufferedImage scaled = scaleImage(sourceImage, w, h);
			sub.worked(10);

			// 4. Resolve palette
			sub.subTask("Resolving palette");
			Palette generatedPalette = null;
			if (generatePalette && palette == null) {
				sub.subTask("Generating palette from source image");
				int maxColors = videoMode.colors();
				generatedPalette = medianCutQuantise(scaled, w, h, maxColors);
				palette = generatedPalette;
			} else if (palette == null) {
				if (paletteFile != null && !paletteFile.isBlank()) {
					palette = loadPaletteFromFile(paletteFile, videoMode.colors(), sourceFile);
				}
				if (palette == null) {
					palette = videoMode.defaultPalette();
				}
			}
			sub.worked(5);

			// Resolve effective transparency index (only for formats that support it)
			int effectiveTransparencyIndex = -1;
			if (transparency && format.supportsTransparency()) {
				effectiveTransparencyIndex = transparencyIndex;
			}

			// 5. Convert
			sub.subTask("Converting to " + format.label());
			byte[] output;
			if (format == OutputFormat.NXI) {
				output = convertNxi(scaled, videoMode, palette, ditherMode, embedPalette, effectiveTransparencyIndex, sourceTransparencyIndex, alphaThreshold);
			} else if (videoMode.ula() && format != OutputFormat.SLR) {
				output = convertAttributed(scaled, videoMode, palette, ditherMode);
			} else {
				output = convertIndexed(scaled, videoMode, palette, ditherMode, effectiveTransparencyIndex, sourceTransparencyIndex, alphaThreshold);
			}
			sub.worked(60);

			// 6. Write output
			sub.subTask("Writing " + outputFile.getName());
			outDir.mkdirs();
			Files.write(outputFile.toPath(), output);

			// 6b. Save generated palette alongside the output file (only if not already embedded)
			if (generatedPalette != null && !(embedPalette && format.supportsEmbeddedPalette())) {
				String palName = stripExtension(sourceFile.getName()) + ".npl";
				File palFile = new File(outDir, palName);
				sub.subTask("Writing generated palette " + palName);
				try (var fos = java.nio.file.Files.newOutputStream(palFile.toPath());
					 var channel = java.nio.channels.Channels.newChannel(fos)) {
					generatedPalette.save(channel);
				}
				LOG.log(new Status(IStatus.INFO, Activator.PLUGIN_ID,
						"Generated palette \"" + palFile.getAbsolutePath() + "\" ("
								+ generatedPalette.colors().length + " colours)"));
			}
			sub.worked(10);

			LOG.log(new Status(IStatus.INFO, Activator.PLUGIN_ID,
					"Converted \"" + sourceFile.getFullPath() + "\" → \""
							+ outputFile.getAbsolutePath() + "\" (" + output.length + " bytes, "
							+ videoMode + ", " + ditherMode + ")"));

			// 7. Refresh workspace — refresh both the output folder and source folder
			sub.subTask("Refreshing workspace");
			try {
				// Find the output folder in the workspace and refresh it
				IResource[] outputContainers = sourceFile.getWorkspace().getRoot()
						.findContainersForLocationURI(outDir.toURI());
				for (IResource container : outputContainers) {
					container.refreshLocal(IResource.DEPTH_ONE, null);
				}
				// Also refresh source parent in case output is co-located
				sourceFile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
			} catch (Exception e) {
				// Non-fatal
			}
			sub.worked(5);

		} catch (Exception e) {
			LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to convert \"" + sourceFile.getFullPath() + "\": " + e.getMessage(), e));
		} finally {
			sub.done();
		}
	}

	/**
	 * Backward-compatible overload.
	 */
	public static void convert(IFile sourceFile, int formatIndex, String outputFolder) {
		convert(sourceFile, formatIndex, outputFolder, DitherMode.FLOYD_STEINBERG, 0, null, null, false, true, false, Palette.DEFAULT_TRANSPARENCY, 128, null);
	}

	// -----------------------------------------------------------------------
	// Indexed mode conversion (SLR, SL2, L2 modes)
	// -----------------------------------------------------------------------

	private static byte[] convertIndexed(BufferedImage img, VideoMode mode, Palette palette, DitherMode dither, int transparencyIndex, int sourceTransparencyIndex, int alphaThreshold) {
		int w = mode.width();
		int h = mode.height();
		VideoMemory vm = mode.createBuffer().asWriteable();

		Entry[] colors = palette.colors();
		int palSize = Math.min(colors.length, mode.colors());
		boolean useTransparency = transparencyIndex >= 0 && transparencyIndex < palSize;

		// Check if source is indexed — if so, we can read raw palette indices
		boolean sourceIsIndexed = img.getType() == BufferedImage.TYPE_BYTE_INDEXED
				|| img.getType() == BufferedImage.TYPE_BYTE_BINARY;
		java.awt.image.Raster sourceRaster = sourceIsIndexed ? img.getRaster() : null;

		int[][] errR = null, errG = null, errB = null;
		if (dither == DitherMode.FLOYD_STEINBERG) {
			errR = new int[h][w];
			errG = new int[h][w];
			errB = new int[h][w];
		}

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				// For indexed source images, check raw palette index
				if (useTransparency && sourceIsIndexed) {
					int srcIdx = sourceRaster.getSample(x, y, 0);
					if (srcIdx == sourceTransparencyIndex) {
						vm.color(x, y, transparencyIndex);
						continue;
					}
				}

				int argb = img.getRGB(x, y);
				int a = (argb >> 24) & 0xff;
				int r = (argb >> 16) & 0xff;
				int g = (argb >> 8) & 0xff;
				int b = argb & 0xff;

				// For non-indexed images (PNG etc.), use alpha channel
				if (useTransparency && !sourceIsIndexed && a < alphaThreshold) {
					vm.color(x, y, transparencyIndex);
					continue;
				}

				if (dither == DitherMode.FLOYD_STEINBERG) {
					r = clamp(r + errR[y][x]);
					g = clamp(g + errG[y][x]);
					b = clamp(b + errB[y][x]);
				} else if (dither == DitherMode.ORDERED) {
					int threshold = BAYER_4X4[y % 4][x % 4] - 128;
					r = clamp(r + threshold);
					g = clamp(g + threshold);
					b = clamp(b + threshold);
				}

				int bestIdx = findNearest(r, g, b, colors, palSize);
				vm.color(x, y, bestIdx);

				if (dither == DitherMode.FLOYD_STEINBERG) {
					Entry chosen = colors[bestIdx];
					int er = r - chosen.r();
					int eg = g - chosen.g();
					int eb = b - chosen.b();
					distributeError(errR, errG, errB, x, y, w, h, er, eg, eb);
				}
			}
		}

		return extractBytes(vm, mode.byteSize());
	}

	// -----------------------------------------------------------------------
	// Attributed mode conversion (SCR, SHR, SHC)
	// -----------------------------------------------------------------------

	private static byte[] convertAttributed(BufferedImage img, VideoMode mode, Palette palette, DitherMode dither) {
		int w = mode.width();
		int h = mode.height();
		VideoMemory rawVm = mode.createBuffer().asWriteable();

		if (!(rawVm instanceof AttributedVideoMemory avm)) {
			return convertIndexed(img, mode, palette, dither, -1, -1, 128);
		}

		Entry[] colors = palette.colors();
		int palSize = Math.min(colors.length, mode.colors());
		int cellW = avm.attributeCellWidth();
		int cellH = avm.attributeCellHeight();

		int cols = w / cellW;
		int rows = h / cellH;

		for (int cr = 0; cr < rows; cr++) {
			for (int cc = 0; cc < cols; cc++) {
				int cx = cc * cellW;
				int cy = cr * cellH;

				// Collect all pixel RGB values in this cell
				int[][] cellPixels = new int[cellH][cellW * 3];
				for (int ly = 0; ly < cellH; ly++) {
					for (int lx = 0; lx < cellW; lx++) {
						int rgb = img.getRGB(cx + lx, cy + ly);
						cellPixels[ly][lx * 3] = (rgb >> 16) & 0xff;
						cellPixels[ly][lx * 3 + 1] = (rgb >> 8) & 0xff;
						cellPixels[ly][lx * 3 + 2] = rgb & 0xff;
					}
				}

				// Find best ink/paper pair for this cell
				int[] bestPair = findBestInkPaper(cellPixels, cellW, cellH, colors, palSize);
				int inkIdx = bestPair[0];
				int paperIdx = bestPair[1];

				// Set attributes
				avm.ink(cx, cy, inkIdx);
				avm.paper(cx, cy, paperIdx);

				if (avm instanceof StandardAttributedVideoMemory savm) {
					boolean bright = inkIdx >= 8 || paperIdx >= 8;
					savm.bright(cx, cy, bright);
					if (bright) {
						inkIdx = inkIdx >= 8 ? inkIdx - 8 : inkIdx;
						paperIdx = paperIdx >= 8 ? paperIdx - 8 : paperIdx;
						avm.ink(cx, cy, inkIdx);
						avm.paper(cx, cy, paperIdx);
					}
				}

				Entry inkColor = colors[bestPair[0]];
				Entry paperColor = colors[bestPair[1]];

				for (int ly = 0; ly < cellH; ly++) {
					for (int lx = 0; lx < cellW; lx++) {
						int pr = cellPixels[ly][lx * 3];
						int pg = cellPixels[ly][lx * 3 + 1];
						int pb = cellPixels[ly][lx * 3 + 2];

						if (dither == DitherMode.ORDERED) {
							int threshold = BAYER_4X4[(cy + ly) % 4][(cx + lx) % 4] - 128;
							pr = clamp(pr + threshold);
							pg = clamp(pg + threshold);
							pb = clamp(pb + threshold);
						}

						int distInk = colorDistance(pr, pg, pb, inkColor);
						int distPaper = colorDistance(pr, pg, pb, paperColor);

						if (distInk <= distPaper) {
							avm.set(cx + lx, cy + ly);
						} else {
							avm.clear(cx + lx, cy + ly);
						}
					}
				}
			}
		}

		return extractBytes(rawVm, mode.byteSize());
	}

	// -----------------------------------------------------------------------
	// NXI conversion (palette header + L2 pixel data)
	// -----------------------------------------------------------------------

	private static byte[] convertNxi(BufferedImage img, VideoMode mode, Palette palette, DitherMode dither, boolean embedPalette, int transparencyIndex, int sourceTransparencyIndex, int alphaThreshold) {
		int w = mode.width();
		int h = mode.height();

		// Use provided palette; only quantise when no palette was given at all
		Palette quantised;
		if (palette != null) {
			quantised = palette;
		} else {
			quantised = medianCutQuantise(img, w, h, 256);
		}
		Entry[] colors = quantised.colors();
		boolean useTransparency = transparencyIndex >= 0 && transparencyIndex < colors.length;

		boolean sourceIsIndexed = img.getType() == BufferedImage.TYPE_BYTE_INDEXED
				|| img.getType() == BufferedImage.TYPE_BYTE_BINARY;

		VideoMemory vm = mode.createBuffer().asWriteable();

		int[][] errR = null, errG = null, errB = null;
		if (dither == DitherMode.FLOYD_STEINBERG) {
			errR = new int[h][w];
			errG = new int[h][w];
			errB = new int[h][w];
		}

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				// For indexed source images at the same resolution, check raw palette index
				if (useTransparency && sourceIsIndexed) {
					int srcIdx = img.getRaster().getSample(x, y, 0);
					if (srcIdx == sourceTransparencyIndex) {
						vm.color(x, y, transparencyIndex);
						continue;
					}
				}

				int argb = img.getRGB(x, y);
				int a = (argb >> 24) & 0xff;
				int r = (argb >> 16) & 0xff;
				int g = (argb >> 8) & 0xff;
				int b = argb & 0xff;

				// For non-indexed images (PNG etc.), use alpha channel
				if (useTransparency && !sourceIsIndexed && a < alphaThreshold) {
					vm.color(x, y, transparencyIndex);
					continue;
				}

				if (dither == DitherMode.FLOYD_STEINBERG) {
					r = clamp(r + errR[y][x]);
					g = clamp(g + errG[y][x]);
					b = clamp(b + errB[y][x]);
				} else if (dither == DitherMode.ORDERED) {
					int threshold = BAYER_4X4[y % 4][x % 4] - 128;
					r = clamp(r + threshold);
					g = clamp(g + threshold);
					b = clamp(b + threshold);
				}

				int bestIdx = findNearest(r, g, b, colors, colors.length);
				vm.color(x, y, bestIdx);

				if (dither == DitherMode.FLOYD_STEINBERG) {
					Entry chosen = colors[bestIdx];
					int er = r - chosen.r();
					int eg = g - chosen.g();
					int eb = b - chosen.b();
					distributeError(errR, errG, errB, x, y, w, h, er, eg, eb);
				}
			}
		}

		byte[] pixelData = extractBytes(vm, mode.byteSize());

		if (embedPalette) {
			byte[] paletteBytes = quantised.asBytes();
			byte[] palHeader = new byte[512];
			System.arraycopy(paletteBytes, 0, palHeader, 0, Math.min(paletteBytes.length, 512));

			byte[] nxi = new byte[palHeader.length + pixelData.length];
			System.arraycopy(palHeader, 0, nxi, 0, palHeader.length);
			System.arraycopy(pixelData, 0, nxi, palHeader.length, pixelData.length);
			return nxi;
		} else {
			return pixelData;
		}
	}

	// -----------------------------------------------------------------------
	// Median-cut quantisation
	// -----------------------------------------------------------------------

	private static Palette medianCutQuantise(BufferedImage img, int w, int h, int maxColors) {
		List<int[]> pixels = new ArrayList<>();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int rgb = img.getRGB(x, y);
				pixels.add(new int[] { (rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff });
			}
		}

		List<List<int[]>> buckets = new ArrayList<>();
		buckets.add(pixels);

		while (buckets.size() < maxColors) {
			int bestBucket = -1;
			int bestRange = -1;
			int bestChannel = 0;

			for (int i = 0; i < buckets.size(); i++) {
				List<int[]> bucket = buckets.get(i);
				if (bucket.size() < 2) continue;
				for (int ch = 0; ch < 3; ch++) {
					int min = 255, max = 0;
					for (int[] p : bucket) {
						if (p[ch] < min) min = p[ch];
						if (p[ch] > max) max = p[ch];
					}
					int range = max - min;
					if (range > bestRange) {
						bestRange = range;
						bestBucket = i;
						bestChannel = ch;
					}
				}
			}

			if (bestBucket < 0 || bestRange == 0) break;

			int ch = bestChannel;
			List<int[]> bucket = buckets.remove(bestBucket);
			bucket.sort(Comparator.comparingInt(p -> p[ch]));
			int mid = bucket.size() / 2;
			buckets.add(new ArrayList<>(bucket.subList(0, mid)));
			buckets.add(new ArrayList<>(bucket.subList(mid, bucket.size())));
		}

		List<Entry> entries = new ArrayList<>();
		for (List<int[]> bucket : buckets) {
			if (bucket.isEmpty()) continue;
			long tr = 0, tg = 0, tb = 0;
			for (int[] p : bucket) {
				tr += p[0];
				tg += p[1];
				tb += p[2];
			}
			int ar = (int) (tr / bucket.size());
			int ag = (int) (tg / bucket.size());
			int ab = (int) (tb / bucket.size());
			ar = quantise3bit(ar);
			ag = quantise3bit(ag);
			ab = quantise3bit(ab);
			entries.add(new Entry(ar, ag, ab));
		}

		while (entries.size() < maxColors) {
			entries.add(new Entry(0, 0, 0));
		}

		return Palette.of(entries);
	}

	private static int quantise3bit(int value) {
		int v3 = (value >> 5) & 0x7;
		return Palette.decodeComponent(v3);
	}

	// -----------------------------------------------------------------------
	// Utility methods
	// -----------------------------------------------------------------------

	/**
	 * Handle transparency for indexed and non-indexed source images.
	 * <p>
	 * For indexed (paletted) images without alpha (e.g. BMP): creates a new
	 * indexed image with an {@link java.awt.image.IndexColorModel} that marks
	 * {@code sourceTransparencyIndex} as the transparent pixel. The image
	 * remains indexed so palette indices are preserved through scaling.
	 * <p>
	 * For images that already have an alpha channel (e.g. PNG): returned as-is;
	 * the conversion loop handles alpha directly.
	 */
	private static BufferedImage applyIndexedTransparency(BufferedImage img, int sourceTransparencyIndex, int outputTransparencyIndex) {
		int type = img.getType();
		if (type != BufferedImage.TYPE_BYTE_INDEXED && type != BufferedImage.TYPE_BYTE_BINARY) {
			// Not an indexed image — alpha channel (if any) is already usable
			return img;
		}

		java.awt.image.IndexColorModel icm = (java.awt.image.IndexColorModel) img.getColorModel();

		// If the IndexColorModel already has a transparent pixel, nothing to do
		if (icm.getTransparentPixel() >= 0) {
			return img;
		}

		// Build a new IndexColorModel with the source transparency index marked
		int mapSize = icm.getMapSize();
		byte[] r = new byte[mapSize];
		byte[] g = new byte[mapSize];
		byte[] b = new byte[mapSize];
		icm.getReds(r);
		icm.getGreens(g);
		icm.getBlues(b);

		java.awt.image.IndexColorModel newIcm = new java.awt.image.IndexColorModel(
				icm.getPixelSize(), mapSize, r, g, b, sourceTransparencyIndex);

		// Create a new image with the same raster but the new color model
		return new BufferedImage(newIcm, img.getRaster(), img.isAlphaPremultiplied(), null);
	}

	private static BufferedImage scaleImage(BufferedImage src, int w, int h) {
		if (src.getWidth() == w && src.getHeight() == h) {
			return src;
		}
		BufferedImage scaled = new BufferedImage(w, h, src.getType());
		Graphics2D g2 = scaled.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.drawImage(src, 0, 0, w, h, null);
		g2.dispose();
		return scaled;
	}

	private static int findNearest(int r, int g, int b, Entry[] palette, int palSize) {
		return findNearest(r, g, b, palette, palSize, -1);
	}

	/**
	 * Encode an RGB colour directly as an RRRGGGBB byte, matching the ZX Next
	 * default Layer 2 palette identity mapping. Each component is truncated
	 * (floored) to its top bits: R=3 bits, G=3 bits, B=2 bits.
	 */
	private static int encodeRGB332(int r, int g, int b) {
		return ((r >> 5) << 5) | ((g >> 5) << 2) | (b >> 6);
	}

	/**
	 * Returns {@code true} when the supplied palette is the default RGB333
	 * identity palette (256 entries whose index encodes the colour directly).
	 */
	private static boolean isDefaultRGB333(Entry[] palette, int palSize) {
		if (palSize != 256) return false;
		// Spot-check a few entries to confirm identity mapping
		for (int i : new int[] { 0, 32, 64, 128, 237, 255 }) {
			Entry e = palette[i];
			int expected = encodeRGB332(e.r(), e.g(), e.b());
			if (expected != i) return false;
		}
		return true;
	}

	private static int findNearest(int r, int g, int b, Entry[] palette, int palSize, int skipIndex) {
		// Fast path: for the default 256-colour RGB332 identity palette, encode directly
		// rather than searching — this matches the per-component quantisation that
		// other ZX Next tools use with the default palette.
		if (isDefaultRGB333(palette, palSize)) {
			int direct = encodeRGB332(r, g, b);
			if (skipIndex < 0 || direct != skipIndex) return direct;
			// Fall through to search if the direct encoding hits the skip index
		}

		int bestIdx = 0;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < palSize; i++) {
			if (i == skipIndex) continue;
			Entry e = palette[i];
			int dist = colorDistanceRaw(r, g, b, e.r(), e.g(), e.b());
			if (dist < bestDist) {
				bestDist = dist;
				bestIdx = i;
			}
		}
		return bestIdx;
	}

	private static int[] findBestInkPaper(int[][] cellPixels, int cellW, int cellH,
			Entry[] colors, int palSize) {
		long tr = 0, tg = 0, tb = 0;
		int count = cellW * cellH;
		int darkestR = 0, darkestG = 0, darkestB = 0;
		int lightestR = 0, lightestG = 0, lightestB = 0;
		int minLum = Integer.MAX_VALUE;
		int maxLum = Integer.MIN_VALUE;

		for (int ly = 0; ly < cellH; ly++) {
			for (int lx = 0; lx < cellW; lx++) {
				int r = cellPixels[ly][lx * 3];
				int g = cellPixels[ly][lx * 3 + 1];
				int b = cellPixels[ly][lx * 3 + 2];
				tr += r; tg += g; tb += b;
				int lum = r * 299 + g * 587 + b * 114;
				if (lum < minLum) {
					minLum = lum;
					darkestR = r; darkestG = g; darkestB = b;
				}
				if (lum > maxLum) {
					maxLum = lum;
					lightestR = r; lightestG = g; lightestB = b;
				}
			}
		}

		int paperIdx = findNearest(darkestR, darkestG, darkestB, colors, palSize);
		int inkIdx = findNearest(lightestR, lightestG, lightestB, colors, palSize);

		if (inkIdx == paperIdx) {
			inkIdx = (inkIdx + 1) % palSize;
		}

		int avgR = (int) (tr / count);
		int avgG = (int) (tg / count);
		int avgB = (int) (tb / count);
		int avgIdx = findNearest(avgR, avgG, avgB, colors, palSize);

		int bestInk = inkIdx;
		int bestPaper = paperIdx;
		int bestError = totalCellError(cellPixels, cellW, cellH, colors[inkIdx], colors[paperIdx]);

		for (int candidate : new int[] { inkIdx, paperIdx, avgIdx }) {
			for (int other = 0; other < palSize; other++) {
				if (other == candidate) continue;
				int err = totalCellError(cellPixels, cellW, cellH, colors[candidate], colors[other]);
				if (err < bestError) {
					bestError = err;
					bestInk = candidate;
					bestPaper = other;
				}
			}
		}

		return new int[] { bestInk, bestPaper };
	}

	private static int totalCellError(int[][] cellPixels, int cellW, int cellH,
			Entry ink, Entry paper) {
		int total = 0;
		for (int ly = 0; ly < cellH; ly++) {
			for (int lx = 0; lx < cellW; lx++) {
				int r = cellPixels[ly][lx * 3];
				int g = cellPixels[ly][lx * 3 + 1];
				int b = cellPixels[ly][lx * 3 + 2];
				int dInk = colorDistanceRaw(r, g, b, ink.r(), ink.g(), ink.b());
				int dPaper = colorDistanceRaw(r, g, b, paper.r(), paper.g(), paper.b());
				total += Math.min(dInk, dPaper);
			}
		}
		return total;
	}

	private static int colorDistance(int r, int g, int b, Entry e) {
		return colorDistanceRaw(r, g, b, e.r(), e.g(), e.b());
	}

	private static int colorDistanceRaw(int r1, int g1, int b1, int r2, int g2, int b2) {
		int dr = r1 - r2;
		int dg = g1 - g2;
		int db = b1 - b2;
		return 2 * dr * dr + 4 * dg * dg + 3 * db * db;
	}

	private static void distributeError(int[][] errR, int[][] errG, int[][] errB,
			int x, int y, int w, int h, int er, int eg, int eb) {
		if (x + 1 < w) {
			errR[y][x + 1] += er * 7 / 16;
			errG[y][x + 1] += eg * 7 / 16;
			errB[y][x + 1] += eb * 7 / 16;
		}
		if (y + 1 < h) {
			if (x > 0) {
				errR[y + 1][x - 1] += er * 3 / 16;
				errG[y + 1][x - 1] += eg * 3 / 16;
				errB[y + 1][x - 1] += eb * 3 / 16;
			}
			errR[y + 1][x] += er * 5 / 16;
			errG[y + 1][x] += eg * 5 / 16;
			errB[y + 1][x] += eb * 5 / 16;
			if (x + 1 < w) {
				errR[y + 1][x + 1] += er / 16;
				errG[y + 1][x + 1] += eg / 16;
				errB[y + 1][x + 1] += eb / 16;
			}
		}
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}

	private static byte[] extractBytes(VideoMemory vm, int size) {
		ByteBuffer buf = vm.data();
		byte[] data = new byte[size];
		buf.position(0);
		buf.get(data, 0, Math.min(buf.remaining(), size));
		return data;
	}

	private static Palette loadPaletteFromFile(String path, int maxColors) {
		return loadPaletteFromFile(path, maxColors, null);
	}

	private static Palette loadPaletteFromFile(String palettePath, int maxColors, IFile contextFile) {
		try {
			Path resolved = resolvePath(palettePath, contextFile);
			var channel = java.nio.channels.Channels.newChannel(
					Files.newInputStream(resolved));
			return Palette.load(channel, maxColors);
		} catch (IOException e) {
			LOG.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
					"Failed to load palette from " + palettePath + ", using default", e));
			return null;
		}
	}

	/**
	 * Resolve a path that may be project-relative or absolute.
	 * <p>
	 * If the path is absolute it is used as a filesystem path directly.
	 * Otherwise it is treated as a project-relative path and resolved via the
	 * project that owns the context file.
	 */
	private static Path resolvePath(String pathStr, IFile contextFile) {
		if (pathStr == null || pathStr.isBlank()) {
			return null;
		}

		Path p = Path.of(pathStr);
		if (p.isAbsolute()) {
			return p;
		}

		// Relative path — resolve against the project root
		if (contextFile != null) {
			IPath projectLoc = contextFile.getProject().getLocation();
			if (projectLoc != null) {
				return projectLoc.toFile().toPath().resolve(pathStr);
			}
		}

		return p;
	}

	private static String stripExtension(String name) {
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}
}
