package uk.co.bithatch.eclipzoxo.views;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IEvaluationService;

import uk.co.bithatch.eclipzoxo.Activator;
import uk.co.bithatch.eclipzoxo.components.ZoxoComponentRegistry;
import uk.co.bithatch.eclipzoxo.editor.TapeBrowser;
import uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferencesAccess;
import uk.co.bithatch.zoxo.system.Machine;
import uk.co.bithatch.zoxo.system.Machine.MachineFactory;
import uk.co.bithatch.zoxo.system.Model;
import uk.co.bithatch.zoxo.zxspectrum.ZXSpectrumSettings;

public class EmulatorView extends ViewPart {
	public static final String DEFAULT_EMULATOR = "Default";
	
	private final static ILog LOG = ILog.of(EmulatorView.class);
	
	private class FastAction extends Action {
		public FastAction() {
			super("Fast Mode", IAction.AS_CHECK_BOX);
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.FAST_PATH));
			setToolTipText("Toggle Fast Emulation Speed");
		}

		@Override
		public boolean isEnabled() {
			return getSelectedEmulator() != null;
		}

		@Override
		public void run() {
			if (isChecked())
				getSelectedEmulator().machine().speedMultiplier(ZoxoPreferencesAccess.get().getFastSpeed());
			else
				getSelectedEmulator().machine().speedMultiplier(1);
		}

	}

	private class HardResetAction extends Action {
		public HardResetAction() {
			super("Hard Reset", IAction.AS_PUSH_BUTTON);
			setToolTipText("Hard reset the emulator");
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.HARD_RESET_PATH));
		}

		@Override
		public boolean isEnabled() {
			return getSelectedEmulator() != null;
		}

		@Override
		public void run() {

			getSelectedEmulator().pause();

			if (ZoxoPreferencesAccess.get().isConfirmDestructiveActions()) {
				if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Reset Emulator",
						"Are you sure you wish to hard reset the emulator?")) {
					startEmulation();
					return;
				}
			}

			hardReset();
		}
	}

	private class HardwareAction extends Action implements IMenuCreator {

		private Map<Model, MenuItem> buttons = new HashMap<>();
		private Model selected = Model.SPECTRUM48K;
		private List<MachineFactory<?, ?>> machines = ZoxoPreferencesAccess.get().machines();

		public HardwareAction() {
			super("Hardware", IAction.AS_DROP_DOWN_MENU);
			setToolTipText("Emulated Hardware");
			setMenuCreator(this);
			select(ZoxoPreferencesAccess.get().getDefaultModel().model());
		}

		@Override
		public void dispose() {
			// Clean up if needed
		}
		
		public Model selected() {
			return selected;
		}

		@Override
		public Menu getMenu(Control parent) {
			Menu menu = new Menu(parent);
			for (var mf : machines) {
				@SuppressWarnings("unchecked")
				var ma = (MachineFactory<Machine<ZXSpectrumSettings>, ZXSpectrumSettings>)mf;
				var mt = ma.model();
				MenuItem item1 = new MenuItem(menu, SWT.NONE);
				item1.setText(ma.name());
				if (mt == selected) {
					item1.setSelection(true);
				}
				item1.addListener(SWT.Selection, e -> {
					var settings = ZoxoPreferencesAccess.get().settings(ZXSpectrumSettings.class);
					var em = getSelectedEmulator();
					if(em != null) {
						
						if (ZoxoPreferencesAccess.get().isConfirmDestructiveActions()) {
							pause();
							if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Reset Emulator",
									"Are you sure you wish to change the emulated mode? The emulator will be reset.")) {
								startEmulation();
								return;
							}
						}

						select(mt);
						em.machine(null);
						em.machine(ma.create(getOrCreateEmulator(), settings));
					}
					else {
						select(mt);
					}
				});
				buttons.put(mt, item1);
			}

			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			return null; // not used
		}

		public void select(Model spectrumModel) {
			var machine = machines.stream().filter(m -> m.model() == spectrumModel).findFirst().get();
			selected = spectrumModel;
			MenuItem button = buttons.get(spectrumModel);
			if (button != null)
				button.setSelection(true);
			setToolTipText(machine.vendor() + " " + machine.name());
			setText(machine.name());
			
			/* Need to redraw bar as size of button might have changed */
			updateToolbar();
		}
	}

	private class PauseAction extends Action {
		public PauseAction() {
			super("Paused", IAction.AS_CHECK_BOX);
		}

		@Override
		public boolean isEnabled() {
			return getSelectedEmulator() != null;
		}

		@Override
		public void run() {
			if (isChecked()) {
				pause();
			} else {
				unpause();
			}
		}
	}

	private class ResetAction extends Action {
		public ResetAction() {
			super("Reset", IAction.AS_PUSH_BUTTON);
			setToolTipText("Soft reset the emulator");
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.RESET_PATH));
		}

		@Override
		public boolean isEnabled() {
			return getSelectedEmulator() != null;
		}

		@Override
		public void run() {

			if (!ZoxoPreferencesAccess.get().isConfirmDestructiveActions()) {
				getSelectedEmulator().reset();
				return;
			}

			getSelectedEmulator().pause();

			if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Reset Emulator",
					"Are you sure you wish to reset the emulator?")) {
				getSelectedEmulator().reset();
			}

			startEmulation();
		}
	}

	private class SilenceAction extends Action {
		public SilenceAction() {
			super("Silence", IAction.AS_CHECK_BOX);
			setAsActive();
		}

		@Override
		public boolean isEnabled() {
			return getSelectedEmulator() != null;
		}

		@Override
		public void run() {
			if (isChecked()) {
				setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.MUTED_PATH));
				setToolTipText("Un-mute the audio");
				getSelectedEmulator().audio().get().muted(true);
			} else {
				setAsActive();
				getSelectedEmulator().audio().get().muted(false);
			}
		}

		private void setAsActive() {
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.UNMUTED_PATH));
			setToolTipText("Mute the audio");
		}
	}

	public static final String ID = "uk.co.bithatch.eclipzoxo.views.EmulatorView";

	public static EmulatorInstance show() throws ExecutionException {
		var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			var view = (EmulatorView) page.showView(EmulatorView.ID);
			var em = view.getOrCreateEmulator(DEFAULT_EMULATOR);
			view.showEmulator(em);
			return em;
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to open view", e);
		}
	}

	public static void open(Path nativeFile) throws ExecutionException {
		try {
			show().load(nativeFile);
		} catch (IOException e) {
			throw new ExecutionException("Failed to open view", e);
		}

	}

	private HardwareAction hardwareAction;
	private PauseAction pauseAction;
	private SilenceAction silenceAction;
	private EmulatorStatusControl statusControl;
	private CTabFolder folder;
	private final List<EmulatorInstance> emulators = new ArrayList<>();
	private final List<CTabItem> tabs = new ArrayList<>();
	private final Map<uk.co.bithatch.zoxo.system.Action, Action> actionToAction = new HashMap<>();
	private Composite stackContainer;
	private StackLayout stackLayout;
	private Composite emptyPlaceholder;

	@Override
	public void createPartControl(Composite parent) {

		makeActions();
		contributeToActionBars();

		stackLayout = new StackLayout();
		stackContainer = new Composite(parent, SWT.NONE);
		stackContainer.setLayout(stackLayout);

		// Empty placeholder shown when no tabs
		emptyPlaceholder = new Composite(stackContainer, SWT.NONE);
		var placeholderLayout = new GridLayout(1, false);
		emptyPlaceholder.setLayout(placeholderLayout);

		// Spacer to push content to center
		var topSpacer = new Label(emptyPlaceholder, SWT.NONE);
		topSpacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Icon
		var iconLabel = new Label(emptyPlaceholder, SWT.NONE);
		Image largeIcon = Activator.getDefault().getImageRegistry().get(Activator.LARGE_ICON_PATH);
		if (largeIcon != null) {
			iconLabel.setImage(largeIcon);
		}
		iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.END, true, false));

		// "Start Emulator" link
		var startLink = new Link(emptyPlaceholder, SWT.NONE);
		startLink.setText("<a>Start Emulator</a>");
		startLink.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		startLink.addListener(SWT.Selection, e -> {
			try {
				showEmulator(show());
			} catch (ExecutionException ex) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Failed to start emulator: " + ex.getMessage());
			}
		});

		// Description text
		var descLabel = new Label(emptyPlaceholder, SWT.WRAP | SWT.CENTER);
		descLabel.setText(".. or Run/Debug a program and choose Zoxo as target");
		descLabel.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, true, false));

		// Description 2 text
		var desc2Label = new Label(emptyPlaceholder, SWT.WRAP | SWT.CENTER);
		desc2Label.setText(".. or Open a supported file in project explorer");
		desc2Label.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, true, false));

		// Spacer to push content to center
		var bottomSpacer = new Label(emptyPlaceholder, SWT.NONE);
		bottomSpacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		folder = new CTabFolder(stackContainer, SWT.BORDER | SWT.LEFT);
        folder.setTabPosition(SWT.BOTTOM);
        folder.setSimple(false);
        folder.setUnselectedImageVisible(true);
        folder.setUnselectedCloseVisible(true);
        folder.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateState();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				updateState();
			}
		});
        folder.addCTabFolder2Listener(new CTabFolder2Adapter() {
            @Override
            public void close(org.eclipse.swt.custom.CTabFolderEvent event) {
                var item = (CTabItem) event.item;
                var idx = tabs.indexOf(item);
                var emu = emulators.get(idx);
                event.doit = false;
                emu.close();
            }
        });

		updateVisibility();
	}

	@Override
	public void dispose() {
		while(!emulators.isEmpty()) {
			emulators.get(0).close();
		}
		super.dispose();
	}

	public void addAction(uk.co.bithatch.zoxo.system.Action action) {
		var bars = getViewSite().getActionBars();
		var toolbar = bars.getToolBarManager();
		var actionAdapter = new Action() {
			@Override
			public void run() {
				action.action();
			}
		};
		actionAdapter.setId(action.name());
		actionAdapter.setText(action.name());
		actionToAction.put(action, actionAdapter);
		toolbar.add(actionAdapter);
	}

	public void removeAction(uk.co.bithatch.zoxo.system.Action action) {
		var actionAdapter = actionToAction.remove(action);
		if(actionAdapter != null) {
			var bars = getViewSite().getActionBars();
			var toolbar = bars.getToolBarManager();
			toolbar.remove(action.name());
		}
	}

	public void hardReset() {
		getSelectedEmulator().hardReset();
	}

	public EmulatorInstance resetAndShowEmulator(String name) {
		var em = resetOrCreateEmulator(name);
		showEmulator(em);
		return em;
	}

	public void showEmulator(EmulatorInstance em) {
		var idx = emulators.indexOf(em);
		if(idx != -1) {
			folder.setSelection(idx);
		}
	}

	public EmulatorInstance getSelectedEmulator() {
		return emulators.isEmpty() ? null : emulators.get(folder.getSelectionIndex());
	}

	public EmulatorInstance getOrCreateEmulator() {
		return getOrCreateEmulator(DEFAULT_EMULATOR);
	}

	public EmulatorInstance getOrCreateEmulator(String id) {
		for(var em : emulators) {
			if(em.getId().equals(id)) {
				return em;
			}
		}
	    return createEmulator(id);
	}

	public EmulatorInstance resetOrCreateEmulator(String id) {
		for(var em : emulators) {
			if(em.getId().equals(id)) {
				em.close();
				break;
			}
		}
	    return createEmulator(id);
	}

	public EmulatorInstance createEmulator(String id) {
		// --- Tab 1 ---
		var tab1 = new CTabItem(folder, SWT.CLOSE);
		tab1.setText(id);
		
		var instance = new EmulatorInstance(id, getSite().getService(IEvaluationService.class), folder, this);

		
		var machineType = hardwareAction.selected();
		try {
			instance.machine(ZoxoComponentRegistry.get(instance, machineType));
		}
		catch(IllegalArgumentException iae) {
			LOG.error(MessageFormat.format("Initial machine `{0}` does not exist, add-on configuration may have changed. Reverting to default.", machineType.name()));
			instance.machine(ZoxoComponentRegistry.get(instance, Model.SPECTRUM48K));
		}
		tab1.setControl(instance.screen());
//
		folder.setSelection(0);
		emulators.add(instance);
		tabs.add(tab1);

		updateVisibility();
		updateState();
		
		instance.addCloseListener(() -> {
			emulators.remove(instance);
	        if(emulators.isEmpty()) {
	        	updateVisibility();
	        }
	        updateState();
		});
		
		return instance;
	}

	void remove(EmulatorInstance emulator) {
		var idx = emulators.indexOf(emulator);
		emulators.remove(idx);
		tabs.remove(idx).dispose();
		updateVisibility();

		for (var editorRef : getViewSite().getWorkbenchWindow().getActivePage().getEditorReferences()) {
			if (editorRef.getId().equals(TapeBrowser.ID)) {
				var vp = editorRef.getPart(false);
				if (vp instanceof TapeBrowser tbrowser && Objects.equals(tbrowser.getEmulator(), emulator)) {
					tbrowser.setEmulator(null);
					break;
				}
			}
		}
	}

	public EmulatorInstance selectedEmulator() {
		return selectedEmulatorOr().orElseThrow(() -> new IllegalStateException("No emulator instance selector."));
	}

	public Optional<EmulatorInstance> selectedEmulatorOr() {
		return Optional.ofNullable(getSelectedEmulator());
	}

	public List<EmulatorInstance> getEmulators() {
		return emulators;
	}

	public boolean isPaused() {
		return pauseAction.isChecked();
	}

	public void pause() {
		pauseAction.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.PLAY_PATH));
		pauseAction.setToolTipText("Un-pause the emulator");
		getSelectedEmulator().pause();
	}

	@Override
	public void setFocus() {
		if (emulators.isEmpty()) {
			emptyPlaceholder.setFocus();
		} else {
			folder.setFocus();
			var sel = getSelectedEmulator();
			if (sel != null) {
				sel.requestFocus();
			}
		}
	}

	public void setMemoryBlock(long offset, byte[] bytes) {
		throw new UnsupportedOperationException("Not yet supported.");
	}

	public void setStatusControl(EmulatorStatusControl statusControl) {
		this.statusControl = statusControl;
	}

	public void unpause() {
		setAsActive();
		getSelectedEmulator().unpause();
	}

	void updateStatusControl() {
		if (statusControl != null)
			statusControl.updateState();
		
		updateTapeBrowserState();
	}

	void updateStatusText() {
		if (statusControl != null)
			statusControl.updateStatusText();
	}

	private void updateTapeBrowserState() {
		var em = getSelectedEmulator();
		var tapeFile = em == null ? null : em.getTapeFile();
		for (var editorRef : getViewSite().getWorkbenchWindow().getActivePage().getEditorReferences()) {
			if (editorRef.getId().equals(TapeBrowser.ID)) {
				var vp = editorRef.getPart(false);
				if (vp instanceof TapeBrowser tbrowser) {
					if(tbrowser.getNativeFile().equals(tapeFile)) {
						tbrowser.setEmulator(em);
						break;
					}
				}
			}
		}
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager toolbar = bars.getToolBarManager();

		toolbar.add(pauseAction = new PauseAction());
		toolbar.add(new FastAction());
		toolbar.add(silenceAction);
		toolbar.add(new Separator());
		toolbar.add(new ResetAction());
		toolbar.add(new HardResetAction());
		toolbar.add(new Separator());
		toolbar.add(hardwareAction);

		setAsActive();
	}

	private void makeActions() {
		hardwareAction = new HardwareAction();
		silenceAction = new SilenceAction();
	}

	private void setAsActive() {
		pauseAction.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.PAUSE_PATH));
		pauseAction.setToolTipText("Pause the emulator");
	}

	private void startEmulation() {

//        if (pauseToggleButton.isSelected()) {
//            return;
//        }

//        speedLabel.setForeground(Color.black);
//		updateGuiSelections();
		getSelectedEmulator().unpause();
	}

//	private void updateGuiSelections() {
//		var spectrum = getSelectedEmulator().getSpectrum();
//		hardwareAction.select(spectrum.getSpectrumModel());
//		switch (spectrum.getSpectrumModel()) {
//		case SPECTRUMPLUS2A: {
////                IF2MediaMenu.setEnabled(false);
//			break;
//		}
//		case SPECTRUMPLUS3: {
////                IF2MediaMenu.setEnabled(false);
//			break;
//		}
//		default:
//			break;
//		}
		//
//        switch (spectrum.getJoystick()) {
//            case KEMPSTON -> kempstonJoystick.setSelected(true);
//            case SINCLAIR1 -> sinclair1Joystick.setSelected(true);
//            case SINCLAIR2 -> sinclair2Joystick.setSelected(true);
//            case CURSOR -> cursorJoystick.setSelected(true);
//            case FULLER -> fullerJoystick.setSelected(true);
//            default -> noneJoystick.setSelected(true);
//        }
//
//        if (settings.getSpectrumSettings().isULAplus()) {
//            if (jscr.isPalFilter()) {
//                noneFilter.setSelected(true);
//                jscr.setAnyFilter(false);
//                scanlinesFilter.setEnabled(true);
//                jscr.setScanlinesFilter(scanlinesFilter.isSelected());
//                jscr.repaint();
//            }
//            palTvFilter.setEnabled(false);
//        } else {
//            palTvFilter.setEnabled(true);
//        }
//
//        switch (jscr.getBorderMode()) {
//            case 0 -> noBorder.setSelected(true);
//            case 2 -> fullBorder.setSelected(true);
//            case 3 -> hugeBorder.setSelected(true);
//            default -> standardBorder.setSelected(true);
//        }
//	}
	
	private void updateState() {
		updateToolbar();
		updateStatusControl();
	}

	private void updateToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager toolbar = bars.getToolBarManager();
		toolbar.markDirty();
	    toolbar.update(true);
	    bars.updateActionBars();
	}

	private void updateVisibility() {
		if (emulators.isEmpty()) {
			stackLayout.topControl = emptyPlaceholder;
		} else {
			stackLayout.topControl = folder;
		}
		if(!stackContainer.isDisposed()) {
			stackContainer.layout();
		}
	}


}
