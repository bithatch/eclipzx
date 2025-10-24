package uk.co.bithatch.eclipzx.ui.intro;

import java.util.ArrayList;
import java.util.Map;
import java.util.jar.Manifest;

import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IntroConfigurer;
import org.eclipse.ui.intro.config.IntroElement;

import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.zxbasic.ui.library.ContributedSDKRegistry;
import uk.co.bithatch.zxbasic.ui.tools.Python;

public class EclipZXIntroConfigurer extends IntroConfigurer {

	@Override
	public void init(IIntroSite site, Map<String, String> themeProperties) {
	}

	@Override
	public String getVariable(String variableName) {
		if ("eclipzx.version".equals(variableName)) {
			/* TODO hmm vasriables dont seem to work */
			return getVersion();
		}
		return null;
	}

	@Override
	public IntroElement[] getGroupChildren(String pageId, String groupId) {
		var elements = new ArrayList<IntroElement>();
		if (groupId.equals("setup")) {
			var z88dks = Z88DKPreferencesAccess.get().getAllSDKs();
			if (z88dks.isEmpty()) {
				var setupZ88DK = new IntroElement("link");
				setupZ88DK.setAttribute("url",
						"http://org.eclipse.ui.intro/runAction?pluginId=uk.co.bithatch.eclipzx.ui&amp;" + "class="
								+ SetupZ88DKAction.class.getName());
				setupZ88DK.setAttribute("label", "Click to setup Z88DK location (for C programs)");
				elements.add(setupZ88DK);
			}

			if (!Python.get().isAvailable()) {
				var setupZ88DK = new IntroElement("link");
				setupZ88DK.setAttribute("url",
						"http://org.eclipse.ui.intro/runAction?pluginId=uk.co.bithatch.eclipzx.ui&amp;" + "class="
								+ SetupPythonAction.class.getName());
				setupZ88DK.setAttribute("label", "Click to setup Python location (for Boriel Basic SDK)");
				elements.add(setupZ88DK);
			}

			if (ContributedSDKRegistry.getAllSDKs().isEmpty()) {
				var setupZ88DK = new IntroElement("link");
				setupZ88DK.setAttribute("url",
						"http://org.eclipse.ui.intro/runAction?pluginId=uk.co.bithatch.eclipzx.ui&amp;" + "class="
								+ SetupZXBasicSDKAction.class.getName());
				setupZ88DK.setAttribute("label", "Click to setup Boriels ZX Basic SDK location");
				elements.add(setupZ88DK);
			}

			if (!elements.isEmpty()) {
				var setupText = new IntroElement("text");
				setupText.setAttribute("style-id", "h1");
				setupText.setValue("Setup Tasks");
				elements.add(0, setupText);

			}
		} else if (groupId.equals("footer")) {

			var setupText = new IntroElement("text");
			setupText.setAttribute("style-id", "footer");
			setupText.setValue("EclipZX version: " + getVersion());
			elements.add(0, setupText);
		}

		return elements.toArray(new IntroElement[0]);
	}

	@Override
	public String resolvePath(String extensionId, String path) {
		return null;
	}

	private String getVersion() {
		var cl = EclipZXIntroConfigurer.class.getClassLoader();
		try {
			var url = cl.getResource("META-INF/MANIFEST.MF");
			try (var in = url.openStream()) {
				var manifest = new Manifest();
				var mainAttributes = manifest.getMainAttributes();
				var implVersion = mainAttributes.getValue("Bundle-Version");
				if (implVersion != null)
					return implVersion;
			}
		} catch (Exception E) {
			// handle
		}
		return "0.0.0";
	}
}
