package gui;

import model.*;
import utils.FileHandler;
import utils.AuthManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.io.File;

/**
 * AdminPanel - Administrative control panel for library inventory management
 *
 * This panel provides administrators with comprehensive tools for managing the library:
 * - Add new items to inventory (books, magazines, journals)
 * - Edit existing item details
 * - Delete items from inventory
 * - Export/import library data
 * - Manage system users
 * - View system audit logs
 *
 * Access to administrative functions is controlled based on user permissions.
 * Only super administrators can perform destructive operations like deletions.
 */
public class AdminPanel extends JPanel {
    // Core components
    private final LibraryManager manager;          // Reference to business logic controller
    private final int currentUserId;               // ID of currently logged-in user
    private final JTable table;                    // Table displaying inventory items
    private final DefaultTableModel model;         // Table data model
    private TableRowSorter<DefaultTableModel> sorter;  // For filtering/searching table

    // Input fields for adding new items
    private final JTextField titleIn, authIn, yearIn, totalIn;  // Title, author, year, quantity
    private final JComboBox<String> typeBox;       // Item type selector (Book/Magazine/Journal)
    private final JLabel idDisplayLabel;           // Shows auto-generated item ID

    // Action buttons - visibility controlled by user permissions
    private final JButton deleteBtn, editItemBtn, historyBtn, manageUsersBtn, exportBtn, importBtn, addUserBtn;

    /**
     * Constructor - Initializes the admin panel with all controls
     *
     * @param manager Library manager instance for data operations
     * @param userId  Current user's ID for permission checking and logging
     */
    public AdminPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Check if current user has super admin privileges
        // This determines which buttons will be visible
        boolean isSuperAdmin = AuthManager.isSuperAdmin(currentUserId);

        // ==================== INPUT PANEL ====================
        // Panel for adding new inventory items
        JPanel inputPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add New Inventory Item"));

        // Generated ID display (read-only, auto-generated on add)
        inputPanel.add(new JLabel("Generated ID:"));
        idDisplayLabel = new JLabel("Auto-Generated");
        idDisplayLabel.setForeground(new Color(0, 102, 204));  // Blue color
        inputPanel.add(idDisplayLabel);

        // Item type selector (Book, Magazine, or Journal)
        inputPanel.add(new JLabel("Type:"));
        typeBox = new JComboBox<>(new String[]{"Book", "Magazine", "Journal"});
        inputPanel.add(typeBox);

        // Title input field
        inputPanel.add(new JLabel("Title:"));
        titleIn = new JTextField();
        inputPanel.add(titleIn);

        // Author input field
        inputPanel.add(new JLabel("Author:"));
        authIn = new JTextField();
        inputPanel.add(authIn);

        // Publication year input
        inputPanel.add(new JLabel("Year:"));
        yearIn = new JTextField();
        inputPanel.add(yearIn);

        // Total quantity input
        inputPanel.add(new JLabel("Total Quantity:"));
        totalIn = new JTextField();
        inputPanel.add(totalIn);

        // Add button to submit new item
        JButton addBtn = new JButton("Add Item to Library");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(new JLabel(""));  // Spacer
        inputPanel.add(addBtn);

        // ==================== ADMIN ACTION BAR ====================
        // Toolbar with admin-only functions
        JPanel adminActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // Edit item button
        editItemBtn = new JButton("Edit Item");

        // Delete button (styled in red for danger)
        deleteBtn = new JButton("🗑 Delete Item");
        deleteBtn.setBackground(new Color(255, 100, 100));  // Red background
        deleteBtn.setForeground(Color.WHITE);                // White text
        deleteBtn.setOpaque(true);
        deleteBtn.setBorderPainted(false);

        // Export/Import data buttons
        exportBtn = new JButton("Export Data");
        importBtn = new JButton("Import Data");

        // User management buttons
        manageUsersBtn = new JButton("View Users");
        addUserBtn = new JButton("👤 Add New User");
        addUserBtn.setBackground(new Color(76, 175, 80));  // Green background
        addUserBtn.setForeground(Color.WHITE);              // White text
        addUserBtn.setOpaque(true);
        addUserBtn.setBorderPainted(false);

        // System audit log button (visible to all users)
        historyBtn = new JButton("📜 System Audit Log");

        // Control button visibility based on user permissions
        // Only super admins can see destructive/sensitive operations
        editItemBtn.setVisible(isSuperAdmin);
        deleteBtn.setVisible(isSuperAdmin);
        exportBtn.setVisible(isSuperAdmin);
        importBtn.setVisible(isSuperAdmin);
        manageUsersBtn.setVisible(isSuperAdmin);
        addUserBtn.setVisible(isSuperAdmin);

        // Add all buttons to the action bar
        adminActionBar.add(editItemBtn);
        adminActionBar.add(deleteBtn);
        adminActionBar.add(new JSeparator(SwingConstants.VERTICAL));  // Visual separator
        adminActionBar.add(exportBtn);
        adminActionBar.add(importBtn);
        adminActionBar.add(manageUsersBtn);
        adminActionBar.add(addUserBtn);
        adminActionBar.add(historyBtn);

        // ==================== TABLE SETUP ====================
        // Table to display all inventory items
        String[] cols = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};

        // Create non-editable table model (prevents direct cell editing)
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;  // All cells read-only
            }
        };

        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);  // Enable sorting and filtering

        // ==================== LAYOUT ASSEMBLY ====================
        // Container for input panel and action bar
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(inputPanel, BorderLayout.CENTER);
        northContainer.add(adminActionBar, BorderLayout.SOUTH);

        // Add panels to main layout
        add(northContainer, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ==================== EVENT LISTENERS ====================
        // Attach handlers to all buttons
        addBtn.addActionListener(e -> handleAddItem());
        editItemBtn.addActionListener(e -> handleEditItem());
        deleteBtn.addActionListener(e -> handleDeleteItem());
        exportBtn.addActionListener(e -> handleExport());
        importBtn.addActionListener(e -> handleImport());
        manageUsersBtn.addActionListener(e -> showUserManagementPopup());
        addUserBtn.addActionListener(e -> showAddUserDialog());
        historyBtn.addActionListener(e -> showHistoryPopup());

        // Load initial data into table
        refreshTable();
    }

    /**
     * Handles deletion of a library item
     *
     * Process:
     * 1. Verify an item is selected
     * 2. Request deletion reason (for audit trail)
     * 3. Confirm with admin password
     * 4. Delete item and update display
     *
     * Safety features:
     * - Cannot delete items currently on loan
     * - Requires reason selection
     * - Requires password confirmation
     * - Logs all deletions
     */
    private void handleDeleteItem() {
        // Get selected row index from table
        int selectedRow = table.getSelectedRow();

        // Validation: Ensure an item is selected
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete.");
            return;
        }

        // Convert view index to model index (needed when table is sorted)
        int modelRow = table.convertRowIndexToModel(selectedRow);

        // Extract item details from table
        String itemId = (String) model.getValueAt(modelRow, 0);
        String itemTitle = (String) model.getValueAt(modelRow, 2);

        // Deletion reason selection (required for audit trail)
        String[] reasons = {"Select Reason", "Missing", "Damaged", "Obsolete"};
        JComboBox<String> reasonBox = new JComboBox<>(reasons);
        Object[] message = {"Item: " + itemTitle, "Select reason for deletion:", reasonBox};

        // Show confirmation dialog with reason selector
        int option = JOptionPane.showConfirmDialog(this, message, "Confirm Deletion", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String selectedReason = (String) reasonBox.getSelectedItem();

            // Validate that a real reason was selected (not the default prompt)
            if (!selectedReason.equals("Select Reason")) {
                // Require admin password confirmation for security
                if (confirmAdminPassword()) {
                    // Attempt deletion (manager handles state saving automatically)
                    boolean success = manager.removeItem(itemId);

                    if (success) {
                        // Log the deletion for audit trail
                        manager.addLog(String.valueOf(currentUserId), "DELETE_ITEM",
                                "Removed " + itemId + " Reason: " + selectedReason);

                        // Update table display
                        refreshTable();

                        // Show success confirmation
                        JOptionPane.showMessageDialog(this, "Item '" + itemTitle + "' removed successfully.");
                    } else {
                        // Deletion failed (item not found or currently borrowed)
                        JOptionPane.showMessageDialog(this,
                                "Deletion Failed: Either item not found or it is currently BORROWED.",
                                "System Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                // User didn't select a valid reason
                JOptionPane.showMessageDialog(this, "Please select a valid reason.");
            }
        }
    }

    /**
     * Handles adding a new library item
     *
     * Process:
     * 1. Validate all input fields
     * 2. Generate unique item ID
     * 3. Create appropriate item object (Book/Magazine/Journal)
     * 4. Add to inventory
     * 5. Clear form and refresh display
     *
     * Validation includes:
     * - All fields must be filled
     * - Year must be numeric
     * - Quantity must be numeric and positive
     */
    private void handleAddItem() {
        try {
            // Generate unique ID for new item
            String id = utils.IDGenerator.generateID();

            // Get and trim input values
            String title = titleIn.getText().trim();
            String author = authIn.getText().trim();
            int year = Integer.parseInt(yearIn.getText().trim());      // Parse year (throws exception if invalid)
            int total = Integer.parseInt(totalIn.getText().trim());    // Parse quantity
            String type = (String) typeBox.getSelectedItem();

            // Create appropriate item type based on selection
            // Uses ternary operator for compact type selection
            LibraryItem newItem = "Journal".equals(type) ? new Journal(id, title, author, year) :
                    "Magazine".equals(type) ? new Magazine(id, title, author, year) :
                            new Book(id, title, author, year);

            // Set inventory quantities (all copies initially available)
            newItem.setTotalCopies(total);
            newItem.setAvailableCopies(total);

            // Add item to inventory (manager handles state saving)
            manager.addItem(newItem);

            // Log the addition for audit trail
            manager.addLog(String.valueOf(currentUserId), "ADD_ITEM", "Added " + type + ": " + title);

            // Clear input form and refresh table
            clearInputFields();
            refreshTable();

        } catch (Exception ex) {
            // Handle any parsing errors (invalid year/quantity)
            JOptionPane.showMessageDialog(this, "Invalid input. Please check all fields.");
        }
    }

    /**
     * Handles editing an existing library item
     *
     * Process:
     * 1. Verify item is selected
     * 2. Load current item details
     * 3. Show edit dialog with pre-filled values
     * 4. Validate new values
     * 5. Confirm with admin password
     * 6. Update item
     *
     * Business rules:
     * - Cannot reduce total quantity below currently borrowed count
     * - Year must be valid integer
     * - All fields required
     */
    private void handleEditItem() {
        // Get selected row
        int row = table.getSelectedRow();

        // Validation: Ensure item is selected
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to edit.");
            return;
        }

        // Convert to model index and get item ID
        int mRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(mRow, 0);

        // Find the actual item object in inventory
        LibraryItem item = manager.getInventory().stream()
                .filter(i -> i.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (item != null) {
            // Calculate how many copies are currently borrowed
            // This is important because we can't reduce total below borrowed count
            int currentlyBorrowed = item.getTotalCopies() - item.getAvailableCopies();

            // Create input fields pre-filled with current values
            JTextField tEdit = new JTextField(item.getTitle());
            JTextField aEdit = new JTextField(item.getAuthor());
            JTextField yEdit = new JTextField(String.valueOf(item.getYear()));
            JTextField qEdit = new JTextField(String.valueOf(item.getTotalCopies()));
            JComboBox<String> typeEdit = new JComboBox<>(new String[]{"Book", "Journal", "Magazine"});
            typeEdit.setSelectedItem(item.getType());

            // Create dialog message array with labels and fields
            Object[] msg = {
                    "Item ID: " + id,
                    "Current Borrowed: " + currentlyBorrowed,  // Show constraint
                    "Title:", tEdit,
                    "Author:", aEdit,
                    "Year:", yEdit,
                    "Type:", typeEdit,
                    "New Total Quantity:", qEdit
            };

            // Show edit dialog
            int result = JOptionPane.showConfirmDialog(this, msg, "Edit Item Details", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    // Parse new total quantity
                    int newTotal = Integer.parseInt(qEdit.getText().trim());

                    // BUSINESS RULE: Cannot reduce total below borrowed count
                    // This would result in negative available copies
                    if (newTotal < currentlyBorrowed) {
                        JOptionPane.showMessageDialog(this, "Cannot reduce Total below " + currentlyBorrowed);
                        return;
                    }

                    // Require admin password for confirmation
                    if (confirmAdminPassword()) {
                        // Update item (manager handles state saving)
                        manager.updateItem(String.valueOf(currentUserId), id, (String)typeEdit.getSelectedItem(),
                                tEdit.getText().trim(), aEdit.getText().trim(),
                                Integer.parseInt(yEdit.getText().trim()), newTotal, "Manual Edit");

                        // Refresh display
                        refreshTable();
                        JOptionPane.showMessageDialog(this, "Item updated successfully!");
                    }
                } catch (NumberFormatException ex) {
                    // Handle invalid numeric input
                    JOptionPane.showMessageDialog(this, "Error: numeric fields required.");
                }
            }
        }
    }

    /**
     * Handles importing library data from a backup file
     *
     * Process:
     * 1. Confirm with admin password
     * 2. Show file chooser dialog
     * 3. Load data from selected file
     * 4. Replace current inventory
     * 5. Save state for undo capability
     *
     * WARNING: This operation replaces all current inventory data
     * Make sure to have a backup before importing
     */
    private void handleImport() {
        // Require admin password before proceeding
        if (!confirmAdminPassword()) return;

        // Show file selection dialog
        JFileChooser c = new JFileChooser();
        if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Load data from selected file
            Object[] data = FileHandler.importBackup(c.getSelectedFile());

            if (data != null) {
                // IMPORTANT: Save state before clearing to enable undo
                manager.saveState(false);

                // Clear current inventory
                manager.getInventory().clear();

                // Add all items from imported data
                manager.getInventory().addAll((List<LibraryItem>) data[0]);

                // Refresh table display
                refreshTable();
            }
        }
    }

    /**
     * Shows dialog to add a new system user
     *
     * Process:
     * 1. Collect user information (name, nickname, password)
     * 2. Set administrator privilege if selected
     * 3. Confirm with admin password
     * 4. Create user account
     * 5. Show generated user ID
     *
     * Only super administrators can add new users
     */
    private void showAddUserDialog() {
        // Create input fields for user information
        JTextField nameField = new JTextField();
        JTextField nickField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JCheckBox adminCheck = new JCheckBox("Assign as Administrator");

        // Create dialog message array
        Object[] message = {"Full Name:", nameField, "Nickname:", nickField, "Password:", passField, "", adminCheck};

        // Show input dialog
        int option = JOptionPane.showConfirmDialog(this, message, "Register Staff", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            // Require admin password confirmation
            if (confirmAdminPassword()) {
                // Generate new user ID (starts at 101 and increments)
                int newId = AuthManager.getAllUsers().size() + 101;

                // Create the user account
                AuthManager.addUser(newId, new String(passField.getPassword()),
                        nickField.getText().trim(), adminCheck.isSelected());

                // Log user creation for audit trail
                manager.addLog(String.valueOf(currentUserId), "USER_CREATED", "Added: " + nickField.getText());

                // Show the new user ID to the administrator
                JOptionPane.showMessageDialog(this, "User ID: " + newId);
            }
        }
    }

    /**
     * Prompts for admin password confirmation
     *
     * Used as a security measure before performing sensitive operations:
     * - Deleting items
     * - Editing items
     * - Managing users
     * - Importing data
     *
     * @return true if password is correct, false otherwise
     */
    private boolean confirmAdminPassword() {
        // Create password input field
        JPasswordField pf = new JPasswordField();

        // Show password dialog
        int res = JOptionPane.showConfirmDialog(this, pf, "Admin Password:", JOptionPane.OK_CANCEL_OPTION);

        // Validate password against current user's credentials
        return (res == JOptionPane.OK_OPTION) && AuthManager.validate(currentUserId, new String(pf.getPassword()));
    }

    /**
     * Handles data export functionality
     *
     * Two export formats available:
     * 1. Text File: Human-readable report format
     * 2. System Data: Complete backup (can be re-imported)
     *
     * Process:
     * 1. Select export format
     * 2. Choose save location
     * 3. Export data to file
     */
    private void handleExport() {
        // Present export format options
        String[] options = {"Text File", "System Data"};
        int choice = JOptionPane.showOptionDialog(this, "Select Export Format:", "Export",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        // Exit if user cancels
        if (choice == -1) return;

        // Show save dialog
        JFileChooser c = new JFileChooser();
        if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = c.getSelectedFile();

            // Export based on selected format
            if (choice == 0) {
                // Text file export (human-readable)
                FileHandler.exportToText(manager.getInventory(), manager.getStudents(), file);
            } else {
                // System data export (complete backup)
                FileHandler.exportBackup(manager.getInventory(), manager.getStudents(), file);
            }
        }
    }

    /**
     * Displays system audit log in a dialog
     *
     * Shows all logged activities including:
     * - Who performed the action
     * - When it occurred
     * - What action was performed
     * - Additional details
     *
     * Available to all users for transparency
     */
    private void showHistoryPopup() {
        // Create modal dialog
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Audit Log", true);
        d.setLayout(new BorderLayout());

        // Define table columns
        String[] hCols = {"Operator", "Time", "Action", "Details"};
        DefaultTableModel hMod = new DefaultTableModel(hCols, 0);

        // Load all log entries into table
        for (SystemLog log : manager.getSystemLogs()) {
            hMod.addRow(new Object[]{log.getUserId(), log.getTimestamp(), log.getAction(), log.getDetails()});
        }

        // Add table to dialog
        d.add(new JScrollPane(new JTable(hMod)), BorderLayout.CENTER);

        // Set dialog properties and display
        d.setSize(700, 450);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    /**
     * Clears all input fields in the add item form
     * Called after successful item addition
     */
    private void clearInputFields() {
        titleIn.setText("");
        authIn.setText("");
        yearIn.setText("");
        totalIn.setText("");
    }

    /**
     * Shows user management dialog
     * Requires admin password confirmation
     */
    private void showUserManagementPopup() {
        if (confirmAdminPassword()) {
            new UserManagementDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    currentUserId, manager).setVisible(true);
        }
    }

    /**
     * Refreshes the inventory table with current data
     * Clears existing rows and reloads from manager
     * Called after any data modification
     */
    public void refreshTable() {
        // Clear all existing rows
        model.setRowCount(0);

        // Add a row for each inventory item
        for (LibraryItem i : manager.getInventory()) {
            model.addRow(new Object[]{
                    i.getId(),
                    i.getType(),
                    i.getTitle(),
                    i.getAuthor(),
                    i.getYear(),
                    i.getAvailableCopies(),
                    i.getTotalCopies()
            });
        }
    }

    /**
     * Applies search filter to the table
     * Called by global search in MainWindow
     *
     * @param text Search text to filter by (case-insensitive)
     */
    public void applyFilter(String text) {
        if (sorter != null) {
            // Apply regex filter ((?i) makes it case-insensitive)
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }
}