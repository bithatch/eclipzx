package uk.co.bithatch.nextzxos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;

import uk.co.bithatch.emuzx.ExternalEmulatorLaunchConfigurationAttributes;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class NextZXOSHelper {
	private final static ILog LOG = ILog.of(NextZXOSHelper.class);
	
	private final  ILaunchConfiguration configuration;
	private final boolean resetImageState;
	private final IFolder out;
	private final Path workingImageFile;
	
	public NextZXOSHelper(ILaunchConfiguration configuration, Path workingImageFile, boolean resetImageState) throws CoreException {
		this.configuration = configuration;
		this.resetImageState = resetImageState;
		this.workingImageFile = workingImageFile;
		
		/* We need the project from the launch configuration to determine where to copy the file to */
		var project = ResourcesPlugin.getWorkspace().getRoot().getProject(configuration.getAttribute(ExternalEmulatorLaunchConfigurationAttributes.PROJECT, ""));
		out = ZXBasicPreferencesAccess.get().getOutputFolder(project);
		
		/* The working image file needs to be known up front */
		LOG.info(String.format("Working Next ZXOS image will be at %s", workingImageFile));
	}
	
	public Path getWorkingImageFile() {
		return workingImageFile;
	}

	public void getImage(IProgressMonitor progress) throws CoreException {
		
		try {
			/* Locate  the base image file, which may involve copying from a URL and/or extracting from a zip archive. This method will return the path to the image file that we can use as the basis for our FAT preparation. */
			var baseImageFile = locateBaseImage(progress);
			
			/*
			 * Now create a new image path. If this path does not exist, or if
			 * "resetImageState" is true, then we will copy the base image to this new
			 * location and return the path to the new location. If the file already exists
			 * and "resetImageState" is false, then we can just return the path to the
			 * existing file.
			 */
			if(!Files.exists(workingImageFile) || resetImageState) {
				LOG.info(String.format("Copying Next ZXOS image from %s to %s", baseImageFile, workingImageFile));
				try(var in = baseImageFile.openStream()) {
					Files.copy(in, workingImageFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			else {
				LOG.info(String.format("Working Next ZXOS image already exists at %s and resetImageState is false, using that", workingImageFile));
			}
		}
		catch(IOException | URISyntaxException e) {
			LOG.error("Failed to access Next ZXOS image", e);
			throw new CoreException(Status.error("Failed to access Next ZXOS image", e));
		}
	}

	private URL locateBaseImage(IProgressMonitor progress)
			throws MalformedURLException, CoreException, IOException, URISyntaxException, ZipException {
		/* First make sure we have a local copy of the source image */
		var localImageOrArchivePath = obtainLocalImageOrArchive(progress);
		var filename = extractFilename(localImageOrArchivePath.getPath());
		
		/* Is this a zip file? If so, we need to extract the image from it and
		 * return the path to the extracted image. If not, then we can just return the
		 * path to the local image file.
		 */
		if(localImageOrArchivePath.toString().toLowerCase().endsWith(".zip")) {
			LOG.info(String.format("Next ZXOS image is a zip file at %s, need to extract it", localImageOrArchivePath));
			
			var imageFile = out.getLocation().toPath().resolve(filename.toString().replaceAll("\\.zip$", ".img"));
			if(!Files.exists(imageFile)) {
				LOG.info(String.format("Extracting Next ZXOS image from %s to %s", localImageOrArchivePath, imageFile));
				var found = false;
				try(var in = localImageOrArchivePath.openStream()) {
					
					var zis = new ZipInputStream(in);
					ZipEntry zipEntry = zis.getNextEntry();
					while (zipEntry != null) {
						if(zipEntry.getName().toLowerCase().endsWith(".img")) {
							 LOG.info(String.format("Found image file in zip: %s", zipEntry.getName()));
							 Files.copy(zis, imageFile);
							 found = true;
							 break;
						 }
						zipEntry = zis.getNextEntry();
					}
					zis.closeEntry();
				}
				if(!found) {
					throw new CoreException(Status.error("Failed to find image file in zip archive"));
				}
			}
			else {
				LOG.info(String.format("Extracted Next ZXOS image already exists at %s, using that", imageFile));
			}
			
			return imageFile.toUri().toURL();
		}
		else {
			LOG.info(String.format("Next ZXOS image is a file at %s, using that", localImageOrArchivePath));
			return localImageOrArchivePath;
		}
	}

	private static String extractFilename(String path) {
		var idx = path.lastIndexOf("/");
		if(idx >= 0 && idx < path.length() - 1) {
			return path.substring(idx + 1);
		}
		throw new IllegalArgumentException("Failed to extract filename from path: " + path);
	}

	private URL obtainLocalImageOrArchive(IProgressMonitor progress) throws MalformedURLException, CoreException, IOException, URISyntaxException {
		var url = getNextZXOSUrl();
		
		/* If the URL is anything other than a file URL, then we need to copy the
		 * contents to a local file, and return the path to that file. 
		 */
		if(url.getProtocol().equals("file") || url.getProtocol().equals("jar") || url.getProtocol().equals("bundleresource")) {
			return url;
		}
		else {
			

			/* Create an image file in the output folder for this project, and copy the
			 * contents of the URL to that location 
			 */
			
			var ext = extractExtensionForURL(url.toString());
			var outf = out.getFile(Integer.toUnsignedLong(configuration.getName().hashCode()) + "-download." + ext);
			var imgfile = outf.getLocation().toPath();
			LOG.info(String.format("Copying Next ZXOS image from %s to %s", url, imgfile));
			
			if(!Files.exists(imgfile)) {
				try(var in = url.openStream()) {
					Files.copy(in, imgfile);
				}
			}
			
			return imgfile.toUri().toURL();
			
		 }
	}
	
	private String extractExtensionForURL(String urlStr) {
		var idx = urlStr.lastIndexOf(".");
		if(idx > 0 && idx < urlStr.length() - 1) {
			var str = urlStr.substring(idx + 1);
			if(str.equalsIgnoreCase("zip")) {
				return "zip";
			}
		}
		return "img";
	}

	private URL getNextZXOSUrl() throws MalformedURLException {
		var locationStr = PreferencesAccess.get().getPreferenceStore().getString(PreferenceConstants.NEXT_ZXOS);
		if(locationStr == null || locationStr.isBlank()) {
			return NextZXOSHelper.class.getResource("/resources/cspect-next-2gb.zip");
		}
		else {
			try {
				return URI.create(locationStr).toURL();
			}
			catch(IllegalArgumentException e) {
				return Path.of(locationStr).toUri().toURL();
			}
		}
	}
}
