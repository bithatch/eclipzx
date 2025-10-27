package uk.co.bithatch.jspeccy.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionException;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IEvaluationService;

import configuration.JSpeccySettings;
import machine.MachineTypes;
import uk.co.bithatch.jspeccy.Activator;

public class EmulatorView extends ViewPart {
	public static final String DEFAULT_EMULATOR = "Default";
	
	private class FastAction extends Action {
		public FastAction() {
			super("Fast Mode", IAction.AS_CHECK_BOX);
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.FAST_PATH));
			setToolTipText("Toggle Fast Emulation Speed");
		}

		@Override
		public void run() {
			if (isChecked())
				getSelectedEmulator().getSpectrum().changeSpeed(settings.getSpectrumSettings().getFramesInt());
			else
				getSelectedEmulator().getSpectrum().changeSpeed(1);
		}

	}

	private class HardResetAction extends Action {
		public HardResetAction() {
			super("Hard Reset", IAction.AS_PUSH_BUTTON);
			setToolTipText("Hard reset the emulator");
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.HARD_RESET_PATH));
		}

		@Override
		public void run() {

			getSelectedEmulator().stopEmulation();

			if (settings.getEmulatorSettings().isConfirmActions()) {
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

		private Map<MachineTypes, MenuItem> buttons = new HashMap<>();
		private MachineTypes selected = MachineTypes.SPECTRUM48K;

		public HardwareAction() {
			super("Hardware", IAction.AS_DROP_DOWN_MENU);
			setToolTipText("Emulated Hardware");
			setMenuCreator(this);
			select(MachineTypes.SPECTRUM48K);
		}

		@Override
		public void dispose() {
			// Clean up if needed
		}

		@Override
		public Menu getMenu(Control parent) {
			Menu menu = new Menu(parent);
			for (var mt : MachineTypes.values()) {
				MenuItem item1 = new MenuItem(menu, SWT.NONE);
				item1.setText(mt.getLongModelName());
				if (mt == selected) {
					item1.setSelection(true);
				}
				item1.addListener(SWT.Selection, e -> {
					getSelectedEmulator().stopEmulation();
					getSelectedEmulator().getSpectrum().selectHardwareModel(mt);
					getSelectedEmulator().reset();
					startEmulation();
				});
				buttons.put(mt, item1);
			}

			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			return null; // not used
		}

		public void select(MachineTypes spectrumModel) {
			selected = spectrumModel;
			MenuItem button = buttons.get(spectrumModel);
			if (button != null)
				button.setSelection(true);
			setToolTipText(spectrumModel.getLongModelName());
			setText(spectrumModel.getShortModelName());
		}
	}

	private class PauseAction extends Action {
		public PauseAction() {
			super("Paused", IAction.AS_CHECK_BOX);
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
		public void run() {

			if (!settings.getEmulatorSettings().isConfirmActions()) {
				getSelectedEmulator().reset();
				return;
			}

			getSelectedEmulator().stopEmulation();

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
		public void run() {
			if (isChecked()) {
				setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.MUTED_PATH));
				setToolTipText("Un-mute the audio");
				getSelectedEmulator().getSpectrum().muteSound(true);
			} else {
				setAsActive();
				getSelectedEmulator().getSpectrum().muteSound(false);
			}
		}

		private void setAsActive() {
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.UNMUTED_PATH));
			setToolTipText("Mute the audio");
		}
	}

	public static final String ID = "uk.co.bithatch.jspeccy.views.EmulatorView";

	public static EmulatorInstance show() throws ExecutionException {
		var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			return ((EmulatorView) page.showView(EmulatorView.ID)).getEmulator(DEFAULT_EMULATOR);
		} catch (PartInitException e) {
			throw new ExecutionException("Failed to open view", e);
		}
	}

	public static void open(File nativeFile) throws ExecutionException {
		try {
			show().load(nativeFile);
		} catch (IOException e) {
			throw new ExecutionException("Failed to open view", e);
		}

	}

	private HardwareAction hardwareAction;
	private PauseAction pauseAction;
	private JSpeccySettings settings;
	private SilenceAction silenceAction;
	private EmulatorStatusControl statusControl;
	private CTabFolder folder;
	private final List<EmulatorInstance> emulators = new ArrayList<>();
	private final List<CTabItem> tabs = new ArrayList<>();

	@Override
	public void createPartControl(Composite parent) {

		makeActions();
		contributeToActionBars();

		var zxsettings = Activator.getDefault().settings();
		settings = zxsettings.jspeccy();
//        command.copyArgumentsToSettings(settings);

//      setTransferHandler(handler);
		initGUI();
		

        folder = new CTabFolder(parent, SWT.BORDER | SWT.LEFT);
        folder.setTabPosition(SWT.BOTTOM);
        folder.setSimple(false);
        folder.setUnselectedImageVisible(true);
        folder.setUnselectedCloseVisible(true);
        
        folder.addCTabFolder2Listener(new CTabFolder2Adapter() {
            @Override
            public void close(org.eclipse.swt.custom.CTabFolderEvent event) {
                var item = (CTabItem) event.item;
                System.out.println("Closing tab: " + item.getText());
                var idx = tabs.indexOf(item);
                var emu = emulators.get(idx);
                event.doit = false;
                emu.close();
                // Default behavior is to dispose() the tab if event.doit is true
                // You can cancel the close by setting event.doit = false
                // event.doit = false; // uncomment to block close
            }
        });
	}

	public void hardReset() {
		EmulatorInstance spectrum = getSelectedEmulator();
		switch (settings.getSpectrumSettings().getDefaultModel()) {
		case 0:
			hardwareAction.select(MachineTypes.SPECTRUM16K);
			break;
		case 2:
			hardwareAction.select(MachineTypes.SPECTRUM128K);
			break;
		case 3:
			hardwareAction.select(MachineTypes.SPECTRUMPLUS2);
			break;
		case 4:
			hardwareAction.select(MachineTypes.SPECTRUMPLUS2A);
//                IF1MediaMenu.setEnabled(false);
			break;
		case 5:
			hardwareAction.select(MachineTypes.SPECTRUMPLUS3);
//                IF1MediaMenu.setEnabled(false);
			break;
		default:
			hardwareAction.select(MachineTypes.SPECTRUM48K);
		}
		spectrum.hardReset();

	}

	public EmulatorInstance showEmulator(String name) {
		var em = getEmulator(name);
		var idx = emulators.indexOf(em);
		if(idx != -1) {
			folder.setSelection(idx);
		}
		return em;
	}

	public EmulatorInstance getEmulator() {
		return getEmulator(DEFAULT_EMULATOR);
	}

	public EmulatorInstance getEmulator(String id) {
		for(var em : emulators) {
			if(em.getId().equals(id)) {
				return em;
			}
		}
	    return createEmulator(id);
	}

	public EmulatorInstance createEmulator(String id) {
		// --- Tab 1 ---
		var tab1 = new CTabItem(folder, SWT.CLOSE);
		tab1.setText(id);
		
		var instance = new EmulatorInstance(id, getSite().getService(IEvaluationService.class), folder, this);
		tab1.setControl(instance);
//
		folder.setSelection(0);
		emulators.add(instance);
		tabs.add(tab1);
		
		return instance;
	}

	void remove(EmulatorInstance emulator) {
		var idx = emulators.indexOf(emulator);
		emulators.remove(idx);
		tabs.remove(idx).dispose();
	}

	public Optional<EmulatorInstance> selectedEmulator() {
		return Optional.ofNullable(getSelectedEmulator());
	}

	public EmulatorInstance getSelectedEmulator() {
		return emulators.isEmpty() ? null : emulators.get(folder.getSelectionIndex());
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
		folder.setFocus();
		var sel = getSelectedEmulator();
		if (sel != null) {
			sel.requestFocus();
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
		;
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

	private void initGUI() {

//		silenceAction.setChecked(settings.getSpectrumSettings().isMutedSound());
//		updateGuiSelections();
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
		getSelectedEmulator().startEmulation();
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

	private void updateStatusControl() {
		if (statusControl != null)
			statusControl.updateStatus();
	}

}
