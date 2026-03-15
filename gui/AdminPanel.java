package gui;

import controller.LibraryManager;
import model.*;
import utils.AuthManager;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Inventory management panel — add, edit, and delete library items.
 * Export/Import are in the File menu. Staff management and Logs are separate sub-tabs.
 */
public class AdminPanel extends JPanel {
    private final LibraryManager manager;
    private final int currentUserId;
    private final JTable table;
    private final VirtualTableModel model;
    private TableRowSorter<VirtualTableModel> sorter;

    private final JTextField titleIn, authIn, yearIn, totalIn;
    private final JComboBox<String> typeBox;


    public AdminPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        boolean isAdmin = AuthManager.isAdmin(currentUserId);

        // Input form — GridBagLayout
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add New Item"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        typeBox  = new JComboBox<>(new String[]{"Book", "Magazine", "Journal"});
        titleIn  = new JTextField();
        authIn   = new JTextField();
        yearIn   = new JTextField();
        totalIn  = new JTextField();

        Object[][] rows = {
            {"Type:", typeBox},
            {"Title:", titleIn}, {"Author:", authIn},
            {"Year:", yearIn}, {"Quantity:", totalIn}
        };
        for (int r = 0; r < rows.length; r++) {
            gbc.gridx = 0; gbc.gridy = r; gbc.weightx = 0;
            JLabel lbl = new JLabel((String) rows[r][0]);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            inputPanel.add(lbl, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            inputPanel.add((Component) rows[r][1], gbc);
        }

        JButton addBtn = new JButton("Add Item");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setToolTipText("Add a new item to the catalogue");
        gbc.gridx = 0; gbc.gridy = rows.length; gbc.gridwidth = 2; gbc.weightx = 1.0;
        inputPanel.add(addBtn, gbc);
        gbc.gridwidth = 1;

        // Table
        String[] cols = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};
        model = new VirtualTableModel(cols);
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Action buttons — below the table
        JButton editBtn   = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(new Color(255, 100, 100));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setOpaque(true);
        deleteBtn.setBorderPainted(false);

        editBtn.setToolTipText("Edit the selected item");
        deleteBtn.setToolTipText("Delete the selected item");

        editBtn.setVisible(isAdmin);
        deleteBtn.setVisible(isAdmin);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actionBar.add(editBtn);
        actionBar.add(deleteBtn);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actionBar, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> handleAddItem());
        editBtn.addActionListener(e -> handleEditItem());
        deleteBtn.addActionListener(e -> handleDeleteItem());

        refreshTable();
    }

    private void handleAddItem() {
        String title = titleIn.getText().trim();
        if (title.isEmpty()) { showValidation("Title cannot be empty.", titleIn); return; }
        String author = authIn.getText().trim();
        if (author.isEmpty()) { showValidation("Author cannot be empty.", authIn); return; }
        int year;
        try { year = Integer.parseInt(yearIn.getText().trim()); }
        catch (NumberFormatException ex) { showValidation("Invalid year.", yearIn); return; }
        int currentYear = java.time.Year.now().getValue();
        if (year < 1000 || year > currentYear + 1) { showValidation("Year must be 1000-" + (currentYear+1) + ".", yearIn); return; }
        int total;
        try { total = Integer.parseInt(totalIn.getText().trim()); }
        catch (NumberFormatException ex) { showValidation("Invalid quantity.", totalIn); return; }
        if (total < 1) { showValidation("Quantity must be at least 1.", totalIn); return; }

        try {
            String id   = utils.IDGenerator.generateID();
            String type = (String) typeBox.getSelectedItem();
            LibraryItem newItem = "Journal".equals(type) ? new Journal(id, title, author, year) :
                    "Magazine".equals(type) ? new Magazine(id, title, author, year) : new Book(id, title, author, year);
            newItem.setTotalCopies(total);
            newItem.setAvailableCopies(total);
            manager.addItem(newItem);
            manager.addLog(String.valueOf(currentUserId), "ADD", id + " - " + title);
            refreshTable();
            titleIn.setText(""); authIn.setText(""); yearIn.setText(""); totalIn.setText("");
            JOptionPane.showMessageDialog(this, "Item added. ID: " + id, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEditItem() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item."); return; }
        int mRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(mRow, 0);
        LibraryItem item = manager.getInventory().stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
        if (item == null) return;

        int borrowed = item.getTotalCopies() - item.getAvailableCopies();
        JTextField tEdit = new JTextField(item.getTitle());
        JTextField aEdit = new JTextField(item.getAuthor());
        JTextField yEdit = new JTextField(String.valueOf(item.getYear()));
        JTextField qEdit = new JTextField(String.valueOf(item.getTotalCopies()));
        JComboBox<String> typeEdit = new JComboBox<>(new String[]{"Book", "Journal", "Magazine"});
        typeEdit.setSelectedItem(item.getType());

        Object[] msg = {"ID: " + id, "Borrowed: " + borrowed, "Title:", tEdit, "Author:", aEdit,
                "Year:", yEdit, "Type:", typeEdit, "Quantity:", qEdit};
        if (JOptionPane.showConfirmDialog(this, msg, "Edit", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                int newTotal = Integer.parseInt(qEdit.getText().trim());
                if (newTotal < borrowed) { JOptionPane.showMessageDialog(this, "Cannot reduce below " + borrowed); return; }
                if (confirmPassword()) {
                    manager.updateItem(String.valueOf(currentUserId), id, (String) typeEdit.getSelectedItem(),
                            tEdit.getText().trim(), aEdit.getText().trim(),
                            Integer.parseInt(yEdit.getText().trim()), newTotal, "Edit");
                    refreshTable();
                }
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid numbers."); }
        }
    }

    private void handleDeleteItem() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item."); return; }
        int mRow = table.convertRowIndexToModel(row);
        String itemId = (String) model.getValueAt(mRow, 0);
        String itemTitle = (String) model.getValueAt(mRow, 2);

        String[] reasons = {"Select", "Missing", "Damaged", "Obsolete"};
        JComboBox<String> reasonBox = new JComboBox<>(reasons);
        Object[] message = {"Item: " + itemTitle, "Reason:", reasonBox};
        if (JOptionPane.showConfirmDialog(this, message, "Delete", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String reason = (String) reasonBox.getSelectedItem();
            if ("Select".equals(reason)) { JOptionPane.showMessageDialog(this, "Please select a reason."); return; }
            if (confirmPassword() && manager.removeItem(itemId)) {
                manager.addLog(String.valueOf(currentUserId), "DELETE", itemId + " - " + reason);
                refreshTable();
                JOptionPane.showMessageDialog(this, "Item removed.");
            }
        }
    }

    private void showValidation(String msg, JTextField field) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
        field.requestFocus();
    }

    private boolean confirmPassword() {
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, pf, "Confirm Password:", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return false;
        boolean ok = AuthManager.validate(currentUserId, new String(pf.getPassword()));
        if (!ok) JOptionPane.showMessageDialog(this, "Incorrect password.", "Access Denied", JOptionPane.ERROR_MESSAGE);
        return ok;
    }

    public void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (LibraryItem i : manager.getInventory()) {
            rows.add(new Object[]{ i.getId(), i.getType(), i.getTitle(), i.getAuthor(),
                i.getYear(), i.getAvailableCopies(), i.getTotalCopies() });
        }
        model.setRows(rows);
    }

    public void applySearch(java.util.List<model.LibraryItem> matches) {
        if (matches == null || sorter == null) {
            if (sorter != null) sorter.setRowFilter(null);
            return;
        }
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (model.LibraryItem m : matches) ids.add(m.getId());
        sorter.setRowFilter(new RowFilter<VirtualTableModel, Integer>() {
            @Override public boolean include(Entry<? extends VirtualTableModel, ? extends Integer> entry) {
                return ids.contains((String) entry.getValue(0));
            }
        });
    }
}
