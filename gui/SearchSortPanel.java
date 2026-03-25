package gui;

import controller.LibraryController;
import utils.GuiUtils;


import controller.SearchEngine;
import model.LibraryItem;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

/** Search and Sort tab — sort the catalogue and see which search algorithm was used. */
public class SearchSortPanel extends JPanel {

    // CATALOGUE_COLUMNS doubles as the sort-field list — column index == field index
    private static final String[] SORT_ALGORITHMS = {
        "Insertion Sort", "Merge Sort", "Quick Sort"
    };

    private final LibraryController controller;
    private final JTable            table;
    private final VirtualTableModel tableModel;
    private final TableRowSorter<VirtualTableModel> sorter;

    private final JComboBox<String> sortFieldCombo;
    private final JComboBox<String> sortAlgoCombo;

    // Shows which search algorithm was used; blank until the user performs a search
    private final JLabel algorithmHintLabel;

    // Snapshot of item IDs in load order — used by the Reset button
    private final List<String> originalOrder;

    public SearchSortPanel(LibraryController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Capture the load order before any sort can change it
        originalOrder = controller.getInventory().stream()
                .map(LibraryItem::getId)
                .collect(Collectors.toList());

        // ── Toolbar ───────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));

        toolbar.add(new JLabel("Sort Field:"));
        sortFieldCombo = new JComboBox<>(GuiUtils.CATALOGUE_COLUMNS);
        sortFieldCombo.setSelectedItem("Title");
        sortFieldCombo.setToolTipText("Choose which column the algorithm will sort by");
        toolbar.add(sortFieldCombo);

        toolbar.add(new JLabel("Algorithm:"));
        sortAlgoCombo = new JComboBox<>(SORT_ALGORITHMS);
        sortAlgoCombo.setSelectedItem("Merge Sort");
        sortAlgoCombo.setToolTipText(
                "Insertion Sort: O(n²) worst, fast on nearly-sorted data  |  " +
                "Merge Sort: O(n log n) always, stable  |  " +
                "Quick Sort: O(n log n) average, in-place");
        toolbar.add(sortAlgoCombo);

        JButton sortBtn  = new JButton("Sort");
        JButton resetBtn = new JButton("Reset");
        sortBtn.setToolTipText("Sort the catalogue using the chosen field and algorithm");
        resetBtn.setToolTipText("Restore the catalogue to the order it was in when the app loaded");
        toolbar.add(sortBtn);
        toolbar.add(resetBtn);

        // ── Algorithm hint label ──────────────────────────────────────────────
        algorithmHintLabel = new JLabel(" ");   // space keeps the row height stable
        algorithmHintLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        algorithmHintLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

        JPanel north = new JPanel(new BorderLayout());
        north.add(toolbar,            BorderLayout.NORTH);
        north.add(algorithmHintLabel, BorderLayout.SOUTH);

        // ── Catalogue table ───────────────────────────────────────────────────
        tableModel = new VirtualTableModel(GuiUtils.CATALOGUE_COLUMNS);
        table      = new JTable(tableModel);
        sorter     = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        // Disable Swing's built-in header-click sort — our MouseListener below
        // replaces it so the data and the view stay in sync for SearchEngine
        for (int c = 0; c < GuiUtils.CATALOGUE_COLUMNS.length; c++)
            sorter.setSortable(c, false);

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

        add(north,                  BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Clicking a column header: update the Sort Field combo and run the sort
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col >= 0 && col < GuiUtils.CATALOGUE_COLUMNS.length) {
                    sortFieldCombo.setSelectedItem(GuiUtils.CATALOGUE_COLUMNS[col]);
                    handleSort();
                }
            }
        });

        sortBtn.addActionListener(e -> handleSort());

        resetBtn.addActionListener(e -> {
            controller.restoreOrder(originalOrder);
            sortFieldCombo.setSelectedItem("Title");
            sortAlgoCombo.setSelectedItem("Merge Sort");
            algorithmHintLabel.setText(" ");
            algorithmHintLabel.setForeground(Color.BLACK);
            refreshTable();
        });

        refreshTable();
    }

    // Reads the combo boxes and delegates the sort to the controller
    private void handleSort() {
        String field = (String) sortFieldCombo.getSelectedItem();
        String algo  = (String) sortAlgoCombo.getSelectedItem();
        if (field == null || algo == null) return;

        switch (algo) {
            case "Insertion Sort": controller.insertionSortBy(field); break;
            case "Merge Sort":     controller.mergeSortBy(field);     break;
            case "Quick Sort":     controller.quickSortBy(field);     break;
        }

        refreshTable();
    }

    // Reloads the catalogue and clears any active search filter
    public void refreshTable() {
        tableModel.setRows(GuiUtils.buildCatalogueRows(controller));
        sorter.setRowFilter(null);
    }


    
     // SearchEngine uses binary search on the sorted field
     
    public void applySearch(List<LibraryItem> matches) {
        if (matches == null) {
            sorter.setRowFilter(null);
            algorithmHintLabel.setText(" ");
            algorithmHintLabel.setForeground(Color.BLACK);
            return;
        }

        GuiUtils.applyItemFilter(sorter, matches);

        // Show the algorithm that applied at the moment of the search
        String sortedField = SearchEngine.detectSortedField(controller.getInventory());
        if (sortedField != null) {
            algorithmHintLabel.setText(
                    "\u2714 Items sorted \u2014 Binary Search used " +
                    "(sorted by " + sortedField + "; other fields searched linearly)");
            algorithmHintLabel.setForeground(new Color(0, 120, 0));
        } else {
            algorithmHintLabel.setText(
                    "\u26A0 Items not sorted \u2014 Linear Search used " +
                    "(sort the catalogue first for faster searching)");
            algorithmHintLabel.setForeground(new Color(170, 80, 0));
        }
    }
}
