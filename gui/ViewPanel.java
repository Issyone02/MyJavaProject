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

        // --- 1. SORT ONLY TOOLBAR ---
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.add(new JLabel("Sort By:"));
        sortBox = new JComboBox<>(new String[]{"Default", "Title", "Year", "Type"});
        toolbar.add(sortBox);

        JButton resetBtn = new JButton("Reset View");
        toolbar.add(resetBtn);

        // --- 2. TABLE SETUP ---
        String[] columns = {"ID", "Type", "Title", "Author", "Year", "Available Copies"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);

        // Added Sorter for Global Search
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.setRowHeight(25);
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

    // Required for MainWindow Global Search
    public void applyFilter(String text) {
        if (text.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void handleSort() {
        String selection = (String) sortBox.getSelectedItem();
        if (selection == null || selection.equals("Default")) return;

        // Matches the JComboBox initialization: new String[]{"Default", "Title", "Year", "Type"}
        if ("Title".equals(selection)) manager.mergeSortByTitle();
        else if ("Year".equals(selection)) manager.selectionSortByYear();
        else if ("Type".equals(selection)) manager.sortByType();

        refreshTable(manager.getInventory());
    }

    public void refreshTable(List<LibraryItem> itemsToDisplay) {
        model.setRowCount(0);
        if (itemsToDisplay == null) return;
        for (LibraryItem item : itemsToDisplay) {
            model.addRow(new Object[]{item.getId(), item.getType(), item.getTitle(),
                    item.getAuthor(), item.getYear(), item.getAvailableCopies()});
        }
    }

    public void refreshTable() { refreshTable(manager.getInventory()); }
}