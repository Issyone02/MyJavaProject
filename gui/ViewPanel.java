package gui;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

/**
 * ViewPanel - Read-only inventory browser
 *
 * Provides users with a sortable, searchable view of all library items.
 * This panel is for browsing only - no editing or transactions.
 *
 * Features:
 * - Sort by Title, Year, or Type
 * - Real-time search filtering (via MainWindow search bar)
 * - Read-only table display
 * - Shows current stock availability
 */
public class ViewPanel extends JPanel {
    private final LibraryManager manager;              // Reference to data
    private final JTable table;                        // Display table
    private final DefaultTableModel model;             // Table data model
    private TableRowSorter<DefaultTableModel> sorter;  // For filtering/sorting
    private final JComboBox<String> sortBox;           // Sort selector

    /**
     * Constructor - Initializes the view panel
     *
     * @param manager Library manager instance
     */
    public ViewPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ==================== TOOLBAR SETUP ====================
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.add(new JLabel("Sort Inventory By:"));

        // Sort options: Default (ID), Title, Year, Type
        sortBox = new JComboBox<>(new String[]{"Default", "Title", "Year", "Type"});
        toolbar.add(sortBox);

        JButton resetBtn = new JButton("Reset View");
        toolbar.add(resetBtn);

        // ==================== TABLE SETUP ====================
        String[] columns = {"Item ID", "Category", "Title", "Author/Creator", "Year", "Stock Available"};

        // Non-editable table model (read-only view)
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);

        // Enable sorting and filtering
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);  // Fixed column order

        // ==================== LAYOUT ====================
        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ==================== EVENT LISTENERS ====================
        sortBox.addActionListener(e -> handleSort());

        // Reset button clears sort and shows all items
        resetBtn.addActionListener(e -> {
            sortBox.setSelectedIndex(0);
            refreshTable(manager.getInventory());
        });

        // Load initial data
        refreshTable(manager.getInventory());
    }

    /**
     * Applies search filter to table
     *
     * Called by MainWindow's global search bar.
     * Filters table rows based on search text (case-insensitive).
     *
     * @param text Search query
     */
    public void applyFilter(String text) {
        if (sorter == null) return;

        if (text == null || text.trim().isEmpty()) {
            sorter.setRowFilter(null);  // Clear filter, show all
        } else {
            // Case-insensitive regex filter
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    /**
     * Handles sort option selection
     *
     * Applies selected sorting algorithm:
     * - Title: Uses merge sort (academic requirement)
     * - Year: Uses selection sort (academic requirement)
     * - Type: Uses quicksort
     * - Default: Sorts by ID
     */
    private void handleSort() {
        String selection = (String) sortBox.getSelectedItem();
        if (selection == null) return;

        if ("Title".equals(selection)) {
            // Merge sort algorithm for Title
            manager.mergeSortByTitle();
        } else if ("Year".equals(selection)) {
            // Selection sort algorithm for Year
            manager.selectionSortByYear();
        } else if ("Type".equals(selection)) {
            // Sort by type (Book, Journal, Magazine)
            manager.sortByType();
        } else {
            // Default: sort by ID alphabetically
            manager.getInventory().sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
        }

        // Refresh display with sorted data
        refreshTable(manager.getInventory());
    }

    /**
     * Refreshes table with provided items
     *
     * @param itemsToDisplay List of items to show
     */
    public void refreshTable(List<LibraryItem> itemsToDisplay) {
        model.setRowCount(0);  // Clear existing rows
        if (itemsToDisplay == null) return;

        // Add each item as a table row
        for (LibraryItem item : itemsToDisplay) {
            model.addRow(new Object[]{
                    item.getId(),
                    item.getType().toUpperCase(),  // BOOK, JOURNAL, MAGAZINE
                    item.getTitle(),
                    item.getAuthor(),
                    item.getYear(),
                    item.getAvailableCopies()       // Current stock
            });
        }
    }

    /**
     * Refreshes table with current inventory
     * Convenience method for no-argument refresh
     */
    public void refreshTable() {
        refreshTable(manager.getInventory());
    }
}