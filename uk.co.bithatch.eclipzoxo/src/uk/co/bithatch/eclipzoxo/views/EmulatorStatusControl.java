package uk.co.bithatch.eclipzoxo.views;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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

import uk.co.bithatch.eclipzoxo.Activator;
import uk.co.bithatch.eclipzoxo.editor.TapeBrowser;
import uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferencesAccess;
import uk.co.bithatch.zoxo.interface1.Interface1AddOn;

public class EmulatorStatusControl extends WorkbenchWindowControlContribution
		implements IPartListener2, IPropertyChangeListener {
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
		ZoxoPreferencesAccess.get().getPreferenceStore().addPropertyChangeListener(this);

		container = new Composite(parent, SWT.NONE);
		var layout = new GridLayout(4, false);
		layout.horizontalSpacing = 8;
		container.setLayout(layout);

		statusLabel = new Label(container, SWT.NONE);
		statusLabel.setText("Zoxo");
		statusLabel.setLayoutData(GridDataFactory.fillDefaults().hint(128, SWT.DEFAULT).create());

		tapeButton = new Button(container, SWT.FLAT | SWT.PUSH);
		tapeButton.setImage(Activator.getDefault().getImageRegistry().get(Activator.TAPE_PATH));
		tapeButton.setLayoutData(GridDataFactory.swtDefaults().hint(32, 18).create());
		tapeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			try {
				IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
						getCurrentEmulatorView().getSelectedEmulator().getTapeFile().toUri(), TapeBrowser.ID, true);
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
			var em = getCurrentEmulatorView();
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
			getCurrentEmulatorView().getSelectedEmulator().ejectROM();
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
			var prevPart = getCurrentEmulatorView();
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
		ZoxoPreferencesAccess.get().getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	public void updateStatusText() {
		if (statusLabel.getDisplay().getThread() == Thread.currentThread()) {
			doUpdateStatus();
		} else {
			statusLabel.getDisplay().asyncExec(() -> {
				doUpdateStatus();
			});
		}
	}

	public void updateState() {
		if (statusLabel.getDisplay().getThread() == Thread.currentThread()) {
			doUpdateState();
			doUpdateStatus();
		} else {
			statusLabel.getDisplay().asyncExec(() -> {
				;
				updateState();
			});
		}

	}

	protected void doUpdateStatus() {
		var emPart = getCurrentEmulatorView();
		var em = emPart.getSelectedEmulator();
		String stext = em == null ? "Inactive" : em.getStatusText();
		if (!stext.equals(statusLabel.getText()))
			statusLabel.setText(stext);
	}

	protected void doUpdateState() {
		var emPart = getCurrentEmulatorView();
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

				if (ZoxoPreferencesAccess.get().isEnabled(Interface1AddOn.class)) {
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

	protected EmulatorView getCurrentEmulatorView() {
		return currentEmulatorPartRef == null ? null : (EmulatorView) currentEmulatorPartRef.getPart(false);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		doUpdateState();
	}
}
