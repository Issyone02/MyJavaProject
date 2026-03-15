package gui;

import controller.LibraryManager;
import controller.SearchEngine;
import model.LibraryItem;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Search & Sort tab. Click a column header (Title/Author/Year) to select
 * the sort field, pick an algorithm from the dropdown, then press Sort.
 * An info label shows the algorithm complexity and active search mode.
 */
public class SearchSortPanel extends JPanel {
    private final LibraryManager manager;
    private final JTable table;
    private final VirtualTableModel tableModel;
    private final TableRowSorter<VirtualTableModel> sorter;

    private final JComboBox<String> sortAlgoBox;
    private final JLabel sortFieldLabel;

    private String selectedSortField = "Title";

    private static final String[] SORT_ALGORITHMS = {"Insertion Sort", "Merge Sort", "Quick Sort"};

    public SearchSortPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));

        toolbar.add(new JLabel("Sort Field:"));
        sortFieldLabel = new JLabel("Title");
        sortFieldLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sortFieldLabel.setForeground(new Color(0, 102, 180));
        toolbar.add(sortFieldLabel);

        toolbar.add(new JLabel("  Algorithm:"));
        sortAlgoBox = new JComboBox<>(SORT_ALGORITHMS);
        sortAlgoBox.setSelectedItem("Merge Sort");
        toolbar.add(sortAlgoBox);

        JButton sortBtn = new JButton("Sort");
        sortBtn.setToolTipText("Apply the selected algorithm");
        toolbar.add(sortBtn);

        JButton resetBtn = new JButton("Reset");
        resetBtn.setToolTipText("Restore original order");
        toolbar.add(resetBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};
        tableModel = new VirtualTableModel(cols);
        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 250));
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });

        int[] widths = {65, 75, 220, 160, 55, 75, 55};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Click column header to select sort field
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                String colName = table.getColumnName(col);
                switch (colName) {
                    case "Title"  -> selectedSortField = "Title";
                    case "Author" -> selectedSortField = "Author";
                    case "Year"   -> selectedSortField = "Year";
                    default -> { return; }
                }
                sortFieldLabel.setText(selectedSortField);
            }
        });

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Events
        sortBtn.addActionListener(e -> handleSort());
        resetBtn.addActionListener(e -> {
            selectedSortField = "Title";
            sortFieldLabel.setText("Title");
            sortAlgoBox.setSelectedItem("Merge Sort");
            refreshTable();
        });

        refreshTable();
    }

    private void handleSort() {
        String algo = (String) sortAlgoBox.getSelectedItem();
        if (algo == null) return;

        switch (algo) {
            case "Insertion Sort" -> manager.insertionSortBy(selectedSortField);
            case "Merge Sort"     -> manager.mergeSortBy(selectedSortField);
            case "Quick Sort"     -> manager.quickSortBy(selectedSortField);
        }

        refreshTable();
    }

    public void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (LibraryItem i : manager.getInventory()) {
            rows.add(new Object[]{
                i.getId(), i.getType(), i.getTitle(), i.getAuthor(),
                i.getYear(), i.getAvailableCopies(), i.getTotalCopies()
            });
        }
        tableModel.setRows(rows);
        if (sorter != null) sorter.setRowFilter(null);
    }

    // Called by global search — uses SearchEngine to filter
    public void applySearch(List<LibraryItem> matches) {
        if (matches == null) { refreshTable(); return; }
        Set<String> matchIds = matches.stream().map(LibraryItem::getId).collect(Collectors.toSet());
        sorter.setRowFilter(new RowFilter<VirtualTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends VirtualTableModel, ? extends Integer> entry) {
                String id = (String) entry.getValue(0);
                return matchIds.contains(id);
            }
        });
    }
}
