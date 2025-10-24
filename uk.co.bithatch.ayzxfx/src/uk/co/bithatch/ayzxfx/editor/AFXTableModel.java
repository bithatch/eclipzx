package uk.co.bithatch.ayzxfx.editor;

import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.SwingUtilities.isEventDispatchThread;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.table.AbstractTableModel;

import uk.co.bithatch.ayzxfx.AYFXUtil;
import uk.co.bithatch.ayzxfx.ay.AFX;
import uk.co.bithatch.ayzxfx.ay.AFXFrame;

@SuppressWarnings("serial")
public class AFXTableModel extends AbstractTableModel {

	public interface UndoableChangeSupport {

		void update(int[] rowIndexes, Function<AFXFrame, AFXFrame> task);

	}

	private AFX afx;
	private boolean freestyleMode;
	private final UndoableChangeSupport undoableChangeSupport;
	private boolean adjusting;
	private boolean snapToNote;
	private final Supplier<int[]> selectionSupplier;
	private boolean actOnAll;

	public AFXTableModel(AFX afx, UndoableChangeSupport undoableChangeSupport, Supplier<int[]> selectionSupplier) {
		this.afx = afx;
		this.undoableChangeSupport = undoableChangeSupport;
		this.selectionSupplier = selectionSupplier;
	}

	@Override
	public int getRowCount() {
		return afx.size();
	}

	public boolean isFreestyleMode() {
		return freestyleMode;
	}

	public void setFreestyleMode(boolean freestyleMode) {
		if (this.freestyleMode != freestyleMode) {
			this.freestyleMode = freestyleMode;
			fireTableDataChanged();
		}
	}

	public boolean isSnapToNote() {
		return snapToNote;
	}

	public void setSnapToNote(boolean snapToNote) {
		this.snapToNote = snapToNote;
	}

	@Override
	public int getColumnCount() {
		return 9;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		var effect = afx.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return rowIndex;
		case 1:
			return effect.t();
		case 2:
			return effect.n();
		case 3:
			return String.format("%04x", effect.period());
		case 4:
			return effect.period();
		case 5:
			return String.format("%02x", effect.noise());
		case 6:
			return effect.noise();
		case 7:
			return String.format("%01x", effect.volume());
		case 8:
			return effect.volume();
		}
		return null;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "Pos";
		case 1:
			return "T";
		case 2:
			return "N";
		case 3:
			return "Pv";
		case 4:
			return "Period";
		case 5:
			return "Nv";
		case 6:
			return "Noise";
		case 7:
			return "Vv";
		case 8:
			return "Vol";
		default:
			return "???";
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
		case 3:
		case 5:
		case 7:
			return String.class;
		case 1:
		case 2:
			return Boolean.class;
		case 4:
		case 6:
		case 8:
			return Integer.class;
		default:
			return String.class;
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex != 0 && !freestyleMode;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (Objects.equals(aValue, getValueAt(rowIndex, columnIndex)))
			return;

		var rowIndices = isActOnAllSelection() ? selectionSupplier.get() : new int[] { rowIndex };

		switch (columnIndex) {
		case 1: {
			undoableChangeSupport.update(rowIndices, (effect) -> effect.update((Boolean) aValue, effect.n()));
			break;
		}
		case 2: {
			undoableChangeSupport.update(rowIndices, (effect) -> effect.update(effect.t(), (Boolean) aValue));
			break;
		}
		case 3: {
			undoableChangeSupport.update(rowIndices, (effect) -> effect
					.update(Integer.parseInt(String.valueOf(aValue), 16), effect.noise(), effect.volume()));
			break;
		}
		case 4: {
			var val = snapToNote ? AYFXUtil.snapToClosestNotePeriod((Integer) aValue) : (Integer) aValue;
			undoableChangeSupport.update(rowIndices, (effect) -> effect.update(val, effect.noise(), effect.volume()));
			break;
		}
		case 5: {
			var ival = Integer.parseInt(String.valueOf(aValue), 16);
			var val = snapToNote ? AYFXUtil.snapToClosestNotePeriod(ival) : ival;
			undoableChangeSupport.update(rowIndices, (effect) -> effect.update(effect.period(), val, effect.volume()));
			break;
		}
		case 6: {
			undoableChangeSupport.update(rowIndices,
					(effect) -> effect.update(effect.period(), (Integer) aValue, effect.volume()));
			break;
		}
		case 7: {
			undoableChangeSupport.update(rowIndices, (effect) -> effect.update(effect.period(), effect.noise(),
					Integer.parseInt(String.valueOf(aValue), 16)));
			break;
		}
		case 8: {
			undoableChangeSupport.update(rowIndices,
					(effect) -> effect.update(effect.period(), effect.noise(), (Integer) aValue));
			break;
		}
		}
	}

	public boolean isActOnAll() {
		return actOnAll;
	}

	public void setActOnAll(boolean actOnAll) {
		this.actOnAll = actOnAll;
	}

	private boolean isActOnAllSelection() {
		return !freestyleMode && actOnAll;
	}

	public void add(AFXFrame frame) {
		var rows = afx.frames().size();
		afx.add(frame);
		if (isEventDispatchThread())
			fireTableRowsInserted(rows, rows);
		else
			invokeLater(() -> fireTableRowsInserted(rows, rows));
	}

	public void add(int index, AFXFrame frame) {
		afx.add(index, frame);
		if (isEventDispatchThread())
			fireTableRowsInserted(index, index);
		else
			invokeLater(() -> fireTableRowsInserted(index, index));
	}

	public void remove(AFXFrame frame) {
		var idx = afx.frames().indexOf(frame);
		afx.remove(frame);
		if (isEventDispatchThread())
			fireTableRowsDeleted(idx, idx);
		else
			invokeLater(() -> fireTableRowsDeleted(idx, idx));
	}

	public AFXFrame remove(int index) {
		var effect = afx.remove(index);
		if (isEventDispatchThread())
			fireTableRowsDeleted(index, index);
		else
			invokeLater(() -> fireTableRowsDeleted(index, index));
		return effect;
	}

	public void afx(AFX afx) {
		this.afx = afx;
		if (isEventDispatchThread())
			refresh();
		else
			invokeLater(this::refresh);
	}

	public boolean isAdjusting() {
		return adjusting;
	}

	public AFX afx() {
		return afx;
	}

	public void refresh() {
		adjusting = true;
		try {
			fireTableDataChanged();
		} finally {
			adjusting = false;
		}
	}

}