package uk.co.bithatch.eclipzoxo.views;

import static uk.co.bithatch.bitzx.FileNames.hasExtensions;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.services.IEvaluationService;

import uk.co.bithatch.eclipzoxo.Activator;
import uk.co.bithatch.eclipzoxo.components.ZoxoComponentRegistry;
import uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferenceConstants;
import uk.co.bithatch.eclipzoxo.preferences.ZoxoPreferencesAccess;
import uk.co.bithatch.zoxo.audio.AudioAddOn;
import uk.co.bithatch.zoxo.interface1.Interface1AddOn;
import uk.co.bithatch.zoxo.interface2.Interface2AddOn;
import uk.co.bithatch.zoxo.snapshots.SnapshotsAddOn;
import uk.co.bithatch.zoxo.swt.SWTGraphicalToolkit;
import uk.co.bithatch.zoxo.swt.SWTScreen;
import uk.co.bithatch.zoxo.swt.SWTSystemKeyboard;
import uk.co.bithatch.zoxo.system.Action;
import uk.co.bithatch.zoxo.system.AddOn.Factory;
import uk.co.bithatch.zoxo.system.AddOnManager;
import uk.co.bithatch.zoxo.system.AddOnSettings;
import uk.co.bithatch.zoxo.system.BorderMode;
import uk.co.bithatch.zoxo.system.GraphicalToolkit;
import uk.co.bithatch.zoxo.system.Machine;
import uk.co.bithatch.zoxo.system.Machine.MachineFactory;
import uk.co.bithatch.zoxo.system.MachineListener;
import uk.co.bithatch.zoxo.system.MachineSettings;
import uk.co.bithatch.zoxo.system.Model;
import uk.co.bithatch.zoxo.system.Session;
import uk.co.bithatch.zoxo.system.Tape;
import uk.co.bithatch.zoxo.system.TapePlayerAddOn;
import uk.co.bithatch.zoxo.system.TapeSettings;
import uk.co.bithatch.zoxo.system.TapeState;
import uk.co.bithatch.zoxo.system.TapeStateListener;

public class EmulatorInstance implements Session, IPropertyChangeListener, Closeable, TapeStateListener {	
	public interface CloseListener {
		void onClose();
	}

	private static String lastPath = System.getProperty("user.dir");

	private final static String[] REFRESHABLE_PROPERTIES = { "uk.co.bithatch.eclipzoxo.views.activeMedia",
			"uk.co.bithatch.eclipzoxo.views.microdrives", "uk.co.bithatch.eclipzoxo.views.tapePlaying",
			"uk.co.bithatch.eclipzoxo.views.tapeRunning", "uk.co.bithatch.eclipzoxo.views.tapeInserted",
			"uk.co.bithatch.eclipzoxo.views.tapeReady", "uk.co.bithatch.eclipzoxo.views.tapeRecording",
			"uk.co.bithatch.eclipzoxo.editor.playing", "uk.co.bithatch.eclipzoxo.editor.running",
			"uk.co.bithatch.eclipzoxo.editor.inserted", "uk.co.bithatch.eclipzoxo.editor.ready",
			"uk.co.bithatch.eclipzoxo.editor.recoding", };
	
	private GraphicalToolkit toolkit;
	private SWTScreen screen;
	private final EmulatorView view;
//	private AnimationTimer timer;
	private Machine<?> machine;
	private final ZoxoPreferencesAccess config;
	private final Display display;
	private final IEvaluationService evaluationService;
	private final List<CloseListener> listeners = new ArrayList<>();
	private final String id;
	private final Shell shell;

	private long currentSpeed;
	private CTabFolder folder;
	private Path snapshotFile;


	EmulatorInstance(String id, IEvaluationService evaluationService, CTabFolder folder, EmulatorView emulatorView) {
		this.id = id;
		this.view = emulatorView;
		this.folder = folder;
		this.evaluationService = evaluationService;
		this.shell = emulatorView.getViewSite().getShell();
		this.display = shell.getDisplay();
		
		config = ZoxoPreferencesAccess.get();
		config.getPreferenceStore().addPropertyChangeListener(this);

		toolkit = new SWTGraphicalToolkit(Display.getCurrent());
	}

	@Override
	public AddOnManager createAddOnManager(Machine<?> machine) {
		return new AddOnManager(machine, m -> ZoxoComponentRegistry.addOns(m));
	}

	public void insertTapeFromFileSystem() throws IOException {
		var fileDialog = new FileDialog(shell);
		fileDialog.setFilterPath(lastPath);
		fileDialog.setText("Select Tape File");
		fileDialog.setFilterExtensions(new String[] { "*.tzx;*.tap;*.csw", "*.*" });
		fileDialog.setFilterNames(new String[] { "Spectrum Tape Files (*.tsz,*.tap,*(.csw)", "All Files (*.*)" });
		var res = fileDialog.open();
		if (res != null) {
			var resFile = Paths.get(res);
			lastPath = resFile.getParent().toAbsolutePath().toString();
			load(resFile);
		}
	}

	public boolean isEmulating() {
		return machine != null && !machine.isPaused();
	}

	public void insertROMFromFileSystem() throws IOException {
		var fileDialog = new FileDialog(shell);
		fileDialog.setFilterPath(lastPath);
		fileDialog.setText("Select ROM File");
		fileDialog.setFilterExtensions(new String[] { "*.rom", "*.*" });
		fileDialog.setFilterNames(new String[] { "Spectrum ROM File (*.rom)", "All Files (*.*)" });
		var res = fileDialog.open();
		if (res != null) {
			var resFile = Paths.get(res);
			lastPath = resFile.getParent().toAbsolutePath().toString();
			load(resFile);
		}

	}

	public void screenshot(Path file) throws IOException {
		try(var out = Files.newOutputStream(file)) {
			machine.saveScreen(out);
		}
	}

	public Path getSnapshotFile() {
		return snapshotFile;
	}

	public Path getRomFile() {
		return interface2().map(Interface2AddOn::getFile).orElse(null);
	}

	public Path getTapeFile() {
		return tapePlayer().map(TapePlayerAddOn::getTape).map(Tape::getTapeFilename).orElse(null);
	}

	public Path getFile() {
		var tapeFile = getTapeFile();
		if (tapeFile != null) {
			return tapeFile;
		} else {
			var romFile = getRomFile();
			if (romFile != null) {
				return romFile;
			} else {
				var snapshotFile = getSnapshotFile();
				if (snapshotFile != null) {
					return snapshotFile;
				}
				return null;
			}
		}
	}

	public String getActiveMedia() {
		var tapeFile = getTapeFile();
		if (tapeFile != null) {
			return "tape";
		} else {
			var romFile = getRomFile();
			if (romFile != null) {
				return "rom";
			} else {
				var snapshotFile = getSnapshotFile();
				if (snapshotFile != null) {
					return "snapshot";
				} else {
					return "none";
				}
			}
		}
	}
	
	public void ejectROM() {
		if (getRomFile() == null)
			return;

		if (!MessageDialog.openConfirm(shell, "Eject ROM and Reset",
				"The ROM will be ejected and the emulator reset. Are you sure?")) {
			return;
		}
		
		var iface2 = interface2().get(); 
		iface2.ejectIF2Rom();
		reset();
	}
	
	public void reset() {
		resetFiles();
		machine.reset();
	}
	
	public void addCloseListener(CloseListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeCloseListener(CloseListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public Action addAction(Action action) {
		display.asyncExec(() -> view.addAction(action));
		return action;
	}

	@Override
	public Action removeAction(Action action) {
		display.asyncExec(() -> view.removeAction(action));
		return action;
	}

	@Override
	public Path dataDir() {
		return Activator.getDefault().getStateLocation().toPath();
	}

	public String getStatusText() {
		return String.format("%5d%%", currentSpeed);
	}
	
	@Override
	public final Machine<?> machine() {
		return machine;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean isPaused() {
		return machine.isPaused();
	}
	
	public void startRecord() {
		tape().ifPresent(tape -> {
			if (!tape.isTapeReady()) {
				MessageDialog.openError(shell, "Start Recording Error",
						"No tape inserted or tape is playing");
			} else {
				try {
					tape.startRecording();
				}
				catch(Exception e) {
					MessageDialog.openError(shell, "Tape File Format Error",
							e.getMessage());	
				}
			}	
		});
		

	}
	
	public Optional<Tape> tape() {
		return tapePlayer().map(TapePlayerAddOn::getTape);
	}

	public void microdrives() {
		var iface1 = machine.addOnManager().addOnOfType(Interface1AddOn.class).get();
		MicrodriveManager.openDialog(
			shell, 
			iface1.getInterface1(), 
			iface1.settings()
		);
	}

	public void nmi() {
		machine.cpu().triggerNMI();
	}

	public void poke() {
		PokeDialog.open(shell, (addr, val) -> {
			var saddr = addr & 0xffff;
			machine.memory().writeByte(addr, val.byteValue());
			if (machine.memory().isScreenByte(saddr)) {
				machine.invalidateScreen(false);
			}
		});

	}

	public void requestFocus() {
		screen.setFocus();
	}

	public void pause() {
		
		if (isPaused()) {
			return;
		}

		machine.stopEmulation();
		updateState();
	}
	
	public void unpause() {
		updateState();
		machine.startEmulation();
	}

	@Override
	public void machine(Machine<?> machine) {
		if(!Objects.equals(machine, machine())) {
			updateMachine(machine(), () -> machine);
		}
	}

	private void updateMachine(Machine<?> wasMachine, Supplier<Machine<?>> machine) {

		boolean wasMuted;
		if(wasMachine == null) {
			 wasMuted = config.isMuted();
		}
		else {
			wasMuted = audio().map(a -> a.muted()).orElse(true);
			tapePlayer().ifPresent(tp -> tp.getTape().removeTapeChangedListener(this));
			wasMachine.close();
//			timer.stop();
		}

		Machine<?> m;
		if(screen == null && machine != null) {
			m = machine.get();
			var kb = new SWTSystemKeyboard(m);
			screen = new SWTScreen(folder, m, kb);
		}
		else if(screen != null) {
			m = machine.get();
			screen.setMachine(m);
			screen.getKeyboard().setMachine(m);
		} else {
			m = null;
		}

		this.machine = m;

		if(m != null) {

			m.addMachineListener(new MachineListener() {
				
				@Override
				public void runStateChanged(boolean arg0) {
					updateStatusControl();
					
				}
				
				@Override
				public void cpuSpeedChange(long speed) {
					currentSpeed = speed;
					updateStatusText();
					
				}
			});
			
			m.borderMode(BorderMode.STANDARD);
		    m.startEmulation();
		    audio().ifPresent(a -> a.muted(wasMuted));
			tapePlayer().ifPresent(tp -> tp.getTape().addTapeChangedListener(this));

	        new Thread(m, "SpectrumThread").start();
		}
	}
	
	public Optional<Interface2AddOn> interface2() {
		return machine.addOnManager().addOnOfType(Interface2AddOn.class);
	}
	
	public Optional<SnapshotsAddOn> snapshots() {
		return machine.addOnManager().addOnOfType(SnapshotsAddOn.class);
	}
	
	public Optional<TapePlayerAddOn<?>> tapePlayer() {
		return machine.addOnManager().addOnOfType(TapePlayerAddOn.class).map(y -> (TapePlayerAddOn<?>)y);
	}
	
	public Optional<AudioAddOn<?, ?, ?>> audio() {
		return audio(machine());
	}
	
	public Optional<AudioAddOn<?, ?, ?>> audio(Machine<?> machine) {
		return machine.addOnManager().addOnOfType(AudioAddOn.class).map(a -> (AudioAddOn<?, ?, ?>)a);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <SETTINGS extends AddOnSettings> Optional<SETTINGS> getSettingsForAddOnFactory(Factory<?, ?> addOnFactory, Machine<?> machine) {
		return (Optional<SETTINGS>) Optional.ofNullable(ZoxoPreferencesAccess.get().settings(addOnFactory.settingsType()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <SETTINGS extends MachineSettings> SETTINGS getSettingsForMachineFactory(MachineFactory<?, ?> addOnFactory) {
		return (SETTINGS) addOnFactory.defaultSettings();
	}

	@Override
	public boolean isAddOnEnabled(Factory<?, ?> addOnFactory, Machine<?> machine) {
		return config.isEnabled(addOnFactory.type());
	}


	@Override
	public boolean isAddOnDisabled(Factory<?, ?> addOnFactory, Machine<?> machine) {
		return !config.isEnabled(addOnFactory.type());
	}

	@Override
	public GraphicalToolkit toolkit() {
		return toolkit;
	}

	@Override
	public SWTScreen screen() {
		return screen;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if(event.getProperty().startsWith(ZoxoPreferenceConstants.KEY_ENABLED + ".")) {
			machine().addOnManager().refresh();
		}
	}

	@Override
	public void close() {
		if(Thread.currentThread() == display.getThread()) {
//		display.asyncExec(() -> {
			dispose();
			view.remove(this);
			listeners.forEach(CloseListener::onClose);
		}
		else {
			display.asyncExec(this::close);
		}
//		});
	}

	public byte[] getMemoryBlock(long startAddress, long length) {
		return machine.memory().getMemoryDataProvider().getData(startAddress, (int)length);
	}

	public void setMemoryBlock(long offset, byte[] bytes) {
		machine.memory().getMemoryDataProvider().setData(offset, bytes);
	}

	@Override
	public void stateChanged(TapeState arg0) {
		updateState();
	}

	public void hardReset() {
		machine.hardReset();
//		switch (settings.getSpectrumSettings().getDefaultModel()) {
//		case 0:
//			spectrum.selectHardwareModel(MachineTypes.SPECTRUM16K);
//			break;
//		case 2:
//			spectrum.selectHardwareModel(MachineTypes.SPECTRUM128K);
//			break;
//		case 3:
//			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS2);
//			break;
//		case 4:
////                IF1MediaMenu.setEnabled(false);
//			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS2A);
//			break;
//		case 5:
////                IF1MediaMenu.setEnabled(false);
//			spectrum.selectHardwareModel(MachineTypes.SPECTRUMPLUS3);
//			break;
//		default:
//			spectrum.selectHardwareModel(MachineTypes.SPECTRUM48K);
//		}
//
//		switch (settings.getKeyboardJoystickSettings().getJoystickModel()) {
//		case 1:
////                kempstonJoystick.setSelected(true);
//			spectrum.setJoystick(JoystickModel.KEMPSTON);
//			break;
//		case 2:
////                sinclair1Joystick.setSelected(true);
//			spectrum.setJoystick(JoystickModel.SINCLAIR1);
//			break;
//		case 3:
////                sinclair2Joystick.setSelected(true);
//			spectrum.setJoystick(JoystickModel.SINCLAIR2);
//			break;
//		case 4:
////                cursorJoystick.setSelected(true);
//			spectrum.setJoystick(JoystickModel.CURSOR);
//			break;
//		default:
////                noneJoystick.setSelected(true);
//			spectrum.setJoystick(JoystickModel.NONE);
//		}

//        if (settings.getSpectrumSettings().getDefaultModel() < 4) {
//            IF1MediaMenu.setEnabled(settings.getInterface1Settings().isConnectedIF1());
//            IF2MediaMenu.setEnabled(true);
//        } else {
//            IF1MediaMenu.setEnabled(false);
//            IF2MediaMenu.setEnabled(false);
//        }

		resetFiles();
		unpause();
	}

	
	public void load(Path file) throws IOException {

		if(isMicrodrive(file) && config.isEnabled(Interface1AddOn.class)) {
			var iface1 = machine.addOnManager().addOnOfType(Interface1AddOn.class).get();
			MicrodriveManager.openDialog(shell, iface1.getInterface1(), iface1.settings(), file);
			return;	
		}
		
		Path snapshotFile = null;

		try {
			
			if (isSnapshot(file)) {
				
				throw new IOException("Snapshots not yet implemented.");
//				snapshotFile = snapshots().map(s -> {
//				});
				
//				try {
//					SnapshotFile snap = SnapshotFactory.getSnapshot(file);
//					SpectrumState snapState = snap.load(file);
//					if (snap instanceof SnapshotSZX snapSZX) {
//						if (snapSZX.isTapeEmbedded()) {
//							tape.eject();
//							tape.insertEmbeddedTape(snapSZX.getTapeName(), snapSZX.getTapeExtension(),
//									snapSZX.getTapeData(), snapSZX.getTapeBlock());
//							snapshotFile = file;
//						}
//
//						if (snapSZX.isTapeLinked()) {
//							File tapeLink = new File(snapSZX.getTapeName());
//
//							if (tapeLink.exists()) {
//								tapeFile = tapeLink;
//								tape.eject();
//								tape.insert(tapeLink);
//								tape.setSelectedBlock(snapSZX.getTapeBlock());
//							}
//						}
//					}
//
//					spectrum.setSpectrumState(snapState);
//					return;
//				} catch (SnapshotException exception) {
//					throw new IOException("Failed to load snapshot.", exception);
//				}
				
			}

			if (isTape(file)) {
				
				tapePlayer().ifPresent(tp -> {
					try {
						tp.getTape().insert(file);
						if(config.settings(TapeSettings.class).isAutoLoad()) {
							tp.autoLoadTape();
						}
					}
					catch(IOException ioe) {
						throw new UncheckedIOException(ioe);
					}
				});
				
				return;
			}

			if (isROM(file)) {
				
				interface2().ifPresent(iface2 -> {
					
					if(machine.model().ordinal() >= Model.SPECTRUMPLUS2A.ordinal()) {
						updateMachine(machine(), () -> ZoxoComponentRegistry.get(this, Model.SPECTRUM48K));
					}
					
					if(iface2.insertIF2Rom(file)) {
						machine.reset();
					}
					else  {
						throw new UncheckedIOException(new IOException("Failed to load ROM."));
					}
				});
				
				return;
			}

			throw new IOException("Unsupported file type for " + file);
		}
		catch(UncheckedIOException ioe) {
			throw ioe.getCause();
		} finally {
			this.snapshotFile = snapshotFile;
		}
	}

	private void resetFiles() {
		try {
			tapePlayer().map(TapePlayerAddOn::getTape).ifPresent(t -> {
				try {
					if(t.isTapeInserted())
						t.eject();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
		finally {
			interface2().ifPresent(iface2 -> {
				iface2.ejectIF2Rom();
			});
			snapshotFile = null;	
		}
	}

	private void dispose() {
		config.getPreferenceStore().removePropertyChangeListener(this);
		try {
			pause();
			screen.dispose();
		}
		finally {
			machine.close();
		}
	}

	private void requestEvaluation() {
		if (evaluationService != null) {
			for (var r : REFRESHABLE_PROPERTIES)
				evaluationService.requestEvaluation(r);
		}
	}

	private void updateState() {
		updateStatusControl();
		requestEvaluation();
	}

	private void updateStatusControl() {
		view.updateStatusControl();
	}

	private void updateStatusText() {
		view.updateStatusText();
	}
	
	private boolean isMicrodrive(Path path) {
		return hasExtensions(path, "mdv", "mdr");
	}
	
	private boolean isSnapshot(Path path) {
		return hasExtensions(path, "sna", "z80", "szx", "sp");
	}
	
	private boolean isTape(Path path) {
		return hasExtensions(path, "tap", "tzx", "csw");
	}
	
	private boolean isROM(Path path) {
		return hasExtensions(path, "rom");
	}
}
