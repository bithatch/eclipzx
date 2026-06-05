package uk.co.bithatch.eclipzoxo.preferences;

import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_BORDER_ENABLED;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_CONFIRM_DESTRUCTIVE_ACTIONS;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_DEFAULT_MACHINE;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_ENABLED;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_EXTEND_BORDER_DISABLED_DURING_LOAD;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_EXTEND_BORDER_ENABLED;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_FAST_SPEED;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_MUTE;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_POKE_ADDRESS;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_POKE_VALUE;
import static uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants.KEY_REFRESH_RATE;

import java.util.List;

import uk.co.bithatch.bitzx.AbstractPreferencesAccess;
import uk.co.bithatch.eclipzoxo.Activator;
import uk.co.bithatch.eclipzoxo.components.ZoxoComponentRegistry;
import uk.co.bithatch.zoxo.audio.AudioSettings;
import uk.co.bithatch.zoxo.ay8912.AY8912Settings;
import uk.co.bithatch.zoxo.dezog.DeZogSettings;
import uk.co.bithatch.zoxo.interface1.Interface1Settings;
import uk.co.bithatch.zoxo.interface2.Interface2Settings;
import uk.co.bithatch.zoxo.system.AddOn;
import uk.co.bithatch.zoxo.system.Machine.MachineFactory;
import uk.co.bithatch.zoxo.system.Model;
import uk.co.bithatch.zoxo.system.Settings;
import uk.co.bithatch.zoxo.system.TapeSettings;
import uk.co.bithatch.zoxo.zxspectrum.ZXSpectrumSettings;

public class ZoxoPreferencesAccess extends AbstractPreferencesAccess {
	
	private Interface1Settings interface1Settings;
	private Interface2Settings interface2Settings;
	private TapeSettings tapeSettings;
	private AudioSettings  audioSettings;
	private AY8912Settings ay8912Settings;
	private ZXSpectrumSettings zxSpectrumSettings;
	private List<MachineFactory<?, ?>> machines = ZoxoComponentRegistry.machineFactories();
	private DeZogSettings dezogSettings;

	public final static class Defaults {
		private final static ZoxoPreferencesAccess DEFAULT = new ZoxoPreferencesAccess();
	}

	public static ZoxoPreferencesAccess get() {
		return Defaults.DEFAULT;
	}

	protected ZoxoPreferencesAccess() {
		super(Activator.PLUGIN_ID);
	}
	
	public List<MachineFactory<?, ?>> machines() {
		return machines;
	}

	public MachineFactory<?, ?> getFallbackMachine() {
		return machines.iterator().next();
	}
	
	public MachineFactory<?, ?> getDefaultModel() {
		var fbmachine = getFallbackMachine();
		var defm = getPreferences().get(KEY_DEFAULT_MACHINE, fbmachine.model().name());
		return machines.stream().filter(f -> f.model().name().equals(defm)).findFirst().orElse(fbmachine);
	}
	
	public Model getFallbackModelName() {
		return getFallbackMachine().model();
	}
	
	public int getPokeAddress() {
		return getPreferences().getInt(KEY_POKE_ADDRESS, 35899);
	}
	
	public int getPokeValue() {
		return getPreferences().getInt(KEY_POKE_VALUE, 0);
	}
	
	public int getFastSpeed() {
		return getPreferences().getInt(KEY_FAST_SPEED, 2);
	}
	
	public boolean isBorderEnabled() {
		return getPreferences().getBoolean(KEY_BORDER_ENABLED, true);
	}
	
	public boolean isExtendBorderDisabledDuringLoad() {
		return getPreferences().getBoolean(KEY_EXTEND_BORDER_DISABLED_DURING_LOAD, true);
	}
	
	public boolean isExtendBorderEnabled() {
		return getPreferences().getBoolean(KEY_EXTEND_BORDER_ENABLED, true);
	}
	
	public boolean isMuted() {
		return getPreferences().getBoolean(KEY_MUTE, true);
	}
	
	public void setMuted(boolean muted) {
		getPreferences().putBoolean(KEY_MUTE, muted);
	}
	
	public void setBorderEnabled(boolean borderEnabled) {
		getPreferences().putBoolean(KEY_BORDER_ENABLED, borderEnabled);
	}
	
	public void setExtendBorderDisabledDuringLoad(boolean extendBorderDisabledDuringLoad) {
		getPreferences().putBoolean(KEY_EXTEND_BORDER_DISABLED_DURING_LOAD, extendBorderDisabledDuringLoad);
	}
	
	public void setExtendBorderEnabled(boolean extendBorderEnabled) {
		getPreferences().putBoolean(KEY_EXTEND_BORDER_ENABLED, extendBorderEnabled);
	}

	public void setPokeAddress(int address) {
		getPreferences().putInt(KEY_POKE_ADDRESS, address);
	}

	public void setPokeValue(int value) {
		getPreferences().putInt(KEY_POKE_VALUE, value);
	}

	public void setFastSpeed(int speed) {
		getPreferences().putInt(KEY_FAST_SPEED, speed);
	}
	
	public boolean isEnabled(Class<? extends AddOn<?, ?>> type) {
		return getPreferences().getBoolean(keyForExtension(type), isBorderEnabled());
	}

	public String keyForExtension(Class<? extends AddOn<?, ?>> type) {
		return KEY_ENABLED + "." + type.getName();
	}
	
	public void setEnabled(Class<? extends AddOn<?, ?>> type, boolean enabled) {
		getPreferences().putBoolean(KEY_ENABLED, enabled);
	}
	
	public void setDefaultMachine(MachineFactory<?, ?> machine) {
		getPreferences().put(KEY_DEFAULT_MACHINE, machine.model().name());
	}

	public boolean isConfirmDestructiveActions() {
		return getPreferences().getBoolean(KEY_CONFIRM_DESTRUCTIVE_ACTIONS, true);
	}

	public void setConfirmDestructiveActions(boolean confirmDestructiveActions) {
		getPreferences().putBoolean(KEY_CONFIRM_DESTRUCTIVE_ACTIONS, confirmDestructiveActions);
	}

	public int getRefreshRate() {
		return getPreferences().getInt(KEY_REFRESH_RATE, 50);
	}

	public void setRefreshRate(int refreshRate) {
		getPreferences().putInt(KEY_REFRESH_RATE, refreshRate);
	}
	
	@SuppressWarnings("unchecked")
	public <S extends Settings> S settings(Class<S> clazz) {
		if(clazz.equals(ZXSpectrumSettings.class)) {
			if(zxSpectrumSettings == null) {
				zxSpectrumSettings = new ZXSpectrumSettingsImpl();
			}
			return (S)zxSpectrumSettings;
		}
		else if(clazz.equals(Interface1Settings.class)) {
			if(interface1Settings == null) {
				interface1Settings = new Interface1SettingsImpl();
			}
			return (S)interface1Settings;
		}
		else if(clazz.equals(Interface2Settings.class)) {
			if(interface2Settings == null) {
				interface2Settings = new Interface2SettingsImpl();
			}
			return (S)interface2Settings;
		}
		else if(clazz.equals(TapeSettings.class)) {
			if(tapeSettings == null) {
				tapeSettings = new TapeSettingsImpl();
			}
			return (S)tapeSettings;
		}
		else if(clazz.equals(AudioSettings.class)) {
			if(audioSettings == null) {
				audioSettings = new AudioSettingsImpl();
			}
			return (S)audioSettings;
		}
		else if(clazz.equals(AY8912Settings.class)) {
			if(ay8912Settings == null) {
				ay8912Settings = new AY8912SettingsImpl();
			}
			return (S)ay8912Settings;
		}
		else if(clazz.equals(DeZogSettings.class)) {
			if(dezogSettings == null) {
				dezogSettings = new DeZogSettingsImpl();
			}
			return (S)dezogSettings;
		}
		else {
			return null;
		}
	}
}
