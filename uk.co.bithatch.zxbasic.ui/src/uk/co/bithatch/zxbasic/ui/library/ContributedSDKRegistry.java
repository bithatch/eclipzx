package uk.co.bithatch.zxbasic.ui.library;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class ContributedSDKRegistry {

    public static List<ZXSDK> getAllSDKs() {
        List<ZXSDK> result = new ArrayList<>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("uk.co.bithatch.zxbasic.sdk");

        for (IConfigurationElement e : point.getConfigurationElements()) {
            if (!"sdk".equals(e.getName())) continue;

            String name = e.getAttribute("name");
            String path = e.getAttribute("path");
            String pluginId = Optional.ofNullable(e.getAttribute("plugin"))
                                      .orElse(e.getContributor().getName());

            try {
                Bundle bundle = Platform.getBundle(pluginId);
                URL entry = FileLocator.find(bundle, new Path(path), null);
                File file = new File(FileLocator.toFileURL(entry).toURI()).getCanonicalFile();

                IConfigurationElement[] libConfigs = e.getChildren("library");
				result.add(new ZXSDK(name, file, ContributedLibraryRegistry.getLibraries(libConfigs, pluginId, path, true)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

	public static String[][] getAllSDKsAsOptions() {
		return getAllSDKs().stream().map(sdk -> new String[] { sdk.name(), sdk.location().getAbsolutePath() }).toList().toArray(new String[0][0]);
	}

	public static Optional<ZXSDK> getSDKByPath(String path) {
		return getAllSDKs().stream().filter(sdk -> sdk.location().getAbsolutePath().equals(path)).findFirst();
	}
	
	public static Optional<ZXSDK> getSDKByName(String name) {
		return getAllSDKs().stream().filter(sdk -> sdk.name().equals(name)).findFirst();
	}

	public static ZXSDK getDefaultSDK() {
		var all = getAllSDKs();
		if(all.isEmpty())
			throw new IllegalStateException("No default SDK.");
		return all.get(0);
	}
}
