package gui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Lazy table model backed by a List of Object[] rows.
 * JTable only calls getValueAt() for visible rows — off-screen data is free.
 */
public class VirtualTableModel extends AbstractTableModel {

    private final String[]   columns;
    private List<Object[]>   rows;

    /** Creates table model with specified column names. */
    public VirtualTableModel(String[] columns) {
        this.columns = columns;
        this.rows    = new ArrayList<>();
    }

    /** Replaces all rows and triggers repaint in O(1). */
    public void setRows(List<Object[]> newRows) {
        this.rows = (newRows != null) ? newRows : new ArrayList<>();
        fireTableDataChanged();
    }

    /** Returns number of rows in the table. */
    @Override public int     getRowCount()                { return rows.size(); }
    
    /** Returns number of columns in the table. */
    @Override public int     getColumnCount()             { return columns.length; }
    
    /** Returns column name at specified index. */
    @Override public String  getColumnName(int col)       { return columns[col]; }
    
    /** Returns false - table cells are not editable. */
    @Override public boolean isCellEditable(int r, int c) { return false; }

    /** Returns value at specified row and column. Returns empty string for invalid indices. */
    @Override
    public Object getValueAt(int row, int col) {
        if (row < 0 || row >= rows.size()) return "";
        Object[] r = rows.get(row);
        return (col >= 0 && col < r.length) ? r[col] : "";
    }
}
