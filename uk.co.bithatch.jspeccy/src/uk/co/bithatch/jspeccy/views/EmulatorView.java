package uk.co.bithatch.jspeccy.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Panel;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IEvaluationService;

import configuration.JSpeccySettings;
import machine.Keyboard.JoystickModel;
import machine.MachineTypes;
import machine.Spectrum;
import snapshots.SnapshotException;
import snapshots.SnapshotFactory;
import snapshots.SnapshotFile;
import snapshots.SnapshotSZX;
import snapshots.SpectrumState;
import uk.co.bithatch.jspeccy.Activator;
import uk.co.bithatch.jspeccy.ZXEmulatorSettings;
import uk.co.bithatch.jspeccy.ZXEmulatorSettings.SettingsListener;
import uk.co.bithatch.jspeccy.editor.TapeBrowser;
import utilities.Tape;
import utilities.Tape.TapeState;
import utilities.TapeBlockListener;
import utilities.TapeStateListener;

public class EmulatorView extends ViewPart implements SettingsListener, TapeBlockListener, TapeStateListener {
	private class FastAction extends Action {
		public FastAction() {
			super("Fast Mode", IAction.AS_CHECK_BOX);
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.FAST_PATH));
			setToolTipText("Toggle Fast Emulation Speed");
		}

		@Override
		public void run() {
			if (isChecked())
				spectrum.changeSpeed(settings.getSpectrumSettings().getFramesInt());
			else
				spectrum.changeSpeed(1);
		}

	}

	private class FileNameExtensionFilter implements Predicate<File> {

		private List<String> extensions;

		FileNameExtensionFilter(String name, String... extensions) {
			this.extensions = Arrays.asList(extensions);
		}

		@Override
		public boolean test(File t) {
			var n = t.getName();
			var i = n.lastIndexOf('.');
			if (i == -1)
				return false;
			else
				return extensions.contains(n.substring(i + 1).toLowerCase());
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

			stopEmulation();

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
					stopEmulation();
					spectrum.selectHardwareModel(mt);
					spectrum.reset();
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
				reset();
				return;
			}

			stopEmulation();

			if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Reset Emulator",
					"Are you sure you wish to reset the emulator?")) {
				reset();
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
				spectrum.muteSound(true);
			} else {
				setAsActive();
				spectrum.muteSound(false);
			}
		}

		private void setAsActive() {
			setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.UNMUTED_PATH));
			setToolTipText("Mute the audio");
		}
	}

	public static final String ID = "uk.co.bithatch.jspeccy.views.EmulatorView";

	private static String lastPath = System.getProperty("user.dir");

	private final static String[] REFRESHABLE_PROPERTIES = { "uk.co.bithatch.jspeccy.views.activeMedia",
			"uk.co.bithatch.jspeccy.views.microdrives", "uk.co.bithatch.jspeccy.views.tapePlaying",
			"uk.co.bithatch.jspeccy.views.tapeRunning", "uk.co.bithatch.jspeccy.views.tapeInserted",
			"uk.co.bithatch.jspeccy.views.tapeReady", "uk.co.bithatch.jspeccy.views.tapeRecording",
			"uk.co.bithatch.jspeccy.editor.playing", "uk.co.bithatch.jspeccy.editor.running",
			"uk.co.bithatch.jspeccy.editor.inserted", "uk.co.bithatch.jspeccy.editor.ready",
			"uk.co.bithatch.jspeccy.editor.recoding", };

	public static EmulatorView show() throws ExecutionException {
		var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			return (EmulatorView) page.showView(EmulatorView.ID);
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

	private static java.awt.Color toColor(int swt) {
		var c = Display.getDefault().getSystemColor(swt);
		return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
	}

	private Composite composite;
	private Frame frame;
	private HardwareAction hardwareAction;
	private EmulatorScreen jscr;
	private Panel panel;
	private PauseAction pauseAction;
	private FileNameExtensionFilter romExtension;
	private JSpeccySettings settings;
	private SilenceAction silenceAction;
	private FileNameExtensionFilter snapshotExtension;
	private Spectrum spectrum;
	private Tape tape;
	private FileNameExtensionFilter tapeExtension;
	private Color widgetBg;
	private Color widgetFg;
	private long currentSpeed;
	private EmulatorStatusControl statusControl;
	private File tapeFile;
	private File snapshotFile;
	private File romFile;
	private boolean extendBorder;
	private boolean extendBorderDisabledDurationLoad;
	private FileNameExtensionFilter microdriveExtension;
	private Thread emulatorThread;

	@Override
	public void blockChanged(int block) {
	}

	@Override
	public void createPartControl(Composite parent) {
		composite = new Composite(parent, SWT.EMBEDDED /* | SWT.NO_BACKGROUND */);
		widgetFg = toColor(SWT.COLOR_WIDGET_FOREGROUND);
		widgetBg = toColor(SWT.COLOR_WIDGET_BACKGROUND);

		makeActions();
		contributeToActionBars();

		var zxsettings = Activator.getDefault().settings();
		settings = zxsettings.jspeccy();
		extendBorder = zxsettings.isExtendBorderDisabledDuringLoad();
		extendBorderDisabledDurationLoad = zxsettings.isExtendBorderDisabledDuringLoad();
//        command.copyArgumentsToSettings(settings);

//      setTransferHandler(handler);
		initEmulator();
		initGUI();

		SwingUtilities.invokeLater(() -> {

//        if (command.isIf1().isPresent() && command.getIf1mdv() != null) {
//            spectrum.getInterface1().insertFile(0, command.getIf1mdv());
//        }

			// TODO this looks like where we can load our compiled BASIC exe

//        if (command.getProgramFile() != null) {
//            File file = new File(command.getProgramFile());
//            
//        }

			/* Put on Swing thread or we get hangs at startup with GTK theme */

			/* Then create container back on the SWT thread */
			composite.getDisplay().syncExec(() -> {
				frame = SWT_AWT.new_Frame(composite);
			});

			/* Then back to Swing thread for remainder */

			panel = configureSwingComponent(new JApplet());
			frame.add(panel);

			panel.add(jscr, BorderLayout.CENTER);
			frame.pack();
			panel.addKeyListener(spectrum.getKeyboard());

			emulatorThread = new Thread(spectrum, "SpectrumThread");
			emulatorThread.start();

			// Add Swing component
			startEmulation();

		});

		Activator.getDefault().settings().addListener(this);

	}

	@Override
	public void dispose() {
		Activator.getDefault().settings().removeListener(this);
		try {
			stopEmulation();
			super.dispose();
		}
		finally {
			spectrum.close();
		}
	}

	public void ejectROM() {
		if (getRomFile() == null)
			return;

		if (!MessageDialog.openConfirm(composite.getShell(), "Eject ROM and Reset",
				"The ROM will be ejected and the emulator reset. Are you sure?")) {
			return;
		}
		spectrum.ejectIF2Rom();
		setROMFile(null);
		reset();
	}

	public void ejectTape() {
		tape.eject();
		setTapeFile(null);
	}

	public String getActiveMedia() {
		if (tapeFile != null) {
			return "tape";
		} else if (romFile != null) {
			return "rom";
		} else if (snapshotFile != null) {
			return "snapshot";
		} else {
			return "none";
		}
	}

	public byte[] getMemoryBlock(long startAddress, long length) {
		return spectrum.getMemory().savePage((int) (startAddress / 8192));
	}

	public File getRomFile() {
		return romFile;
	}

	public File getSnapshotFile() {
		return snapshotFile;
	}

	public String getStatusText() {
		return String.format("%5d%%", currentSpeed);
	}

	public Tape getTape() {
		return tape;
	}

	public File getTapeFile() {
		return tapeFile;
	}

	public void hardReset() {
		spectrum.hardReset();
		switch (settings.getSpectrumSettings().getDefaultModel()) {
		case 0:
			hardwareAction.select(MachineTypes.SPECTRUM16K);
			spectrum.selectHardwareModel(MachineTypes.SPECTRUM16K);
			break;
		case 2:
			hardwareAction.select(MachineTypes.SPECTRUM128K);
			spectrum.selectHardwareModel(MachineTypes.SPECTRUM128K);
			break;
		case 3:
			hardwareAction.select(MachineTypes.SPECTRUMPLUS2);
			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS2);
			break;
		case 4:
			hardwareAction.select(MachineTypes.SPECTRUMPLUS2A);
//                IF1MediaMenu.setEnabled(false);
			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS2A);
			break;
		case 5:
			hardwareAction.select(MachineTypes.SPECTRUMPLUS3);
//                IF1MediaMenu.setEnabled(false);
			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS3);
			break;
		default:
			hardwareAction.select(MachineTypes.SPECTRUM48K);
			spectrum.selectHardwareModel(MachineTypes.SPECTRUM48K);
		}

		switch (settings.getKeyboardJoystickSettings().getJoystickModel()) {
		case 1:
//                kempstonJoystick.setSelected(true);
			spectrum.setJoystick(JoystickModel.KEMPSTON);
			break;
		case 2:
//                sinclair1Joystick.setSelected(true);
			spectrum.setJoystick(JoystickModel.SINCLAIR1);
			break;
		case 3:
//                sinclair2Joystick.setSelected(true);
			spectrum.setJoystick(JoystickModel.SINCLAIR2);
			break;
		case 4:
//                cursorJoystick.setSelected(true);
			spectrum.setJoystick(JoystickModel.CURSOR);
			break;
		default:
//                noneJoystick.setSelected(true);
			spectrum.setJoystick(JoystickModel.NONE);
		}

//        if (settings.getSpectrumSettings().getDefaultModel() < 4) {
//            IF1MediaMenu.setEnabled(settings.getInterface1Settings().isConnectedIF1());
//            IF2MediaMenu.setEnabled(true);
//        } else {
//            IF1MediaMenu.setEnabled(false);
//            IF2MediaMenu.setEnabled(false);
//        }

		resetFiles();
		startEmulation();
	}

	public void insertROMFromFileSystem() throws IOException {
		var fileDialog = new FileDialog(composite.getShell());
		fileDialog.setFilterPath(lastPath);
		fileDialog.setText("Select ROM File");
		fileDialog.setFilterExtensions(new String[] { "*.rom", "*.*" });
		fileDialog.setFilterNames(new String[] { "Spectrum ROM File (*.rom)", "All Files (*.*)" });
		var res = fileDialog.open();
		if (res != null) {
			var resFile = new File(res);
			lastPath = resFile.getParentFile().getAbsolutePath();
			load(resFile);
		}

	}

	public void insertTapeFromFileSystem() throws IOException {
		var fileDialog = new FileDialog(composite.getShell());
		fileDialog.setFilterPath(lastPath);
		fileDialog.setText("Select Tape File");
		fileDialog.setFilterExtensions(new String[] { "*.tzx;*.tap;*.csw", "*.*" });
		fileDialog.setFilterNames(new String[] { "Spectrum Tape Files (*.tsz,*.tap,*(.csw)", "All Files (*.*)" });
		var res = fileDialog.open();
		if (res != null) {
			var resFile = new File(res);
			lastPath = resFile.getParentFile().getAbsolutePath();
			load(resFile);
		}

	}

	public boolean isEmulating() {
		return spectrum != null && !spectrum.isPaused();
	}

	public boolean isPaused() {
		return pauseAction.isChecked();
	}

	public boolean isTapeInserted() {
		return tape.isTapeInserted();
	}

	public boolean isTapePlaying() {
		return tape.isTapePlaying();
	}

	public boolean isTapeReady() {
		return tape.isTapeReady();
	}

	public boolean isTapeRecording() {
		return tape.isTapeRecording();
	}

	public boolean isTapeRunning() {
		return tape.isTapeRunning();
	}

	public void screenshot(File file) {
		spectrum.saveImage(file);
	}

	public void load(File file) throws IOException {

		if(microdriveExtension.test(file) && settings.getInterface1Settings().isConnectedIF1()) {
			MicrodriveManager.openDialog(getSite().getShell(), spectrum.getInterface1(), settings, file);
			return;	
		}
		
		File tapeFile = null;
		File snapshotFile = null;
		File romFile = null;

		try {
			
			if (snapshotExtension.test(file)) {
				try {
					SnapshotFile snap = SnapshotFactory.getSnapshot(file);
					SpectrumState snapState = snap.load(file);
					if (snap instanceof SnapshotSZX snapSZX) {
						if (snapSZX.isTapeEmbedded()) {
							tape.eject();
							tape.insertEmbeddedTape(snapSZX.getTapeName(), snapSZX.getTapeExtension(),
									snapSZX.getTapeData(), snapSZX.getTapeBlock());
							snapshotFile = file;
						}

						if (snapSZX.isTapeLinked()) {
							File tapeLink = new File(snapSZX.getTapeName());

							if (tapeLink.exists()) {
								tapeFile = tapeLink;
								tape.eject();
								tape.insert(tapeLink);
								tape.setSelectedBlock(snapSZX.getTapeBlock());
							}
						}
					}

					spectrum.setSpectrumState(snapState);
					return;
				} catch (SnapshotException exception) {
					throw new IOException("Failed to load snapshot.", exception);
				}
			}

			if (tapeExtension.test(file)) {
				if (tape.insert(file)) {
					tapeFile = file;
					if (settings.getTapeSettings().isAutoLoadTape()) {
						spectrum.autoLoadTape();
					}
				} else {
					throw new IOException("Failed to load tap .");
				}
				return;
			}

			if (romExtension.test(file)) {
				if (spectrum.getSpectrumModel().codeModel == MachineTypes.CodeModel.SPECTRUMPLUS3) {
					spectrum.selectHardwareModel(MachineTypes.SPECTRUM48K);
				}

				if (spectrum.insertIF2Rom(file)) {
					romFile = file;
					// insertIF2RomMediaMenu.setEnabled(false);
					// extractIF2RomMediaMenu.setEnabled(true);
					spectrum.reset();
				} else {
					throw new IOException("Failed to load ROM.");
				}
				return;
			}

			throw new IOException("Unsupported file type for " + file);
		} finally {
			setTapeFile(tapeFile);
			setROMFile(romFile);
			setSnapshotFile(snapshotFile);
		}
	}

	public void microdrives() {
		MicrodriveManager.openDialog(composite.getShell(), spectrum.getInterface1(), settings);
	}

	public void pause() {
		pauseAction.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.PLAY_PATH));
		pauseAction.setToolTipText("Un-pause the emulator");
		stopEmulation();
	}

	public void playTape() {
		if (tape.isTapePlaying()) {
			tape.stop();
		} else {
			tape.play(true);
		}

	}

	public void reset() {
		resetFiles();
		spectrum.reset();
	}

	public void rewindTape() {
		tape.rewind();
	}

	public void nmi() {
		spectrum.triggerNMI();
	}

	public void poke() {
		PokeDialog.open(composite.getShell(), (addr, val) -> {
			var saddr = addr & 0xffff;
			spectrum.getMemory().writeByte(addr, val.byteValue());
			if (spectrum.getMemory().isScreenByte(saddr)) {
				spectrum.invalidateScreen(false);
			}
		});

	}

	@Override
	public void setFocus() {
		composite.setFocus();
		SwingUtilities.invokeLater(() -> {
			panel.requestFocus();
		});
	}

	public void setMemoryBlock(long offset, byte[] bytes) {
		throw new UnsupportedOperationException("Not yet supported.");
	}

	public void setStatusControl(EmulatorStatusControl statusControl) {
		this.statusControl = statusControl;
	}

	@Override
	public void settingsChanged(ZXEmulatorSettings settings) {

		extendBorder = settings.isExtendBorderEnabled();
		extendBorderDisabledDurationLoad = settings.isExtendBorderDisabledDuringLoad();
		SwingUtilities.invokeLater(() -> {
			updateBorderExtend();
			spectrum.setBorderMode(settings.isBorderEnabled() ? 1 : 0);
			jscr.setBorderEnabled(settings.isBorderEnabled());
		});
		requestEvaluation();
	}

	protected void updateBorderExtend() {
		jscr.setBorderExtend(Activator.getDefault().settings().isBorderEnabled() && extendBorder
				&& (!extendBorderDisabledDurationLoad || !(isTapeRunning())));
	}

	public void startRecord() {

		if (!tape.isTapeReady()) {
			MessageDialog.openError(composite.getDisplay().getActiveShell(), "Start Recording Error",
					"No tape inserted or tape is playing");
		} else {

			if (!spectrum.startRecording()) {
				MessageDialog.openError(composite.getDisplay().getActiveShell(), "Tape File Format Error",
						"The tape file isn't a TZX");
				return;
			}

		}

	}

	@Override
	public void stateChanged(TapeState state) {
		SwingUtilities.invokeLater(() -> updateBorderExtend());
		composite.getDisplay().asyncExec(() -> {
			requestEvaluation();
		});
	}

	public void stopEmulation() {

		if (spectrum.isPaused()) {
			return;
		}

		spectrum.stopEmulation();
		updateState();

//        speedLabel.setForeground(Color.red);
//        speedLabel.setText("STOP");

		updateGuiSelections();
	}

	public void stopTape() {
		tape.stop();
	}

	public void stopRecord() {
		spectrum.stopRecording();
	}

	public void unpause() {
		setAsActive();
		startEmulation();
	}

	protected void resetFiles() {
		tape.eject();
		setTapeFile(null);
		setROMFile(null);
		setSnapshotFile(null);
	}

	private <J extends Component> J configureSwingComponent(J jc) {
		if (Platform.getOS().equals("linux")) {
			jc.setBackground(widgetBg);
			jc.setForeground(widgetFg);
		}
		return jc;
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

	private void initEmulator() {

		microdriveExtension = new FileNameExtensionFilter("Microdrive", "mdv", "mdr");
		snapshotExtension = new FileNameExtensionFilter("Snapshot", "sna", "z80", "szx", "sp");
		tapeExtension = new FileNameExtensionFilter("Tap", "tap", "tzx", "csw");
		romExtension = new FileNameExtensionFilter("ROM", "rom");

		spectrum = new Spectrum(settings);
		spectrum.selectHardwareModel(settings.getSpectrumSettings().getDefaultModel());
		spectrum.setJoystick(settings.getKeyboardJoystickSettings().getJoystickModel());
		spectrum.setBorderMode(1);
//        spectrum.setBorderMode(settings.getSpectrumSettings().getBorderSize());

		spectrum.loadConfigVars();

		tape = new Tape(settings.getTapeSettings());
		tape.addTapeBlockListener(this);
		tape.addTapeChangedListener(this);
		spectrum.setTape(tape);
	}

	private void initGUI() {

		jscr = new EmulatorScreen();
		spectrum.setScreenComponent(jscr);
		jscr.setTvImage(spectrum.getTvImage());
		jscr.setBorderEnabled(true);
		updateBorderExtend();
		spectrum.onSpeedChange(speed -> {
			this.currentSpeed = speed;
			updateStatusControl();
		});

		silenceAction.setChecked(settings.getSpectrumSettings().isMutedSound());
//
//
//        if (settings.getSpectrumSettings().isHibernateMode()) {
//            File autoload = new File(System.getProperty("user.home") + "/JSpeccy.szx");
//            if (autoload.exists()) {
//                SnapshotSZX snapSZX = new SnapshotSZX();
//                try {
//                    spectrum.setSpectrumState(snapSZX.load(autoload));
//                    if (snapSZX.isTapeLinked()) {
//                        File tapeLink = new File(snapSZX.getTapeName());
//
//                        if (tapeLink.exists()) {
//                            tape.eject();
//                            tape.insert(tapeLink);
//                            tape.setSelectedBlock(snapSZX.getTapeBlock());
//                        }
//                    }
//                } catch (SnapshotException ex) {
//                    JOptionPane.showMessageDialog(this,
//                        ResourceBundle.getBundle("gui/Bundle").getString("HIBERNATE_IMAGE_ERROR"),
//                        ResourceBundle.getBundle("gui/Bundle").getString(
//                        "HIBERNATE_LOAD_ERROR"), JOptionPane.ERROR_MESSAGE);
//                }
//            }
//        }
//
//        switch (settings.getSpectrumSettings().getZoomMethod()) {
//            case 1 -> { // Bilineal
//                jscr.setInterpolationMethod(RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//                bilinearZoom.setSelected(true);
//            }
//            case 2 -> { // Bicubic
//                jscr.setInterpolationMethod(RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//                bicubicZoom.setSelected(true);
//            }
//            default -> {
//                jscr.setInterpolationMethod(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
//                standardZoom.setSelected(true);
//            }
//        }
//
//
//        switch (settings.getSpectrumSettings().getFilterMethod()) {
//            case 1 -> { // PAL TV
//                jscr.setPalFilter(true);
//                palTvFilter.setSelected(true);
//            }
//            case 2 -> { // RGB
//                jscr.setRgbFilter(true);
//                rgbFilter.setSelected(true);
//                scanlinesFilter.setEnabled(false);
//            }
//            default -> {
//                jscr.setAnyFilter(settings.getSpectrumSettings().isScanLines());
//                noneFilter.setSelected(true);
//            }
//        }

		updateGuiSelections();
	}

	private void makeActions() {
		hardwareAction = new HardwareAction();
		silenceAction = new SilenceAction();
	}

	private void requestEvaluation() {
		var eval = getSite().getService(IEvaluationService.class);
		if (eval != null) {
			for (var r : REFRESHABLE_PROPERTIES)
				eval.requestEvaluation(r);
		}
	}

	private void setAsActive() {
		pauseAction.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor(Activator.PAUSE_PATH));
		pauseAction.setToolTipText("Pause the emulator");
	}

	private void setROMFile(File romFile) {
		if (!Objects.equals(romFile, this.romFile)) {
			this.romFile = romFile;
			updateState();
		}
	}

	private void setSnapshotFile(File snapshotFile) {
		if (!Objects.equals(snapshotFile, this.snapshotFile)) {
			this.snapshotFile = snapshotFile;
			updateState();
		}
	}

	private void setTapeFile(File tapeFile) {
		if (!Objects.equals(tapeFile, this.tapeFile)) {

			for (var editorRef : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.getEditorReferences()) {
				var editor = editorRef.getEditor(false);
				if (editor instanceof TapeBrowser tapeBrowser) {
					if (Objects.equals(tapeFile, tapeBrowser.getNativeFile()))
						tapeBrowser.setEmulator(this);
					else if (Objects.equals(tapeBrowser.getEmulator(), this)) {
						tapeBrowser.setEmulator(null);
					}
				}

			}
			this.tapeFile = tapeFile;
			updateState();
		}

	}

	private void startEmulation() {

//        if (pauseToggleButton.isSelected()) {
//            return;
//        }

//        speedLabel.setForeground(Color.black);
		updateGuiSelections();
		spectrum.startEmulation();
	}

	private void updateGuiSelections() {

		if (spectrum.getSpectrumModel().codeModel != MachineTypes.CodeModel.SPECTRUMPLUS3) {
			if (settings.getInterface1Settings().isConnectedIF1()) {
//                IF1MediaMenu.setEnabled(true);
//                mdrvLabel.setEnabled(true);
//                mdrvLabel.setIcon(mdrOff);
//                mdrvLabel.setToolTipText(
//                ResourceBundle.getBundle("gui/Bundle").getString("MICRODRIVES_STOPPED"));
			} else {
//                IF1MediaMenu.setEnabled(false);
//                mdrvLabel.setEnabled(false);
//                mdrvLabel.setToolTipText(null);
			}

//            IF2MediaMenu.setEnabled(true);
//            insertIF2RomMediaMenu.setEnabled(!spectrum.isIF2RomInserted());
//            extractIF2RomMediaMenu.setEnabled(spectrum.isIF2RomInserted());
		} else {
//            IF1MediaMenu.setEnabled(false);
//            IF2MediaMenu.setEnabled(false);
//            mdrvLabel.setEnabled(false);
//            mdrvLabel.setToolTipText(null);
		}
//
		hardwareAction.select(spectrum.getSpectrumModel());
		switch (spectrum.getSpectrumModel()) {
		case SPECTRUMPLUS2A: {
//                IF2MediaMenu.setEnabled(false);
			break;
		}
		case SPECTRUMPLUS3: {
//                IF2MediaMenu.setEnabled(false);
			break;
		}
		default:
			break;
		}
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
	}

	private void updateState() {
		if (statusControl != null)
			statusControl.updateState();
		requestEvaluation();
	}

	private void updateStatusControl() {
		if (statusControl != null)
			statusControl.updateStatus();
	}

	public File getFile() {
		if (tapeFile != null) {
			return tapeFile;
		} else if (romFile != null) {
			return romFile;
		} else if (snapshotFile != null) {
			return snapshotFile;
		}
		return null;
	}

}
