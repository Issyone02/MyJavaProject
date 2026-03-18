package gui;

import controller.LibraryController;
import utils.GuiUtils;


import model.LibraryItem;
import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

/** Inventory management panel — add, edit, and delete library items. */
public class AdminPanel extends JPanel {

    private final LibraryController controller;
    private final int               currentUserId;
    private final JTable            table;
    private final VirtualTableModel model;
    private       TableRowSorter<VirtualTableModel> sorter;

    private final JTextField        titleIn, authIn, yearIn, totalIn;
    private final JComboBox<String> typeBox;

    /** Creates admin panel with controller and user ID. Initializes UI components. */
    public AdminPanel(LibraryController controller, int userId) {
        this.controller    = controller;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        boolean isAdmin = controller.isAdminUser(currentUserId);

        // ── Input form (GridBagLayout) ────────────────────────────────────────
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add New Item"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        typeBox = new JComboBox<>(new String[]{"Book", "Magazine", "Journal"});
        titleIn = new JTextField();
        authIn  = new JTextField();
        yearIn  = new JTextField();
        totalIn = new JTextField();

        Object[][] rows = {
            {"Type:",     typeBox},
            {"Title:",    titleIn},
            {"Author:",   authIn},
            {"Year:",     yearIn},
            {"Quantity:", totalIn}
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

        // ── Inventory table ───────────────────────────────────────────────────
        String[] cols = GuiUtils.CATALOGUE_COLUMNS;
        model  = new VirtualTableModel(cols);
        table  = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // ── Action buttons ────────────────────────────────────────────────────
        JButton editBtn   = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(new Color(255, 100, 100));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setOpaque(true);
        deleteBtn.setBorderPainted(false);
        editBtn.setToolTipText("Edit the selected item");
        deleteBtn.setToolTipText("Delete the selected item");
        // Edit and Delete are admin-only
        editBtn.setVisible(isAdmin);
        deleteBtn.setVisible(isAdmin);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actionBar.add(editBtn);
        actionBar.add(deleteBtn);

        add(inputPanel,             BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(actionBar,              BorderLayout.SOUTH);

        addBtn.addActionListener(e    -> handleAddItem());
        editBtn.addActionListener(e   -> handleEditItem());
        deleteBtn.addActionListener(e -> handleDeleteItem());

        refreshTable();
    }

    // ── Add ───────────────────────────────────────────────────────────────────

    /** Validates input fields and adds new item to catalogue via controller. */
    private void handleAddItem() {
        String title = titleIn.getText().trim();
        if (title.isEmpty()) { showValidation("Title cannot be empty.", titleIn); return; }

        String author = authIn.getText().trim();
        if (author.isEmpty()) { showValidation("Author cannot be empty.", authIn); return; }

        int year;
        try { year = Integer.parseInt(yearIn.getText().trim()); }
        catch (NumberFormatException ex) { showValidation("Invalid year.", yearIn); return; }
        int currentYear = java.time.Year.now().getValue();
        if (year < 1000 || year > currentYear + 1) {
            showValidation("Year must be 1000–" + (currentYear + 1) + ".", yearIn);
            return;
        }

        int total;
        try { total = Integer.parseInt(totalIn.getText().trim()); }
        catch (NumberFormatException ex) { showValidation("Invalid quantity.", totalIn); return; }
        if (total < 1) { showValidation("Quantity must be at least 1.", totalIn); return; }

        try {
            String type = (String) typeBox.getSelectedItem();
            // Factory: controller constructs the correct subtype and returns the generated ID
            String id = controller.createItem(
                    type, title, author, year, total, String.valueOf(currentUserId));
            refreshTable();
            titleIn.setText(""); authIn.setText(""); yearIn.setText(""); totalIn.setText("");
            JOptionPane.showMessageDialog(this, "Item added. ID: " + id,
                                          "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    /** Edits selected item with validation and password confirmation. */
    private void handleEditItem() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item to edit."); return; }
        int    mRow = table.convertRowIndexToModel(row);
        String id   = (String) model.getValueAt(mRow, 0);

        LibraryItem item = controller.getInventory().stream()
                .filter(i -> i.getId().equals(id)).findFirst().orElse(null);
        if (item == null) return;

        int borrowed = item.getTotalCopies() - item.getAvailableCopies();
        JTextField        tEdit    = new JTextField(item.getTitle());
        JTextField        aEdit    = new JTextField(item.getAuthor());
        JTextField        yEdit    = new JTextField(String.valueOf(item.getYear()));
        JTextField        qEdit    = new JTextField(String.valueOf(item.getTotalCopies()));
        JComboBox<String> typeEdit = new JComboBox<>(new String[]{"Book", "Journal", "Magazine"});
        typeEdit.setSelectedItem(item.getType());

        Object[] msg = {
            "ID: " + id, "Currently borrowed: " + borrowed,
            "Title:", tEdit, "Author:", aEdit, "Year:", yEdit,
            "Type:", typeEdit, "Quantity:", qEdit
        };
        if (JOptionPane.showConfirmDialog(this, msg, "Edit Item", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            try {
                int newTotal = Integer.parseInt(qEdit.getText().trim());
                if (newTotal < borrowed) {
                    JOptionPane.showMessageDialog(this,
                            "Cannot reduce quantity below " + borrowed + " (copies currently out on loan).");
                    return;
                }
                if (confirmPassword()) {
                    controller.updateItem(
                            String.valueOf(currentUserId), id,
                            (String) typeEdit.getSelectedItem(),
                            tEdit.getText().trim(), aEdit.getText().trim(),
                            Integer.parseInt(yEdit.getText().trim()), newTotal, "Edit");
                    refreshTable();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number entered.");
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /** Deletes selected item with reason selection and password confirmation. */
    private void handleDeleteItem() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item to delete."); return; }
        int    mRow      = table.convertRowIndexToModel(row);
        String itemId    = (String) model.getValueAt(mRow, 0);
        String itemTitle = (String) model.getValueAt(mRow, 2);

        // Check if copies are still out on loan
        int totalQty = (Integer) model.getValueAt(mRow, 4);
        int available = (Integer) model.getValueAt(mRow, 5);

        if (available < totalQty) {
            JOptionPane.showMessageDialog(this,
                    "The item can't be deleted cos copies are still in students possession.",
                    "Deletion Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[]          reasons   = {"Select", "Missing", "Damaged", "Obsolete"};
        JComboBox<String> reasonBox = new JComboBox<>(reasons);
        Object[]          message   = {"Item: " + itemTitle, "Reason:", reasonBox};

        if (JOptionPane.showConfirmDialog(this, message, "Delete Item", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            String reason = (String) reasonBox.getSelectedItem();
            if ("Select".equals(reason)) {
                JOptionPane.showMessageDialog(this, "Please select a deletion reason.");
                return;
            }
            if (confirmPassword()) {
                boolean removed = controller.removeItem(itemId, String.valueOf(currentUserId), reason);
                if (removed) {
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "Item removed.");
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Shows validation error message and focuses the problematic field. */
    private void showValidation(String msg, JTextField field) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
        field.requestFocus();
    }

    /** Refreshes the inventory table with current catalogue data. */
    public void refreshTable() {
        model.setRows(GuiUtils.buildCatalogueRows(controller));
    }

    /** Filters table to matching items; null clears the filter. */
    public void applySearch(List<LibraryItem> matches) {
        GuiUtils.applyItemFilter(sorter, matches);
    }

    /** Confirms admin password before sensitive operations. */
    private boolean confirmPassword() {
        return GuiUtils.confirmPassword(this, controller, currentUserId, "Confirm Password:");
    }
}