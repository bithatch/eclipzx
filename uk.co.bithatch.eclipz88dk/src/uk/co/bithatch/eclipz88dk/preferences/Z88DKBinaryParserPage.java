package uk.co.bithatch.eclipz88dk.preferences;

import java.io.File;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigExtensionReference;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.dialogs.AbstractCOptionPage;
import org.eclipse.cdt.utils.ui.controls.ControlFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Binary parser options shown in CDT when Z88DK parser is selected.
 */
public class Z88DKBinaryParserPage extends AbstractCOptionPage {

	private static final String EXT_NM = "nm";
	private static final String EXT_NM_ARGS = "nmArgs";
	private static final String DEFAULT_NM = "z88dk-z80nm";
	private static final String DEFAULT_NM_ARGS = "-a";
	private static final String GROUP_TITLE = "Binary Parser Options";
	private static final String LABEL_NM = "z88dk-z80nm command:";
	private static final String LABEL_NM_ARGS = "z88dk-z80nm args:";
	private static final String LABEL_BROWSE = "Browse...";

	private Text nmCommandText;
	private Text nmArgsText;

	@Override
	public void createControl(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
		group.setText(GROUP_TITLE);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label nmLabel = ControlFactory.createLabel(group, LABEL_NM);
		nmLabel.setLayoutData(span2());
		nmCommandText = ControlFactory.createTextField(group, SWT.SINGLE | SWT.BORDER);
		nmCommandText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button browse = ControlFactory.createPushButton(group, LABEL_BROWSE);
		browse.addListener(SWT.Selection, e -> browseForCommand());

		Label argsLabel = ControlFactory.createLabel(group, LABEL_NM_ARGS);
		argsLabel.setLayoutData(span2());
		nmArgsText = ControlFactory.createTextField(group, SWT.SINGLE | SWT.BORDER);
		nmArgsText.setLayoutData(span2Fill());

		setControl(group);
		initializeValues();
	}

	@Override
	public void performApply(IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		var nm = nmCommandText.getText().trim();
		var nmArgs = nmArgsText.getText().trim();

		var project = getContainer().getProject();
		if (project != null) {
			var cext = getParserExtensionRefs(project);
			var parserId = parserIdForThisPage();
			for (var ref : cext) {
				if (ref.getID().equals(parserId)) {
					setIfChanged(ref, EXT_NM, nm);
					setIfChanged(ref, EXT_NM_ARGS, nmArgs);
				}
			}
			var desc = CCorePlugin.getDefault().getProjectDescription(project, true);
			if (desc != null) {
				CCorePlugin.getDefault().setProjectDescription(project, desc);
			}
		} else {
			var prefs = getContainer().getPreferences();
			if (prefs != null) {
				prefs.setValue(CUIPlugin.PLUGIN_ID + "." + EXT_NM, nm);
				prefs.setValue(CUIPlugin.PLUGIN_ID + "." + EXT_NM_ARGS, nmArgs);
			}
		}
	}

	@Override
	public void performDefaults() {
		nmCommandText.setText(DEFAULT_NM);
		nmArgsText.setText(DEFAULT_NM_ARGS);
	}

	private void initializeValues() {
		var project = getContainer().getProject();
		if (project != null) {
			for (var ref : getParserExtensionRefs(project)) {
				if (ref.getID().equals(parserIdForThisPage())) {
					nmCommandText.setText(defaultIfBlank(ref.getExtensionData(EXT_NM), DEFAULT_NM));
					nmArgsText.setText(defaultIfBlank(ref.getExtensionData(EXT_NM_ARGS), DEFAULT_NM_ARGS));
					return;
				}
			}
		}
		Preferences prefs = getContainer().getPreferences();
		nmCommandText.setText(defaultIfBlank(prefs != null ? prefs.getString(CUIPlugin.PLUGIN_ID + "." + EXT_NM) : null, DEFAULT_NM));
		nmArgsText.setText(defaultIfBlank(prefs != null ? prefs.getString(CUIPlugin.PLUGIN_ID + "." + EXT_NM_ARGS) : null, DEFAULT_NM_ARGS));
	}

	private static String defaultIfBlank(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private static void setIfChanged(ICConfigExtensionReference ref, String key, String value) throws CoreException {
		var old = ref.getExtensionData(key);
		if (old == null || !old.equals(value)) {
			ref.setExtensionData(key, value);
		}
	}

	private ICConfigExtensionReference[] getParserExtensionRefs(IProject project) {
		ICProjectDescription desc = CCorePlugin.getDefault().getProjectDescription(project, true);
		if (desc == null) {
			return new ICConfigExtensionReference[0];
		}
		ICConfigurationDescription cfg = desc.getDefaultSettingConfiguration();
		if (cfg == null) {
			return new ICConfigExtensionReference[0];
		}
		ICConfigExtensionReference[] refs = cfg.get(CCorePlugin.BINARY_PARSER_UNIQ_ID);
		return refs != null ? refs : new ICConfigExtensionReference[0];
	}

	private String parserIdForThisPage() {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(CUIPlugin.PLUGIN_ID, "BinaryParserPage");
		if (point == null) {
			return "";
		}
		for (IConfigurationElement element : point.getConfigurationElements()) {
			String clazz = element.getAttribute("class");
			if (getClass().getName().equals(clazz)) {
				return defaultIfBlank(element.getAttribute("parserID"), "");
			}
		}
		return "";
	}

	private void browseForCommand() {
		FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
		dialog.setText("Select z88dk-z80nm");
		String command = nmCommandText.getText().trim();
		int sep = command.lastIndexOf(File.separator);
		if (sep != -1) {
			dialog.setFilterPath(command.substring(0, sep));
		}
		String result = dialog.open();
		if (result != null) {
			nmCommandText.setText(result);
		}
	}

	private static GridData span2() {
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		return gd;
	}

	private static GridData span2Fill() {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		return gd;
	}
}
