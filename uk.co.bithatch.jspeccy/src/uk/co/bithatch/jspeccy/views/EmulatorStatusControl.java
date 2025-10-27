package uk.co.bithatch.jspeccy.views;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import uk.co.bithatch.jspeccy.Activator;
import uk.co.bithatch.jspeccy.ZXEmulatorSettings;
import uk.co.bithatch.jspeccy.ZXEmulatorSettings.SettingsListener;
import uk.co.bithatch.jspeccy.editor.TapeBrowser;

public class EmulatorStatusControl extends WorkbenchWindowControlContribution
		implements IPartListener2, SettingsListener {
	public final static ILog LOG = ILog.of(EmulatorStatusControl.class);

	private Label statusLabel;
	private Button tapeButton;
	private Button microdriveButton;
	private IWorkbenchPartReference currentEmulatorPartRef;
	private Composite container;

	private Button romButton;

	private GridData romLayoutData;
	private GridData microdriveLayoutData;

	public EmulatorStatusControl() {
		super();
	}

	public EmulatorStatusControl(String id) {
		super(id);
	}

	@Override
	protected Control createControl(Composite parent) {
		Activator.getDefault().settings().addListener(this);

		container = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		statusLabel = new Label(container, SWT.NONE);
		statusLabel.setText("JSpeccy");
		statusLabel.setLayoutData(GridDataFactory.fillDefaults().hint(128, SWT.DEFAULT).create());

		tapeButton = new Button(container, SWT.FLAT | SWT.PUSH);
		tapeButton.setImage(Activator.getDefault().getImageRegistry().get(Activator.TAPE_PATH));
		tapeButton.setLayoutData(GridDataFactory.swtDefaults().hint(32, 18).create());
		tapeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			try {
				IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
						getCurrentEmulator().getSelectedEmulator().getTapeFile().toURI(), TapeBrowser.ID, true);
			} catch (PartInitException e1) {
				LOG.error("Failed to open tape browser.", e1);
			}
		}));
		tapeButton.setToolTipText("Click to open in tape browser.");

		microdriveButton = new Button(container, SWT.FLAT | SWT.PUSH);
		microdriveButton.setImage(Activator.getDefault().getImageRegistry().get(Activator.MICRODRIVE_PATH));
		microdriveLayoutData = GridDataFactory.swtDefaults().hint(32, 18).create();
		microdriveButton.setLayoutData(microdriveLayoutData);
		microdriveButton.setToolTipText("Click to manage microdrives.");
		microdriveButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
//			afx(afb.get(afb.indexOf(model.afx()) - 1));
			var em = getCurrentEmulator();
			if (em != null) {
				em.getSelectedEmulator().microdrives();
			}
		}));

		romButton = new Button(container, SWT.FLAT | SWT.PUSH);
		romButton.setImage(Activator.getDefault().getImageRegistry().get(Activator.ROM_PATH));
		romLayoutData = GridDataFactory.swtDefaults().hint(32, 18).create();
		romButton.setLayoutData(romLayoutData);
		romButton.setToolTipText("Click to eject ROM.");
		romButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			getCurrentEmulator().getSelectedEmulator().ejectROM();
		}));

		var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		var viewRef = page.findViewReference(EmulatorView.ID);
		if (viewRef != null) {
			currentEmulatorPartRef = viewRef;
			var pt = (EmulatorView) currentEmulatorPartRef.getPart(false);
			if (pt != null) {
				pt.setStatusControl(this);
			}
		}

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(this);

		doUpdateState();
		return container;
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(EmulatorView.ID)) {
			var part = (EmulatorView) partRef.getPart(false);
			if (part != null) {
				removeFromPreviousEmulator();
				currentEmulatorPartRef = partRef;
				(part).setStatusControl(this);
				doUpdateState();
			}
		}
	}

	protected void removeFromPreviousEmulator() {
		if (currentEmulatorPartRef != null) {
			var prevPart = getCurrentEmulator();
			if (prevPart != null) {
				prevPart.setStatusControl(null);
			}
			currentEmulatorPartRef = null;
			doUpdateState();
		}
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		if (partRef.equals(currentEmulatorPartRef)) {
			removeFromPreviousEmulator();
		}
	}

	@Override
	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(this);
		Activator.getDefault().settings().removeListener(this);
		super.dispose();
	}

	public void updateStatus() {
		statusLabel.getDisplay().asyncExec(() -> {
			doUpdateStatus();
		});
	}

	public void updateState() {
		statusLabel.getDisplay().asyncExec(() -> {
			doUpdateState();
			doUpdateStatus();
		});

	}

	protected void doUpdateStatus() {
		var emPart = getCurrentEmulator();
		String stext = emPart.getSelectedEmulator().getStatusText();
		if (!stext.equals(statusLabel.getText()))
			statusLabel.setText(stext);
	}

	protected void doUpdateState() {
		var emPart = getCurrentEmulator();
		if (emPart == null) {
			container.setVisible(false);
		} else {
			var selectedEmulator = emPart.getSelectedEmulator();
			if (selectedEmulator == null) {
				container.setVisible(false);
			} else {
				container.setVisible(true);
				tapeButton.setEnabled(selectedEmulator.getTapeFile() != null);
				romButton.setEnabled(selectedEmulator.getRomFile() != null);

				if (Activator.getDefault().settings().jspeccy().getInterface1Settings().isConnectedIF1()) {
					microdriveButton.setVisible(true);
					microdriveLayoutData.exclude = false;
				} else {
					microdriveButton.setVisible(false);
					microdriveLayoutData.exclude = true;
				}
				container.layout();
			}
		}
	}

	protected EmulatorView getCurrentEmulator() {
		return currentEmulatorPartRef == null ? null : (EmulatorView) currentEmulatorPartRef.getPart(false);
	}

	@Override
	public void settingsChanged(ZXEmulatorSettings settings) {
		doUpdateState();
	}
}
