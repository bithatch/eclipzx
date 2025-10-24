package uk.co.bithatch.ayzxfx.editor;

import static javax.swing.SwingUtilities.invokeLater;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.EventObject;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.EditorPart;

import uk.co.bithatch.ayzxfx.ay.AFX;
import uk.co.bithatch.ayzxfx.ay.AFXFrame;
import uk.co.bithatch.ayzxfx.ay.AYPlayer;

public class AFXEditor extends EditorPart /* implements SelectionListener */ {

	@SuppressWarnings("serial")
	class ValuePanel extends JPanel {
		protected final ValueBar slider;

		protected ValuePanel(int max, int value, boolean logarithmic) {
			super(new BorderLayout());
			slider = new ValueBar(max, value) {
				@Override
				public boolean isLogarithmic() {
					return logarithmic && AFXEditor.this.isLogarithmicScaling();
				}
			};
			slider.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			slider.addPropertyChangeListener(pce -> {
				if (pce.getPropertyName().equals(ValueBar.SELECTION)) {
					valueChosen(((Integer) pce.getNewValue()).intValue());
				} else if (pce.getPropertyName().equals(ValueBar.VALUE)) {
					valueChanged(((Integer) pce.getNewValue()).intValue());
				} else if (pce.getPropertyName().equals(ValueBar.CANCELLED)) {
					valueCancelled();
				}
			});
			setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			add(slider);
		}

		public ValueBar getValueBar() {
			return slider;
		}

		public void updateValue(int bt) {
			slider.setValue(bt);
		}

		protected void valueCancelled() {
		}

		protected void valueChanged(int value) {
		}

		protected void valueChosen(int value) {
		}
	}

	@SuppressWarnings("serial")
	private class ValueEditor extends ValuePanel implements TableCellEditor {
		protected transient ChangeEvent changeEvent;

		private int column;
		private boolean lastWasMultiSelect;

		private int row;

		private JTable table;

		public ValueEditor(int max, int value, boolean logarithmic) {
			super(max, value, logarithmic);
		}

		@Override
		public void addCellEditorListener(CellEditorListener l) {
			listenerList.add(CellEditorListener.class, l);
		}

		@Override
		public void cancelCellEditing() {
			fireEditingCanceled();
		}

		@Override
		public Object getCellEditorValue() {
			return slider.getValue();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			this.table = table;
			this.column = column;
			this.row = row;
			if (!freestyleMode) {
				if (lastWasMultiSelect)
					table.addRowSelectionInterval(row, row);
				else
					table.getSelectionModel().setSelectionInterval(row, row);
				this.setBackground(table.getSelectionBackground());
			}
			updateValue((Integer) value);
			return this;
		}

		@Override
		public boolean isCellEditable(EventObject e) {
			if (e instanceof MouseEvent me) {
				lastWasMultiSelect = me.isControlDown() || me.isShiftDown();
			} else {
				lastWasMultiSelect = false;
			}
			return true;
		}

		@Override
		public void removeCellEditorListener(CellEditorListener l) {
			listenerList.remove(CellEditorListener.class, l);
		}

		@Override
		public boolean shouldSelectCell(EventObject anEvent) {
			return false;
		}

		@Override
		public boolean stopCellEditing() {
			fireEditingStopped();
			return true;
		}

		protected void fireEditingCanceled() {
			Object[] listeners = listenerList.getListenerList();
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == CellEditorListener.class) {
					if (Objects.isNull(changeEvent)) {
						changeEvent = new ChangeEvent(this);
					}
					((CellEditorListener) listeners[i + 1]).editingCanceled(changeEvent);
				}
			}
		}

		protected void fireEditingStopped() {
			Object[] listeners = listenerList.getListenerList();
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == CellEditorListener.class) {
					if (Objects.isNull(changeEvent)) {
						changeEvent = new ChangeEvent(this);
					}
					((CellEditorListener) listeners[i + 1]).editingStopped(changeEvent);
				}
			}
		}

		@Override
		protected void valueCancelled() {
			cancelCellEditing();
		}

		@Override
		protected void valueChosen(int value) {
			fireEditingStopped();
			table.getModel().setValueAt(value, row, column);
		}
	}

	@SuppressWarnings("serial")
	private class ValueRenderer extends ValuePanel implements TableCellRenderer {
		public ValueRenderer(int max, int value, boolean logarithmic) {
			super(max, value, logarithmic);
			setName("Table.cellRenderer");
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
			updateValue((Integer) value);
			return this;
		}
	}

	private static java.awt.Color toColor(int swt) {
		var c = Display.getDefault().getSystemColor(swt);
		return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
	}

	protected IOperationHistory history;
	protected AFXTableModel model;
	protected Composite embed;
	
	private boolean autoAdd;
	private boolean dirty;
	private Frame frame;
	private boolean freestyleMode;
	private Color listBackground;
	private Color listForeground;
	private boolean logarithmicScaling = true;
	private ProgressBar progress;
	private JScrollPane scrollpane;
	private Color selectionBackground;
	private Color selectionForeground;
	private final IUndoContext undoContext = new ObjectUndoContext(this);
	private JTable viewer;
	private Color widgetBackground;
	private Color widgetForeground;

	public void addFrame() {
		if (viewer.getRowCount() == 0)
			addFrame(new AFXFrame());
		else {
			var lastRow = model.afx().frames().get(viewer.getRowCount() - 1);
			addFrame(lastRow.copy());
		}
	}

	public void addFrame(AFXFrame newFrame) {
		var op = new AddFrameOperation(model, newFrame);
		execute(op);
	}

	@Override
	public void createPartControl(Composite parent) {

		setupUndo();

		var root = new Composite(parent, SWT.NONE);
		root.setLayout(new org.eclipse.swt.layout.BorderLayout());

		embed = new Composite(root, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		embed.setLayoutData(new BorderData(SWT.CENTER));

		progress = new ProgressBar(root, SWT.NONE);
		progress.setLayoutData(new BorderData(SWT.BOTTOM));

		createAccessory(root);

		frame = configureSwingComponent(SWT_AWT.new_Frame(embed));
		frame.setLayout(new BorderLayout());

		widgetBackground = toColor(SWT.COLOR_WIDGET_BACKGROUND);
		widgetBackground = toColor(SWT.COLOR_WIDGET_BACKGROUND);
		widgetForeground = toColor(SWT.COLOR_WIDGET_FOREGROUND);
		listBackground = toColor(SWT.COLOR_LIST_BACKGROUND);
		listForeground = toColor(SWT.COLOR_LIST_FOREGROUND);
		selectionBackground = toColor(SWT.COLOR_LIST_SELECTION);
		selectionForeground = toColor(SWT.COLOR_LIST_SELECTION_TEXT);

		var panel = configureSwingComponent(new Panel());
		panel.setLayout(new BorderLayout());
		panel.setBackground(widgetBackground);

		createTable();

		progress.setMaximum(model.getRowCount());

		scrollpane = configureSwingComponent(new JScrollPane(viewer));
		scrollpane.setBackground(widgetBackground);
		panel.add(scrollpane, BorderLayout.CENTER);
		frame.add(panel, BorderLayout.CENTER);
		configureSwingComponent(scrollpane.getVerticalScrollBar());
		
		onPartControl();
	}

	@Override
	public void dispose() {
		history.dispose(undoContext, dirty, dirty, dirty);
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		var file = getEditorInput().getAdapter(IFile.class);
		try {
			doSave(file);
			markClean();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void doSaveAs() {
		// Optional
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IUndoContext.class) {
			return (T) undoContext;
		}
		return super.getAdapter(adapter);
	}

	public IUndoContext getUndoContext() {
		return undoContext;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {

		setSite(site);
		setInput(input);
		setPartName(input.getName());

		var file = getFile();
		var loc = file.getLocation();

		model = new AFXTableModel(load(loc), (row, func) -> {
			execute(new UpdateFrameOperation(model, row, func));
		}, () -> {
			return viewer.getSelectedRows();
		});

		onInit();
	}

	public boolean isAutoAdd() {
		return autoAdd;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public boolean isLogarithmicScaling() {
		return logarithmicScaling;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public boolean isSnapToNote() {
		return model.isSnapToNote();
	}

	public void play() {

		var player = new AYPlayer.Builder(model.afx()).onFrame((idx, f) -> {
			embed.getDisplay().execute(() -> {
				progress.setSelection(idx);
			});
		}).build();

		new Thread(() -> {
			try {
				player.run();
			} finally {
				embed.getDisplay().execute(() -> {
					progress.setSelection(0);
				});
			}
		}, "PlayAFX").start();
	}

	public void removeSelection() {
		execute(new RemoveFrameOperation(model, viewer.getSelectedRows()));
	}

	public AFX afx() {
		return model.afx();
	}

	public void afx(AFX afx) {
		model.afx(afx);
	}
	
	public void setAutoAdd(boolean autoAdd) {
		if (this.autoAdd != autoAdd) {
			this.autoAdd = autoAdd;
		}
	}

	@Override
	public void setFocus() {
		embed.setFocus();
		invokeLater(viewer::requestFocus);
	}

	public void setFreestyleMode(boolean freestyleMode) {
		if (this.freestyleMode != freestyleMode) {
			this.freestyleMode = freestyleMode;
			if (SwingUtilities.isEventDispatchThread()) {
				setupTableSelection();
			} else {
				SwingUtilities.invokeLater(this::setupTableSelection);
			}
		}
	}

	public void setLogarithmicScaling(boolean logarithmicScaling) {
		if(this.logarithmicScaling != logarithmicScaling) {
			this.logarithmicScaling = logarithmicScaling;
			model.refresh();
		}
	}

	public void setSnapToNote(boolean snapToNote) {
		model.setSnapToNote(snapToNote);
	}

	protected void createAccessory(Composite root) {
	}
	
	protected void onInit() {
	}

	protected void onPartControl() {
	}

	protected void doSave(IFile file) throws IOException {
		try (var wtr = Files.newByteChannel(file.getLocation().toPath(), StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
			model.afx().save(wtr);
		}
	}

	protected void execute(IUndoableOperation op) {
		System.out.println("UNDOABLE OP: " + op);
		op.addContext(getUndoContext());
		try {
			history.execute(op, null, null);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Cannot add frame.", e);
		}
	}

	protected IFile getFile() {
		return getEditorInput().getAdapter(IFile.class);
	}

	protected AFX load(IPath loc) throws PartInitException {
		try (var in = Files.newByteChannel(loc.toPath())) {
			return AFX.load(in);
		} catch (IOException e) {
			throw new PartInitException(Status.error("Failed to load AFB file.", e));
		}
	}

	protected void markDirty() {
		if (!dirty) {
			dirty = true;
			firePropertyChange(PROP_DIRTY);
		}
	}

	private <J extends Component> J configureSwingComponent(J jc) {
		if (Platform.getOS().equals("linux")) {
			jc.setBackground(widgetBackground);
			jc.setForeground(widgetForeground);
		}
		return jc;
	}

	private void createTable() {

		model.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (!model.isAdjusting()) {
					embed.getDisplay().execute(() -> {
						progress.setMaximum(model.getRowCount());
						markDirty();
					});
				}
			}
		});

		var mouseAdapter = new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				updateForMousePoint(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				updateForMousePoint(e);
			}

			private void updateForMousePoint(MouseEvent e) {
				// Only used in freestyle, otherwise normal table editing facilities are used
				if (freestyleMode) {
					var row = viewer.rowAtPoint(e.getPoint());
					if (row == -1 || e.getY() > viewer.getHeight() - (viewer.getRowHeight() / 2)) {
						if (e.getY() < viewer.getHeight() / 2) {
							return;
						} else {
							// Off bottom
							if (autoAdd) {
								addFrame();
								row = viewer.getRowCount() - 1;
							} else
								return;
						}
					}
					var col = viewer.columnAtPoint(e.getPoint());
					var cr = viewer.getCellRenderer(row, col);
					if (cr instanceof ValuePanel rndr) {
						var vbar = rndr.getValueBar();
						var rect = viewer.getCellRect(row, col, true);
						var relX = e.getX() - rect.x;
						var val = ValueBar.calcNewValue(relX, col == 4 && AFXEditor.this.logarithmicScaling, vbar.getMax(), rect.width);
						model.setValueAt(val, row, col);
					}
				}
			}

		};

		freestyleMode = FreestyleStateSourceProvider.isFreestyleModeEnabled();

		viewer = configureSwingComponent(new JTable());
		viewer.setColumnSelectionAllowed(false);
		viewer.setCellSelectionEnabled(false);
		viewer.setFillsViewportHeight(true);
		viewer.addMouseListener(mouseAdapter);
		viewer.addMouseMotionListener(mouseAdapter);
		if (Platform.getOS().equals("linux")) {
			viewer.setBackground(listBackground);
			viewer.setForeground(listForeground);
			viewer.setSelectionBackground(selectionBackground);
			viewer.setSelectionForeground(selectionForeground);
		}
		viewer.setRowHeight(24);
		viewer.setModel(model);
		setFreestyleMode(freestyleMode);
		setupTableSelection();
		configureSwingComponent(viewer.getTableHeader());

		setFixedColWidth(0, 64);
		setFixedColWidth(1, 40);
		setFixedColWidth(2, 40);
		setFixedColWidth(3, 64);
		setFixedColWidth(5, 64);
		setFixedColWidth(7, 64);

		var valCol = viewer.getColumnModel().getColumn(4);
		valCol.setCellRenderer(new ValueRenderer(65535, 0, true));
		valCol.setCellEditor(new ValueEditor(65535, 0, true));
		valCol = viewer.getColumnModel().getColumn(6);
		valCol.setCellRenderer(new ValueRenderer(255, 0, false));
		valCol.setCellEditor(new ValueEditor(255, 0, false));
		valCol = viewer.getColumnModel().getColumn(8);
		valCol.setCellRenderer(new ValueRenderer(15, 0, false));
		valCol.setCellEditor(new ValueEditor(15, 0, false));
	}

	private void markClean() {
		if (dirty) {
			dirty = false;
			firePropertyChange(PROP_DIRTY);
		}
	}

	private void setFixedColWidth(int colNo, int width) {
		var col = viewer.getColumnModel().getColumn(colNo);
		col.setMinWidth(width);
		col.setWidth(width);
		col.setMaxWidth(width);
	}

	private void setupTableSelection() {
		if (freestyleMode) {
			viewer.getSelectionModel().clearSelection();
			viewer.setRowSelectionAllowed(false);
			viewer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		} else {
			viewer.setRowSelectionAllowed(true);
			viewer.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}
		model.setFreestyleMode(freestyleMode);
	}

	private void setupUndo() {
		history = OperationHistoryFactory.getOperationHistory();
		history.setLimit(undoContext, 1000);
		var undoHandler = new UndoActionHandler(getSite(), undoContext);
		var redoHandler = new RedoActionHandler(getSite(), undoContext);
		var bars = getEditorSite().getActionBars();

		bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoHandler);
		bars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoHandler);
		bars.updateActionBars();
	}

}
