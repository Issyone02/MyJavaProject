package gui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared lazy table model used across all panels.
 * Backed by a live List of Object[] rows — JTable only calls getValueAt()
 * for visible rows, so off-screen data costs zero work.
 *
 * Usage:
 *   VirtualTableModel model = new VirtualTableModel(new String[]{"ID", "Name"});
 *   JTable table = new JTable(model);
 *   model.setRows(myRowList);  // replaces all data in O(1)
 */
public class VirtualTableModel extends AbstractTableModel {

    private final String[]     columns;
    private       List<Object[]> rows;

    public VirtualTableModel(String[] columns) {
        this.columns = columns;
        this.rows    = new ArrayList<>();
    }

    // Replaces all rows and repaints. O(1) — just swaps the list reference.
    public void setRows(List<Object[]> newRows) {
        this.rows = (newRows != null) ? newRows : new ArrayList<>();
        fireTableDataChanged();
    }

    public void clearRows() {
        this.rows = new ArrayList<>();
        fireTableDataChanged();
    }

    @Override public int    getRowCount()              { return rows.size(); }
    @Override public int    getColumnCount()            { return columns.length; }
    @Override public String getColumnName(int col)      { return columns[col]; }
    @Override public boolean isCellEditable(int r, int c) { return false; }

    // Only called for visible rows — this is the lazy evaluation point
    @Override
    public Object getValueAt(int row, int col) {
        if (row < 0 || row >= rows.size()) return "";
        Object[] rowData = rows.get(row);
        if (col < 0 || col >= rowData.length) return "";
        return rowData[col];
    }
}
