package uk.co.bithatch.jspeccy;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import configuration.JSpeccySettings;
import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import machine.Spectrum;

public class ZXEmulatorSettings {
	
	public interface SettingsListener {
		void settingsChanged(ZXEmulatorSettings settings);
	}

	private static final String KEY_POKE_ADDRESS = "pokeAddress";
	private static final String KEY_POKE_VALUE = "pokeValue";
	private static final String KEY_BORDER_ENABLED = "borderEnabled";
	private static final String KEY_EXTEND_BORDER_ENABLED = "extendBorderEnabled";
	private static final String KEY_EXTEND_BORDER_DISABLED_DURING_LOAD = "extendBorderDisabledDuringLoad";

	private JSpeccySettings settings;
	private List<SettingsListener> settingsListeners = new ArrayList<>();

	public ZXEmulatorSettings() throws IOException {
		readSettingsFile();
		getPreferences().addPreferenceChangeListener(evt -> {
			settingsListeners.forEach(l -> l.settingsChanged(ZXEmulatorSettings.this));
		});
	}
	
	public void addListener(SettingsListener listener) {
		this.settingsListeners.add(listener);
	}
	
	public void removeListener(SettingsListener listener) {
		this.settingsListeners.remove(listener);
	}

	public JSpeccySettings jspeccy() {
		return settings;
	}
	
	public int getPokeValue() {
		return getPreferences().getInt(KEY_POKE_ADDRESS, 35899);
	}
	
	public int getPokeAddress() {
		return getPreferences().getInt(KEY_POKE_ADDRESS, 0);
	}
	
	public void setPokeValue(int value) {
		getPreferences().putInt(KEY_POKE_VALUE, value);
	}
	
	public void setPokeAddress(int address) {
		getPreferences().putInt(KEY_POKE_ADDRESS, address);
	}
	
	public boolean isBorderEnabled() {
		return getPreferences().getBoolean(KEY_BORDER_ENABLED, true);
	}
	
	public boolean isExtendBorderEnabled() {
		return getPreferences().getBoolean(KEY_EXTEND_BORDER_ENABLED, true);
	}
	
	public boolean isExtendBorderDisabledDuringLoad() {
		return getPreferences().getBoolean(KEY_EXTEND_BORDER_DISABLED_DURING_LOAD, true);
	}
	
	public void setBorderEnabled(boolean borderEnabled) {
		getPreferences().putBoolean(KEY_BORDER_ENABLED, borderEnabled);
	}
	
	public void setExtendBorderEnabled(boolean extendBorderEnabled) {
		getPreferences().putBoolean(KEY_EXTEND_BORDER_ENABLED, extendBorderEnabled);
	}
	
	public void setExtendBorderDisabledDuringLoad(boolean extendBorderDisabledDuringLoad) {
		getPreferences().putBoolean(KEY_EXTEND_BORDER_DISABLED_DURING_LOAD, extendBorderDisabledDuringLoad);
	}

	public void save() {
		var file = getConfigFile();
		file.getParentFile().mkdirs();
		var was = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		try (BufferedOutputStream fOut = new BufferedOutputStream(new FileOutputStream(file))) {
			JAXB.marshal(settings, fOut);
			settingsListeners.forEach(l -> l.settingsChanged(this));
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		} finally {
			Thread.currentThread().setContextClassLoader(was);
		}
	}

	private void verifyConfigFile(boolean deleteFile) {

		var file = getConfigFile();
		if (file.exists() && !deleteFile) {
			return;
		}

		if (deleteFile && (!file.delete())) {
			ILog.get().error("Unable to delete the corrupted JSpeccy.xml file");
		}

		file.getParentFile().mkdirs();

		InputStream input = null;
		BufferedOutputStream output = null;
		try {
			input = Spectrum.class.getResourceAsStream("/schema/JSpeccy.xml");
			output = new BufferedOutputStream(new FileOutputStream(file));

			var fileConf = new byte[input.available()];
			input.read(fileConf);
			output.write(fileConf, 0, fileConf.length);
		} catch (IOException ioException) {
			ILog.get().error("Unable to find configuration file", ioException);
		} finally {
			try {
				if (input != null) {
					input.close();
				}

				if (output != null) {
					output.close();
				}
			} catch (IOException ioException) {
				ILog.get().error("Unable to close configuration file", ioException);
			}
		}
	}

	private File getConfigFile() {
		return new File(new File(new File(new File(System.getProperty("user.home")), ".eclipzx"), "jspeccy"),
				"JSpeccy.xml");
	}

	private void readSettingsFile() throws IOException {
		verifyConfigFile(false);

		boolean wasRead = readConfig();

		if (wasRead)
			return;

		ILog.get().info("Trying to create a new JSpeccy.xml file for you");

		verifyConfigFile(true);
		if (!readConfig()) {
			ILog.get().error("Can't open the JSpeccy.xml configuration file anyway");
			throw new IOException("Give up trying to read or create emulator configuration file");
		}

	}

	private boolean readConfig() {
		boolean wasRead = true;
		try {
			var jc = JAXBContext.newInstance("configuration", getClass().getClassLoader());
			var unmsh = jc.createUnmarshaller();
			settings = (JSpeccySettings) unmsh.unmarshal(new FileInputStream(getConfigFile()));
		} catch (JAXBException e) {
			ILog.get().error("Failed to unmarshall settings", e);
			wasRead = false;
		} catch (FileNotFoundException ioexcpt) {
			ILog.get().warn("Can't find JSpeccy.xml configuration file", ioexcpt);
			wasRead = false;
		}
		return wasRead;
	}
	
	public static IPreferenceStore getPreferenceStore() {
		return new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID);
	}

	public static IEclipsePreferences getPreferences() {
		return InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
	}
}
