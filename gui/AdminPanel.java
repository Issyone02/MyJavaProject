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

public class AdminPanel extends JPanel {
    private final LibraryManager manager;
    private final int currentUserId;
    private final JTable table;
    private final DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private final JTextField titleIn, authIn, yearIn, totalIn;
    private final JComboBox<String> typeBox;
    private final JLabel idDisplayLabel;
    private final JButton deleteBtn, editItemBtn, historyBtn, manageUsersBtn, exportBtn, importBtn, addUserBtn;

    public AdminPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        boolean isSuperAdmin = AuthManager.isSuperAdmin(currentUserId);

        // --- INPUT PANEL ---
        JPanel inputPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add New Inventory Item"));

        inputPanel.add(new JLabel("Generated ID:"));
        idDisplayLabel = new JLabel("Auto-Generated");
        idDisplayLabel.setForeground(new Color(0, 102, 204));
        inputPanel.add(idDisplayLabel);

        inputPanel.add(new JLabel("Type:"));
        typeBox = new JComboBox<>(new String[]{"Book", "Magazine", "Journal"});
        inputPanel.add(typeBox);

        inputPanel.add(new JLabel("Title:"));
        titleIn = new JTextField();
        inputPanel.add(titleIn);

        inputPanel.add(new JLabel("Author:"));
        authIn = new JTextField();
        inputPanel.add(authIn);

        inputPanel.add(new JLabel("Year:"));
        yearIn = new JTextField();
        inputPanel.add(yearIn);

        inputPanel.add(new JLabel("Total Quantity:"));
        totalIn = new JTextField();
        inputPanel.add(totalIn);

        JButton addBtn = new JButton("Add Item to Library");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(new JLabel(""));
        inputPanel.add(addBtn);

        // --- ADMIN ACTION BAR ---
        JPanel adminActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        editItemBtn = new JButton("Edit Item");
        deleteBtn = new JButton("🗑 Delete Item");
        deleteBtn.setBackground(new Color(255, 100, 100));
        deleteBtn.setForeground(Color.WHITE);

        exportBtn = new JButton("Export Data");
        importBtn = new JButton("Import Data");
        manageUsersBtn = new JButton("View Users");
        addUserBtn = new JButton("👤 Add New User");
        addUserBtn.setBackground(new Color(76, 175, 80));
        addUserBtn.setForeground(Color.WHITE);

        historyBtn = new JButton("📜 System Audit Log");

        editItemBtn.setVisible(isSuperAdmin);
        deleteBtn.setVisible(isSuperAdmin);
        exportBtn.setVisible(isSuperAdmin);
        importBtn.setVisible(isSuperAdmin);
        manageUsersBtn.setVisible(isSuperAdmin);
        addUserBtn.setVisible(isSuperAdmin);

        adminActionBar.add(editItemBtn);
        adminActionBar.add(deleteBtn);
        adminActionBar.add(new JSeparator(SwingConstants.VERTICAL));
        adminActionBar.add(exportBtn);
        adminActionBar.add(importBtn);
        adminActionBar.add(manageUsersBtn);
        adminActionBar.add(addUserBtn);
        adminActionBar.add(historyBtn);

        // --- TABLE SETUP ---
        String[] cols = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(inputPanel, BorderLayout.CENTER);
        northContainer.add(adminActionBar, BorderLayout.SOUTH);

        add(northContainer, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- LISTENERS ---
        addBtn.addActionListener(e -> handleAddItem());
        editItemBtn.addActionListener(e -> handleEditItem());
        deleteBtn.addActionListener(e -> handleDeleteItem());
        exportBtn.addActionListener(e -> handleExport());
        importBtn.addActionListener(e -> handleImport());
        manageUsersBtn.addActionListener(e -> showUserManagementPopup());
        addUserBtn.addActionListener(e -> showAddUserDialog());
        historyBtn.addActionListener(e -> showHistoryPopup());

        refreshTable();
    }

    private void handleDeleteItem() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        String itemId = (String) model.getValueAt(modelRow, 0);
        String itemTitle = (String) model.getValueAt(modelRow, 2);

        String[] reasons = {"Select Reason", "Missing", "Damaged", "Obsolete"};
        JComboBox<String> reasonBox = new JComboBox<>(reasons);
        Object[] message = {"Item: " + itemTitle, "Select reason for deletion:", reasonBox};

        int option = JOptionPane.showConfirmDialog(this, message, "Confirm Deletion", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String selectedReason = (String) reasonBox.getSelectedItem();
            if (!selectedReason.equals("Select Reason")) {
                if (confirmAdminPassword()) {
                    // ACTION: manager.removeItem already calls saveState(false)
                    boolean success = manager.removeItem(itemId);

                    if (success) {
                        manager.addLog(String.valueOf(currentUserId), "DELETE_ITEM",
                                "Removed " + itemId + " Reason: " + selectedReason);

                        // REMOVED manual FileHandler calls. Manager handles this now.
                        refreshTable();
                        JOptionPane.showMessageDialog(this, "Item '" + itemTitle + "' removed successfully.");
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Deletion Failed: Either item not found or it is currently BORROWED.",
                                "System Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a valid reason.");
            }
        }
    }

    private void handleAddItem() {
        try {
            String id = utils.IDGenerator.generateID();
            String title = titleIn.getText().trim();
            String author = authIn.getText().trim();
            int year = Integer.parseInt(yearIn.getText().trim());
            int total = Integer.parseInt(totalIn.getText().trim());
            String type = (String) typeBox.getSelectedItem();

            LibraryItem newItem = "Journal".equals(type) ? new Journal(id, title, author, year) :
                    "Magazine".equals(type) ? new Magazine(id, title, author, year) : new Book(id, title, author, year);

            newItem.setTotalCopies(total);
            newItem.setAvailableCopies(total);

            // ACTION: manager.addItem already calls saveState(false)
            manager.addItem(newItem);
            manager.addLog(String.valueOf(currentUserId), "ADD_ITEM", "Added " + type + ": " + title);

            clearInputFields();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please check all fields.");
        }
    }

    private void handleEditItem() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to edit.");
            return;
        }

        int mRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(mRow, 0);

        LibraryItem item = manager.getInventory().stream()
                .filter(i -> i.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (item != null) {
            int currentlyBorrowed = item.getTotalCopies() - item.getAvailableCopies();

            JTextField tEdit = new JTextField(item.getTitle());
            JTextField aEdit = new JTextField(item.getAuthor());
            JTextField yEdit = new JTextField(String.valueOf(item.getYear()));
            JTextField qEdit = new JTextField(String.valueOf(item.getTotalCopies()));
            JComboBox<String> typeEdit = new JComboBox<>(new String[]{"Book", "Journal", "Magazine"});
            typeEdit.setSelectedItem(item.getType());

            Object[] msg = {
                    "Item ID: " + id,
                    "Current Borrowed: " + currentlyBorrowed,
                    "Title:", tEdit,
                    "Author:", aEdit,
                    "Year:", yEdit,
                    "Type:", typeEdit,
                    "New Total Quantity:", qEdit
            };

            int result = JOptionPane.showConfirmDialog(this, msg, "Edit Item Details", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    int newTotal = Integer.parseInt(qEdit.getText().trim());

                    if (newTotal < currentlyBorrowed) {
                        JOptionPane.showMessageDialog(this, "Cannot reduce Total below " + currentlyBorrowed);
                        return;
                    }

                    if (confirmAdminPassword()) {
                        // ACTION: updateItem handles saveState(false) internally
                        manager.updateItem(String.valueOf(currentUserId), id, (String)typeEdit.getSelectedItem(),
                                tEdit.getText().trim(), aEdit.getText().trim(),
                                Integer.parseInt(yEdit.getText().trim()), newTotal, "Manual Edit");

                        refreshTable();
                        JOptionPane.showMessageDialog(this, "Item updated successfully!");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Error: numeric fields required.");
                }
            }
        }
    }

    private void handleImport() {
        if (!confirmAdminPassword()) return;
        JFileChooser c = new JFileChooser();
        if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Object[] data = FileHandler.importBackup(c.getSelectedFile());
            if (data != null) {
                // IMPORTANT: Use saveState before clearing to allow Undo/Redo of the import
                manager.saveState(false);
                manager.getInventory().clear();
                manager.getInventory().addAll((List<LibraryItem>) data[0]);
                refreshTable();
            }
        }
    }

    // --- OTHER METHODS (REMAIN UNCHANGED BUT CLEANED OF MANUAL SAVES) ---

    private void showAddUserDialog() {
        JTextField nameField = new JTextField();
        JTextField nickField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JCheckBox adminCheck = new JCheckBox("Assign as Administrator");

        Object[] message = {"Full Name:", nameField, "Nickname:", nickField, "Password:", passField, "", adminCheck};

        int option = JOptionPane.showConfirmDialog(this, message, "Register Staff", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (confirmAdminPassword()) {
                int newId = AuthManager.getAllUsers().size() + 101;
                AuthManager.addUser(newId, new String(passField.getPassword()), nickField.getText().trim(), adminCheck.isSelected());
                manager.addLog(String.valueOf(currentUserId), "USER_CREATED", "Added: " + nickField.getText());
                JOptionPane.showMessageDialog(this, "User ID: " + newId);
            }
        }
    }

    private boolean confirmAdminPassword() {
        JPasswordField pf = new JPasswordField();
        int res = JOptionPane.showConfirmDialog(this, pf, "Admin Password:", JOptionPane.OK_CANCEL_OPTION);
        return (res == JOptionPane.OK_OPTION) && AuthManager.validate(currentUserId, new String(pf.getPassword()));
    }

    private void handleExport() {
        String[] options = {"Text File", "System Data"};
        int choice = JOptionPane.showOptionDialog(this, "Select Export Format:", "Export",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == -1) return;
        JFileChooser c = new JFileChooser();
        if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = c.getSelectedFile();
            if (choice == 0) FileHandler.exportToText(manager.getInventory(), manager.getStudents(), file);
            else FileHandler.exportBackup(manager.getInventory(), manager.getStudents(), file);
        }
    }

    private void showHistoryPopup() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Audit Log", true);
        d.setLayout(new BorderLayout());
        String[] hCols = {"Operator", "Time", "Action", "Details"};
        DefaultTableModel hMod = new DefaultTableModel(hCols, 0);
        for (SystemLog log : manager.getSystemLogs()) {
            hMod.addRow(new Object[]{log.getUserId(), log.getTimestamp(), log.getAction(), log.getDetails()});
        }
        d.add(new JScrollPane(new JTable(hMod)), BorderLayout.CENTER);
        d.setSize(700, 450); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private void clearInputFields() { titleIn.setText(""); authIn.setText(""); yearIn.setText(""); totalIn.setText(""); }

    private void showUserManagementPopup() {
        if (confirmAdminPassword()) new UserManagementDialog((Frame) SwingUtilities.getWindowAncestor(this), currentUserId, manager).setVisible(true);
    }

    public void refreshTable() {
        model.setRowCount(0);
        for (LibraryItem i : manager.getInventory()) model.addRow(new Object[]{i.getId(), i.getType(), i.getTitle(), i.getAuthor(), i.getYear(), i.getAvailableCopies(), i.getTotalCopies()});
    }

    public void applyFilter(String text) { if (sorter != null) sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text)); }
}