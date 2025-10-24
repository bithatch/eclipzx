package uk.co.bithatch.zxbasic.ui.library;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import uk.co.bithatch.bitzx.IArchitecture;
import uk.co.bithatch.zxbasic.ui.language.BorielZXBasicArchitecture;
import uk.co.bithatch.zxbasic.ui.preferences.ZXBasicPreferencesAccess;

public class ContributedLibraryRegistry {


	public static Optional<ZXLibrary> getLibrary(IProject project, String name) {
		return getEditableProjectLibraries(project).stream().filter(l -> name.equals(l.name())).findFirst();
	}

	public static List<ZXLibrary> getEditableProjectLibraries(IProject project) {
		return getActiveLibraries(project).
				stream().
				filter(l -> !l.builtIn()).
				toList();
	}
	
	public static List<ZXLibrary> getActiveLibraries(IProject project) {
		return getLibrariesFor(ZXBasicPreferencesAccess.get().getSDK(project), 
				(BorielZXBasicArchitecture) ZXBasicPreferencesAccess.get().getArchitecture(project));
	}
	
	public static List<ZXLibrary> getLibrariesFor(ZXSDK sdk, BorielZXBasicArchitecture arch) {
		return getAllLibraries(sdk).
				stream().
				filter(l -> l.arch() == null || l.arch() == arch).
				toList();
	}
	
	public static List<ZXLibrary> getBuiltInProjectLibraries(IProject project) {
		return getActiveLibraries(project).
				stream().
				filter(l -> l.builtIn()).
				toList();
	}

    public static List<ZXLibrary> getAllLibraries(IProject project) {
        return getAllLibraries(ZXBasicPreferencesAccess.get().getSDK(project));
    }
    
    public static List<ZXLibrary> getAllLibraries(ZXSDK sdk) {
        List<ZXLibrary> result = getContributedLibraries();
        result.addAll(sdk.libraries());
        return result;
    }

	public static List<ZXLibrary> getContributedLibraries(IArchitecture arch) {
		return getContributedLibraries().
				stream().
				filter(l -> l.arch() == null || l.arch() == arch).
				toList();
	}

	public static List<ZXLibrary> getContributedLibraries() {
		List<ZXLibrary> result = new ArrayList<>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("uk.co.bithatch.zxbasic.library");

        IConfigurationElement[] els = point.getConfigurationElements();
        result.addAll(getLibraries(els, null, null, false));
		return result;
	}

	static List<ZXLibrary> getLibraries(IConfigurationElement[] els, String defaultPlugin, String parentPath, boolean builtIn) {
        List<ZXLibrary> result = new ArrayList<>();
		for (IConfigurationElement e : els) {
            if (!"library".equals(e.getName())) continue;

            String name = e.getAttribute("name");
            String arch = e.getAttribute("architecture");
            String path = e.getAttribute("path");
            String icon = e.getAttribute("icon");
            String pluginId = Optional.ofNullable(e.getAttribute("plugin"))
                                      .orElse(defaultPlugin);
            if(pluginId == null) {
            	pluginId  = defaultPlugin;
            	if(pluginId == null) {
            		pluginId = e.getDeclaringExtension().getContributor().getName();
            	}
            }
            
            
            if(parentPath != null) {
            	if(path.startsWith("/")) {
            		path = path.substring(1);
            	}
            	else {
            		path = parentPath + "/" + path;
            	}
            }
            
            if(icon == null) {
            	icon = "icons/library16.png";
            }

            try {
                Bundle bundle = Platform.getBundle(pluginId);
                URL entry = FileLocator.find(bundle, new Path(path), null);
                URL locatedURL = FileLocator.toFileURL(entry);
                if(locatedURL == null) {
                	throw new IOException("Cannot find resources bundle for the ZX library " + 
                				name + ", which should exist at " + path + " in the plugin " + pluginId);
                }
				File file = new File(locatedURL.toURI()).getCanonicalFile();

                result.add(new ZXLibrary(name, pluginId, icon, file, arch == null || arch.equals("") ? null : BorielZXBasicArchitecture.valueOf(arch.toUpperCase()), true, builtIn));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
	}
}
