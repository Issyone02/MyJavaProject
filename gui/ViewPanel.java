package gui;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class ViewPanel extends JPanel {
    private final LibraryManager manager;
    private final JTable table;
    private final DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private final JComboBox<String> sortBox;

    public ViewPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- 1. TOOLBAR SETUP ---
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.add(new JLabel("Sort Inventory By:"));
        sortBox = new JComboBox<>(new String[]{"Default", "Title", "Year", "Type"});
        toolbar.add(sortBox);

        JButton resetBtn = new JButton("Reset View");
        toolbar.add(resetBtn);

        // --- 2. TABLE SETUP ---
        String[] columns = {"Item ID", "Category", "Title", "Author/Creator", "Year", "Stock Available"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);

        // Initialize Sorter for Global Search Functionality
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- 3. EVENT LISTENERS ---
        sortBox.addActionListener(e -> handleSort());
        resetBtn.addActionListener(e -> {
            sortBox.setSelectedIndex(0);
            refreshTable(manager.getInventory());
        });

        refreshTable(manager.getInventory());
    }

    /**
     * Integrated with MainWindow's Search Bar.
     * Filters the table dynamically based on user input.
     */
    public void applyFilter(String text) {
        if (sorter == null) return;
        if (text == null || text.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Case-insensitive regex filter
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void handleSort() {
        String selection = (String) sortBox.getSelectedItem();
        if (selection == null) return;

        if ("Title".equals(selection)) {
            manager.mergeSortByTitle();
        } else if ("Year".equals(selection)) {
            manager.selectionSortByYear();
        } else if ("Type".equals(selection)) {
            manager.sortByType();
        } else {
            // Default sorting logic (usually by ID)
            manager.getInventory().sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        }

        refreshTable(manager.getInventory());
    }

    public void refreshTable(List<LibraryItem> itemsToDisplay) {
        model.setRowCount(0);
        if (itemsToDisplay == null) return;
        for (LibraryItem item : itemsToDisplay) {
            model.addRow(new Object[]{
                    item.getId(),
                    item.getType().toUpperCase(),
                    item.getTitle(),
                    item.getAuthor(),
                    item.getYear(),
                    item.getAvailableCopies()
            });
        }
    }

    public void refreshTable() {
        refreshTable(manager.getInventory());
    }
}