package uk.co.bithatch.jspeccy.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Panel;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
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

public class EmulatorInstance extends Composite implements SettingsListener, TapeBlockListener, TapeStateListener, Closeable {
	
	public interface CloseListener {
		void onClose();
	}

	private static String lastPath = System.getProperty("user.dir");

	private final static String[] REFRESHABLE_PROPERTIES = { "uk.co.bithatch.jspeccy.views.activeMedia",
			"uk.co.bithatch.jspeccy.views.microdrives", "uk.co.bithatch.jspeccy.views.tapePlaying",
			"uk.co.bithatch.jspeccy.views.tapeRunning", "uk.co.bithatch.jspeccy.views.tapeInserted",
			"uk.co.bithatch.jspeccy.views.tapeReady", "uk.co.bithatch.jspeccy.views.tapeRecording",
			"uk.co.bithatch.jspeccy.editor.playing", "uk.co.bithatch.jspeccy.editor.running",
			"uk.co.bithatch.jspeccy.editor.inserted", "uk.co.bithatch.jspeccy.editor.ready",
			"uk.co.bithatch.jspeccy.editor.recoding", };

	private static java.awt.Color toColor(int swt) {
		var c = Display.getDefault().getSystemColor(swt);
		return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
	}

	private Frame frame;
	private EmulatorScreen jscr;
	private Panel panel;
	private FileNameExtensionFilter romExtension;
	private JSpeccySettings settings;
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
	private final IEvaluationService evaluationService;
	private final String id;
	private final EmulatorView view;
	private final List<CloseListener> listeners = new ArrayList<>();

	@Override
	public void blockChanged(int block) {
	}
	
	public void addCloseListener(CloseListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeCloseListener(CloseListener listener) {
		this.listeners.remove(listener);
	}
	
	public String getId() {
		return id;
	}

	EmulatorInstance(String id, IEvaluationService evaluationService, Composite parent, EmulatorView view) {
		super(parent, SWT.EMBEDDED);
		this.id = id;
		this.view = view;
		this.evaluationService = evaluationService;
		widgetFg = toColor(SWT.COLOR_WIDGET_FOREGROUND);
		widgetBg = toColor(SWT.COLOR_WIDGET_BACKGROUND);

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
			getDisplay().syncExec(() -> {
				frame = SWT_AWT.new_Frame(this);
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

		if (!MessageDialog.openConfirm(getShell(), "Eject ROM and Reset",
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
			spectrum.selectHardwareModel(MachineTypes.SPECTRUM16K);
			break;
		case 2:
			spectrum.selectHardwareModel(MachineTypes.SPECTRUM128K);
			break;
		case 3:
			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS2);
			break;
		case 4:
//                IF1MediaMenu.setEnabled(false);
			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS2A);
			break;
		case 5:
//                IF1MediaMenu.setEnabled(false);
			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS3);
			break;
		default:
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
		var fileDialog = new FileDialog(getShell());
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
		var fileDialog = new FileDialog(getShell());
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

	public boolean isPaused() {
		return spectrum == null || spectrum.isPaused();
	}

	public boolean isEmulating() {
		return spectrum != null && !spectrum.isPaused();
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
			MicrodriveManager.openDialog(getShell(), spectrum.getInterface1(), settings, file);
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
		MicrodriveManager.openDialog(getShell(), spectrum.getInterface1(), settings);
	}

	public void pause() {
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
		PokeDialog.open(getShell(), (addr, val) -> {
			var saddr = addr & 0xffff;
			spectrum.getMemory().writeByte(addr, val.byteValue());
			if (spectrum.getMemory().isScreenByte(saddr)) {
				spectrum.invalidateScreen(false);
			}
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
			MessageDialog.openError(getDisplay().getActiveShell(), "Start Recording Error",
					"No tape inserted or tape is playing");
		} else {

			if (!spectrum.startRecording()) {
				MessageDialog.openError(getDisplay().getActiveShell(), "Tape File Format Error",
						"The tape file isn't a TZX");
				return;
			}

		}

	}

	@Override
	public void stateChanged(TapeState state) {
		SwingUtilities.invokeLater(() -> updateBorderExtend());
		getDisplay().asyncExec(() -> {
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

//		silenceAction.setChecked(settings.getSpectrumSettings().isMutedSound());
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

	private void requestEvaluation() {
		if (evaluationService != null) {
			for (var r : REFRESHABLE_PROPERTIES)
				evaluationService.requestEvaluation(r);
		}
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
	
	Spectrum getSpectrum() {
		return spectrum;
	}

	void startEmulation() {

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
//		hardwareAction.select(spectrum.getSpectrumModel());
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


	public void requestFocus() {
		setFocus();
		SwingUtilities.invokeLater(() -> {
			panel.requestFocus();
		});
		
	}

	@Override
	public void close() {
		getDisplay().asyncExec(() -> {
			dispose();
			view.remove(this);
			listeners.forEach(CloseListener::onClose);
		});
	}
}
