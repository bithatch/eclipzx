package uk.co.bithatch.emuzx.ui;

import static java.util.Collections.unmodifiableList;
import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import uk.co.bithatch.emuzx.EmulatorRegistry;
import uk.co.bithatch.emuzx.PreferenceConstants;
import uk.co.bithatch.emuzx.PreferencesAccess;
import uk.co.bithatch.emuzx.api.EmulatorDescriptor;

public class EmulatorSelectorDialog extends ListDialog implements ISelectionChangedListener {

	public final static List<File> systemPaths = unmodifiableList(systemPaths());
	public final static List<File> pathsToSearch = unmodifiableList(pathsToSearch());

	private Text emulatorExecutable;
	private Button browseEmulatorPath;
	private Text emulatorHome;
	private Button browseEmulatorHome;
	private ControlDecoration emulatorPathDecoration;
	private ControlDecoration emulatorHomeDecoration;
	private File emulatorExecutableFile;
	private File emulatorHomeFile;
	private EmulatorDescriptor selectedEmulator;

	public EmulatorSelectorDialog(Shell parent) {
		super(parent);
		setInput(EmulatorRegistry.descriptors());
		setLabelProvider(new EmulatorLabelProvider());
		setTitle("Choose Supported Emulator");
		setMessage("Select a supported Emulator. The launch configuration will be populated\n"
				+ "with recommended initial configuration.");
		setContentProvider(ArrayContentProvider.getInstance());
	}

	@Override
	protected Control createDialogArea(Composite container) {
		var ctrl = super.createDialogArea(container);

		var details = new Composite(container, SWT.NONE);
		var layout = new GridLayout(3, false);
		layout.verticalSpacing = 8;
		layout.horizontalSpacing = 16;
		details.setLayout(layout);
		details.setLayoutData(fillDefaults().grab(true, false).create());

		var lbl = new Label(details, SWT.NONE);
		lbl.setText("Emulator Executable:");

		emulatorExecutable = new Text(details, SWT.NONE);
		emulatorExecutable.setLayoutData(fillDefaults().grab(true, false).hint(300, SWT.DEFAULT).create());
		emulatorExecutable.setToolTipText("The full path to the emulator executable.");
		emulatorExecutable.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				pathsChanged();
			}
		});

		emulatorPathDecoration = new ControlDecoration(emulatorExecutable, SWT.LEFT | SWT.TOP);
		emulatorPathDecoration.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		emulatorPathDecoration.setDescriptionText("Emulator path does not exist.");
		emulatorPathDecoration.hide();

		browseEmulatorPath = new Button(details, SWT.PUSH);
		browseEmulatorPath.setText("..");
		browseEmulatorPath.setLayoutData(swtDefaults().create());
		browseEmulatorPath.addSelectionListener(widgetSelectedAdapter(e -> {
			var dialog = new FileDialog(getShell(), SWT.OPEN);
			var result = dialog.open();
			if (result != null) {
				emulatorExecutable.setText(result);
			}
		}));

		lbl = new Label(details, SWT.NONE);
		lbl.setText("Emulator Home:");

		emulatorHome = new Text(details, SWT.NONE);
		emulatorHome.setLayoutData(fillDefaults().grab(true, false).hint(300, SWT.DEFAULT).create());
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
		emulatorHomeError();
		emulatorHomeDecoration.hide();

		browseEmulatorHome = new Button(details, SWT.PUSH);
		browseEmulatorHome.setText("..");
		browseEmulatorHome.setLayoutData(swtDefaults().create());
		browseEmulatorHome.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var dialog = new DirectoryDialog(getShell(), SWT.OPEN);
			var result = dialog.open();
			if (result != null) {
				emulatorHome.setText(result);
			}
		}));

		pathsChanged();
		getTableViewer().addSelectionChangedListener(this);

		return ctrl;
	}

	public File getEmulatorExecutable() {
		return emulatorExecutableFile;
	}

	protected void emulatorHomeError() {
		emulatorHomeDecoration
				.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
		emulatorHomeDecoration.setDescriptionText("Emulator home does not exist.");
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		pathsChanged();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		selectedEmulator = calcSelectedEmulator();
		if (selectedEmulator != null) {
			
			var defEmuExe = PreferencesAccess.get().getPreferences().get(selectedEmulator.getIdOrDefault(Activator.PLUGIN_ID) + "." + PreferenceConstants.EXTERNAL_EMULATOR_EXECUTABLE, "");
			var defEmuHome = PreferencesAccess.get().getPreferences().get(selectedEmulator.getIdOrDefault(Activator.PLUGIN_ID) + "." + PreferenceConstants.EXTERNAL_EMULATOR_HOME, "");
			
			if(defEmuExe.equals("") || defEmuHome.equals("")) {
				File found = null;
	
				for (var path : pathsToSearch) {
					var name = selectedEmulator.getExecutable();
					if (Platform.getOS().equals(Platform.OS_WIN32)) {
						name += ".exe";
					}
					var fpath = new File(path, name);
					if (fpath.exists()) {
						found = fpath;
					}
				}
	
				if (found != null) {
					defEmuExe = found.toString();
					defEmuHome = found.getParent();
				} 
			}

			emulatorExecutable.setText(defEmuExe);
			emulatorHome.setText(defEmuHome);
		}

	}
	
	public EmulatorDescriptor getSelectedEmulator() {
		return selectedEmulator;
	}

	private EmulatorDescriptor calcSelectedEmulator() {
		var isel = getTableViewer().getSelection();
		if (isel instanceof StructuredSelection sel)
			return (EmulatorDescriptor) sel.getFirstElement();
		else
			return null;
	}

	public File getEmulatorHome() {
		return emulatorHomeFile;
	}

	private void pathsChanged() {
		emulatorExecutableFile = new File(emulatorExecutable.getText());
		var exeOk = emulatorExecutableFile.exists() && emulatorExecutableFile.isFile();
		if (exeOk) {
			emulatorPathDecoration.hide();
		} else {
			emulatorPathDecoration.show();
		}

		emulatorHomeFile = new File(emulatorHome.getText());
		var homeOk = emulatorHomeFile.exists() && emulatorHomeFile.isDirectory();
		if (homeOk) {
			if(systemPaths.stream().map(File::toString).toList().contains(emulatorHomeFile.toString())) {
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

		var okButton = getOkButton();
		if(okButton != null)
			okButton.setEnabled(homeOk && exeOk);
	}

	private static List<File> systemPaths() {
		var lst = new ArrayList<File>();
		var pth = System.getenv("PATH");
		if (pth != null && pth.length() > 0) {
			lst.addAll(Arrays.asList(pth.split(Pattern.quote(File.pathSeparator))).stream().map(File::new).toList());
		}
		return lst;
	}

	private static List<File> pathsToSearch() {
		var lst = new ArrayList<File>(systemPaths);
		addAllIfExists(lst, "C:\\Program Files");
		addAllIfExists(lst, "C:\\Program Files (x86)");
		addAllIfExists(lst, System.getProperty("user.home"));
		addAllIfExists(lst, System.getProperty("user.home") + File.separator + "Applications");
		return lst;
	}

	private static void addAllIfExists(List<File> lst, String path) {
		var fl = new File(path);
		if (fl.exists() && fl.isDirectory()) {
			try {
				lst.addAll(Arrays.asList(fl.listFiles()).stream().filter(File::isDirectory).toList());
			} catch (Exception ioe) {
			}
		}
	}
}
