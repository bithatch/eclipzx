package uk.co.bithatch.emuzx.ui;

import static org.eclipse.jface.layout.GridDataFactory.defaultsFor;
import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.jface.resource.JFaceResources.getFontRegistry;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static uk.co.bithatch.emuzx.PreferenceConstants.EXTERNAL_EMULATOR_EXECUTABLE;
import static uk.co.bithatch.emuzx.PreferenceConstants.EXTERNAL_EMULATOR_HOME;
import static uk.co.bithatch.emuzx.PreferenceConstants.EXTERNAL_EMULATOR_LEADING_OPTIONS;
import static uk.co.bithatch.emuzx.PreferenceConstants.EXTERNAL_EMULATOR_TRAILING_OPTIONS;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import uk.co.bithatch.emuzx.EmulatorRegistry;
import uk.co.bithatch.emuzx.PreferencesAccess;
import uk.co.bithatch.emuzx.api.EmulatorDescriptor;

public class ExternalEmulatorsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, ISelectionChangedListener {
    

	private TableViewer emulators;
	private Text emulatorExecutable;
	private ControlDecoration emulatorPathDecoration;
	private Button browseEmulatorPath;
	private Text emulatorHome;
	private ControlDecoration emulatorHomeDecoration;
	private Button browseEmulatorHome;
	private File emulatorExecutableFile;
	private File emulatorHomeFile;
	private Label emulatorExecutableLbl;
	private Label emulatorHomeLbl;
	private Label leadingOptionsLbl;
	private Label trailingOptionsLbl;
	private Text leadingOptions;
	private Text trailingOptions;
	private Map<String, String> tempVals = new HashMap<>();
	private Map<String, File> emulatorLocationCache = new HashMap<>();
	private EmulatorDescriptor selectedEmulator;

	public ExternalEmulatorsPreferencePage() {
        setDescription("This configures the defaults for External Emulator Launching for "
        		+ "all supported emulators.");
    }
	
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(PreferencesAccess.get().getPreferenceStore());
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if(selectedEmulator != null) {
			storeCurrent();
		}
		emulatorChanged();
	}

	@Override
	protected Control createContents(Composite parent) {
		
		var container = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(3, false);
		var descriptors = EmulatorRegistry.descriptors();
		
		layout.verticalSpacing = 8;
		layout.horizontalSpacing = 24;
		container.setLayout(layout);

		emulators = new TableViewer(container, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

		
		emulators.setContentProvider(ArrayContentProvider.getInstance());
		emulators.setLabelProvider(new EmulatorLabelProvider());
		emulators.setInput(descriptors);
		emulators.addDoubleClickListener(event -> {
//			if (fAddCancelButton) {
//				okPressed();
//			}
		});
		emulators.addSelectionChangedListener(this);
		emulators.getControl().setLayoutData(fillDefaults().
				grab(true, false).
				span(3, 1).
				hint(SWT.DEFAULT, 120).
				create());
		

		emulatorExecutableLbl = new Label(container, SWT.NONE);
		emulatorExecutableLbl.setText("Emulator Executable:");

		emulatorExecutable = new Text(container, SWT.NONE);
		emulatorExecutable.setLayoutData(fillDefaults().grab(true, false).create());
		emulatorExecutable.setToolTipText("The full path to the emulator executable.");
		emulatorExecutable.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				pathsChanged();
			}
		});

		emulatorPathDecoration = new ControlDecoration(emulatorExecutable, SWT.LEFT | SWT.TOP);
		emulatorPathDecoration
				.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		emulatorPathDecoration.setDescriptionText("Emulator path does not exist.");
		emulatorPathDecoration.hide();

		browseEmulatorPath = new Button(container, SWT.PUSH);
		browseEmulatorPath.setText("Browse");
		browseEmulatorPath.setLayoutData(defaultsFor(browseEmulatorPath).create());
		browseEmulatorPath.addSelectionListener(widgetSelectedAdapter(e -> {
			var dialog = new FileDialog(getShell(), SWT.OPEN);
			var result = dialog.open();
			if (result != null) {
				emulatorExecutable.setText(result);
			}
		}));

		emulatorHomeLbl = new Label(container, SWT.NONE);
		emulatorHomeLbl.setText("Emulator Home:");

		emulatorHome = new Text(container, SWT.NONE);
		emulatorHome.setLayoutData(fillDefaults().grab(true, false).create());
		emulatorHome.setToolTipText("The full path to the emulator home, i.e. the root of where\n"
				+ " other data files it uses are. Depending on the emulator or\n"
				+ "how it was installed, this might be in the same directory\n"
				+ "as the executable, or it might be in some other system wide\n"
				+ "data directory (e.g. /usr/share on Linux).");
		emulatorHome.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				pathsChanged();
			}
		});

		emulatorHomeDecoration = new ControlDecoration(emulatorHome, SWT.LEFT | SWT.TOP);
		emulatorHomeDecoration.hide();

		browseEmulatorHome = new Button(container, SWT.PUSH);
		browseEmulatorHome.setText("Browse");
		browseEmulatorHome.setLayoutData(defaultsFor(browseEmulatorHome).create());
		browseEmulatorHome.addSelectionListener(widgetSelectedAdapter(e -> {
			var dialog = new DirectoryDialog(getShell(), SWT.OPEN);
			var result = dialog.open();
			if (result != null) {
				emulatorHome.setText(result);
			}
		}));
		

		var infoLbl = new Label(container, SWT.WRAP);
		infoLbl.setText(
				"These options are added to begining or end of those defined in an External "
				+ "Emulator Launch configuration when this emulator is selected. One option per line.");
		infoLbl.setFont(getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));
		infoLbl.setLayoutData(fillDefaults().grab(true, false).hint(240, 32).indent(0, 24).span(3, 1).create());

		leadingOptionsLbl = new Label(container, SWT.NONE);
		leadingOptionsLbl.setText("Preceding options");
		leadingOptionsLbl.setLayoutData(fillDefaults().grab(true, false).indent(0, 24).span(3, 1).create());
		
		leadingOptions = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		leadingOptions.setLayoutData(fillDefaults().grab(true, true).span(3, 1).create());

		trailingOptionsLbl = new Label(container, SWT.NONE);
		trailingOptionsLbl.setText("Trailing options");
		trailingOptionsLbl.setLayoutData(fillDefaults().grab(true, false).indent(0, 24).span(3, 1).create());
		
		trailingOptions = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		trailingOptions.setLayoutData(fillDefaults().grab(true, true).span(3, 1).create());

		if(descriptors.size() > 0)
			emulators.setSelection(new StructuredSelection(descriptors.get(0)));
		else
			emulatorChanged();
		
		return container;
	}

	@Override
	protected void performDefaults() {
		emulatorExecutable.setText("");
		emulatorHome.setText("");
		leadingOptions.setText("");
		trailingOptions.setText("");
		tempVals.clear();
		emulatorChanged();
//		IPreferenceStore store = PrefUtil.getInternalPreferenceStore();
//		store.setToDefault(IPreferenceConstants.PLUGINS_NOT_ACTIVATED_ON_STARTUP);
//		updateCheckState();
//		windowsDefenderIgnore.values().forEach(b -> b.setSelection(PREFERENCE_STARTUP_CHECK_SKIP_DEFAULT));
//		updateWindowsDefenderHandlingOptions();
	}

	@Override
	public void performApply() {
		super.performApply();
	}

	@Override
	public boolean performOk() {
		storeCurrent();
		var prefs = getPreferenceStore();
		for(var tempEn : tempVals.entrySet()) {
			prefs.setValue(tempEn.getKey(), tempEn.getValue());
		}
		return super.performOk();
	}

	private void emulatorChanged() {
		
		var sel = (IStructuredSelection)emulators.getSelection();
		selectedEmulator = sel == null ? null : (EmulatorDescriptor)sel.getFirstElement();
		
		var haveEm = selectedEmulator != null;

		emulatorHome.setEnabled(haveEm);
		emulatorExecutable.setEnabled(haveEm);
		leadingOptions.setEnabled(haveEm);
		trailingOptions.setEnabled(haveEm);
		emulatorExecutableLbl.setEnabled(haveEm);
		emulatorHomeLbl.setEnabled(haveEm);
		
		
		if(haveEm) {
			emulatorExecutable.setText(tempVals.computeIfAbsent(selectedEmulator.getIdOrDefault(Activator.PLUGIN_ID) + "." + EXTERNAL_EMULATOR_EXECUTABLE, (k) -> { 
				var str = getPreferenceStore().getString(k);
				if(str.equals("")) {
					var found = findEmulator();
					if(found != null) {
						str = found.toString();
					}
				}
				return str;
			}));
			emulatorHome.setText(tempVals.computeIfAbsent(selectedEmulator.getIdOrDefault(Activator.PLUGIN_ID) + "." + EXTERNAL_EMULATOR_HOME, (k) -> {
				var str = getPreferenceStore().getString(k);
				if(str.equals("")) {
					var found = findEmulator();
					if(found != null) {
						str = found.getParent().toString();
					}
				}
				return str; 
			}));
			leadingOptions.setText(tempVals.computeIfAbsent(selectedEmulator.getIdOrDefault(Activator.PLUGIN_ID) + "." + EXTERNAL_EMULATOR_LEADING_OPTIONS, (k) -> getPreferenceStore().getString(k)));
			trailingOptions.setText(tempVals.computeIfAbsent(selectedEmulator.getIdOrDefault(Activator.PLUGIN_ID) + "." + EXTERNAL_EMULATOR_TRAILING_OPTIONS, (k) -> getPreferenceStore().getString(k)));
		}
		else {
			emulatorHome.setText("");
			emulatorExecutable.setText("");
			leadingOptions.setText("");
			trailingOptions.setText("");
		}
	}
	
	private File findEmulator() {
		return emulatorLocationCache.computeIfAbsent(selectedEmulator.getIdOrDefault(Activator.PLUGIN_ID), k -> {
			File found = null;

			for (var path : EmulatorSelectorDialog.pathsToSearch) {
				var name = selectedEmulator.getExecutable();
				if (Platform.getOS().equals(Platform.OS_WIN32)) {
					name += ".exe";
				}
				var fpath = new File(path, name);
				if (fpath.exists()) {
					found = fpath;
				}
			}
			
			return found;	
		});
	}

	private void storeCurrent() {
		var key = selectedEmulator.getIdOrDefault(Activator.PLUGIN_ID);
		tempVals.put(key + "." + EXTERNAL_EMULATOR_HOME, emulatorHome.getText());
		tempVals.put(key + "." + EXTERNAL_EMULATOR_EXECUTABLE, emulatorExecutable.getText());
		tempVals.put(key + "." + EXTERNAL_EMULATOR_LEADING_OPTIONS, leadingOptions.getText());
		tempVals.put(key + "." + EXTERNAL_EMULATOR_TRAILING_OPTIONS, trailingOptions.getText());
	}

	private void pathsChanged() {
		emulatorExecutableFile = new File(emulatorExecutable.getText());
		var exeOk = emulatorExecutable.getText().equals("") || ( emulatorExecutableFile.exists() && emulatorExecutableFile.isFile() );
		if (exeOk) {
			emulatorPathDecoration.hide();
		} else {
			emulatorPathDecoration.show();
		}

		emulatorHomeFile = new File(emulatorHome.getText());
		var homeOk = emulatorHome.getText().equals("") || ( emulatorHomeFile.exists() && emulatorHomeFile.isDirectory() );
		if (homeOk) {
			if(EmulatorSelectorDialog.systemPaths.stream().map(File::toString).toList().contains(emulatorHomeFile.toString())) {
				emulatorHomeDecoration
						.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
				emulatorHomeDecoration.setDescriptionText("This is a sytem path, the emulator home is probably not here.");
				emulatorHomeDecoration.show();
			}
			else {
				emulatorHomeDecoration.hide();
			}
		} else {
			emulatorHomeError();
			emulatorHomeDecoration.show();
		}

		setValid(homeOk && exeOk);
	}
	
	private void emulatorHomeError() {
		emulatorHomeDecoration
				.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		emulatorHomeDecoration.setDescriptionText("Emulator home does not exist.");
	}

}
