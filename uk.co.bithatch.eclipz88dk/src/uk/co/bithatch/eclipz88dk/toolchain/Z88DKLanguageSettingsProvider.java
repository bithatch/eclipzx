package uk.co.bithatch.eclipz88dk.toolchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsEditableProvider;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsSerializableProvider;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IResource;

import uk.co.bithatch.eclipz88dk.preferences.PreferenceConstants;
import uk.co.bithatch.eclipz88dk.preferences.Z88DKPreferencesAccess;
import uk.co.bithatch.eclipz88dk.toolchain.Z88DKConfigurationFile.Key;

public class Z88DKLanguageSettingsProvider extends LanguageSettingsSerializableProvider implements ILanguageSettingsEditableProvider {
	@Override
	public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
			String languageId) {
		var pax = Z88DKPreferencesAccess.get();
		var sdks = pax.getPathListPreference(null, PreferenceConstants.SDK_PATHS);
		if (sdks.isEmpty())
			throw new IllegalStateException("No Z88DK home!");

		var project = rc.getProject();
		var sdkOr = pax.getSDK(project);
		if (sdkOr.isPresent()) {
			var sdk = sdkOr.get();
			var config = sdk.configurations().configuration(pax.getSystem(project)).get();
			var entries = new ArrayList<ICLanguageSettingEntry>();
			
			/* Default OPTION */
			config.getValue(Key.OPTIONS).ifPresent(entry -> {
				for(var opt : entry.options()) {
					addOptions(sdk, entries, opt);
				}
			});
			
			/* CLIB options */
			var entry = config.cLibraryFor(pax.getCLibrary(project));
			if (entry.isPresent()) {
				for (var opt : entry.get().options()) {
					addOptions(sdk, entries, opt);
				}
			}

			return entries;
		}
		return Collections.emptyList();
	}

	protected void addOptions(Z88DKSDK sdk, ArrayList<ICLanguageSettingEntry> entries, String opt) {
		if (opt.startsWith("-D")) {
			var mtext = opt.substring(2);
			var idx = mtext.indexOf('=');
			var name = idx == -1 ? mtext : mtext.substring(0, idx);
			var value = idx == -1 ? "" : mtext.substring(idx + 1);
			entries.add( CDataUtil.createCMacroEntry(name, value, ICSettingEntry.BUILTIN | ICSettingEntry.READONLY));
		}

		if (opt.startsWith("-isystem")) {
			var path = opt.substring(8).replace("DESTDIR", sdk.location().getAbsolutePath());
			entries.add( CDataUtil.createCIncludePathEntry(path, ICSettingEntry.READONLY | ICSettingEntry.BUILTIN));
		}

		if (opt.startsWith("-L")) {
			var path = opt.substring(2).replace("DESTDIR", sdk.location().getAbsolutePath());
			entries.add(CDataUtil.createCLibraryPathEntry(path, ICSettingEntry.READONLY | ICSettingEntry.BUILTIN));
		}

		if (opt.startsWith("-l")) {
			var name = opt.substring(2);
			entries.add(CDataUtil.createCLibraryFileEntry(name, ICSettingEntry.READONLY | ICSettingEntry.BUILTIN));
		}

		if (opt.startsWith("-crt0=")) {
			var path = opt.substring(6).replace("DESTDIR", sdk.location().getAbsolutePath());
			entries.add(CDataUtil.createCMacroFileEntry(path, ICSettingEntry.READONLY | ICSettingEntry.BUILTIN));
		}
	}

	@Override
	public Z88DKLanguageSettingsProvider cloneShallow() throws CloneNotSupportedException {
		Z88DKLanguageSettingsProvider clone = (Z88DKLanguageSettingsProvider) super.cloneShallow();
//		clone.setProperty(ATTR_CDB_MODIFIED_TIME, null);
		return clone;
	}

	@Override
	public Z88DKLanguageSettingsProvider clone() throws CloneNotSupportedException {
		return (Z88DKLanguageSettingsProvider) super.clone();
	}

//	@Override
//	public LanguageSettingsStorage copyStorage() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public void setSettingEntries(ICConfigurationDescription cfgDescription, IResource rc, String languageId,
//			List<? extends ICLanguageSettingEntry> entries) {
//		// TODO Auto-generated method stub
//		
//	}

//	@Override
//	public ILanguageSettingsEditableProvider cloneShallow() throws CloneNotSupportedException {
//		// TODO Auto-generated method stub
//		return null;
//	}
}
