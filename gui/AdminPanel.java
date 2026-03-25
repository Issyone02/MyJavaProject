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
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an item to delete.");
            return;
        }

        int    mRow      = table.convertRowIndexToModel(row);
        String itemId    = (String) model.getValueAt(mRow, 0);
        String itemTitle = (String) model.getValueAt(mRow, 2);

        // --- Obtain authoritative total/available counts ---
        // Prefer reading from the controller's inventory (source of truth).
        Integer totalQty = null;
        Integer available = null;

        LibraryItem item = controller.getInventory().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst().orElse(null);

        if (item != null) {
            // Use the LibraryItem getters when available
            totalQty  = item.getTotalCopies();
            available = item.getAvailableCopies();
        } else {
            // Fallback: attempt to read from the table model (columns may vary)
            // Column indices here assume the model stores Available at 5 and Total at 6.
            // We parse defensively and handle parsing errors gracefully.
            try {
                Object totalObj = model.getValueAt(mRow, 6); // NOTE: GuiUtils.CATALOGUE_COLUMNS uses index 6 for Total
                Object availObj = model.getValueAt(mRow, 5); // and index 5 for Available

                // Defensive checks: getValueAt may return "" for missing cells; treat that as invalid.
                if (totalObj == null || "".equals(totalObj.toString().trim())
                        || availObj == null || "".equals(availObj.toString().trim())) {
                    JOptionPane.showMessageDialog(this,
                            "Unable to determine item quantities from the table. Deletion aborted.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Remove non-digit characters just in case and parse
                String totalStr = totalObj.toString().replaceAll("[^0-9-]", "");
                String availStr = availObj.toString().replaceAll("[^0-9-]", "");

                totalQty  = Integer.parseInt(totalStr);
                available = Integer.parseInt(availStr);
            } catch (NumberFormatException ex) {
                // If parsing fails, show an error and abort deletion to avoid accidental removal.
                JOptionPane.showMessageDialog(this,
                        "Unable to determine item quantities. Deletion aborted.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (ArrayIndexOutOfBoundsException ex) {
                // If model columns are not as expected, abort and ask admin to refresh.
                JOptionPane.showMessageDialog(this,
                        "Table model does not contain quantity columns. Please refresh and try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // If we still couldn't determine counts, abort.
        if (totalQty == null || available == null) {
            JOptionPane.showMessageDialog(this,
                    "Could not determine total/available counts for this item. Deletion aborted.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- Deletion policy: only allow deletion when ALL copies are present ---
        // Deny deletion if any copy is missing (i.e., available != total).
        if (!available.equals(totalQty)) {
            int missing = totalQty - available;
            JOptionPane.showMessageDialog(this,
                    "The item '" + itemTitle + "' cannot be deleted because " + missing +
                            " copy/copies are currently not in the library.",
                    "Deletion Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Proceed to ask for deletion reason and confirm
        String[]          reasons   = {"Select", "Missing", "Damaged", "Obsolete"};
        JComboBox<String> reasonBox = new JComboBox<>(reasons);
        Object[]          message   = {
                "Item: " + itemTitle,
                "All copies are currently in the library.",
                "Select Deletion Reason:",
                reasonBox
        };

        if (JOptionPane.showConfirmDialog(this, message, "Confirm Item Deletion", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {

            String reason = (String) reasonBox.getSelectedItem();
            if ("Select".equals(reason) || reason == null) {
                JOptionPane.showMessageDialog(this, "Please select a valid deletion reason.");
                return;
            }

            // Final password confirmation before deletion
            if (confirmPassword()) {
                boolean removed = controller.removeItem(itemId, String.valueOf(currentUserId), reason);
                if (removed) {
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "Item successfully removed from the database.");
                } else {
                    JOptionPane.showMessageDialog(this, "Database error: Could not remove item.");
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