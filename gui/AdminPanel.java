package gui;

import model.*;
import utils.FileHandler;
import utils.AuthManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.PrintWriter;
import java.util.List;
import java.io.File;
import java.util.ArrayList;

public class AdminPanel extends JPanel {
    private final LibraryManager manager;
    private final int currentUserId;
    private final JTable table;
    private final DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private final JTextField titleIn, authIn, yearIn, totalIn;
    private final JComboBox<String> typeBox;
    private final JLabel idDisplayLabel;
    private final JButton deleteBtn, editItemBtn, historyBtn;

    public AdminPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        boolean isSuperAdmin = AuthManager.isSuperAdmin(currentUserId);

        JPanel inputPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Manage Inventory"));
        inputPanel.add(new JLabel("Generated ID:"));
        idDisplayLabel = new JLabel("Auto-Generated");
        idDisplayLabel.setForeground(Color.BLUE);
        inputPanel.add(idDisplayLabel);
        inputPanel.add(new JLabel("Item Type:"));
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
        JButton addBtn = new JButton("Add Item");
        inputPanel.add(new JLabel("Admin: " + currentUserId));
        inputPanel.add(addBtn);

        JPanel adminActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton exportBtn = new JButton("Export Data");
        JButton importBtn = new JButton("Import Data");
        JButton manageUsersBtn = new JButton("Manage Users");
        historyBtn = new JButton("📜 System Audit Log");

        deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(new Color(255, 150, 150));
        editItemBtn = new JButton("Edit");

        exportBtn.setEnabled(isSuperAdmin);
        importBtn.setEnabled(isSuperAdmin);
        manageUsersBtn.setEnabled(isSuperAdmin);
        deleteBtn.setEnabled(isSuperAdmin);
        editItemBtn.setEnabled(isSuperAdmin);

        adminActionBar.add(editItemBtn);
        adminActionBar.add(deleteBtn);
        adminActionBar.add(exportBtn);
        adminActionBar.add(importBtn);
        adminActionBar.add(manageUsersBtn);
        adminActionBar.add(historyBtn);

        String[] cols = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};
        model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(inputPanel, BorderLayout.CENTER);
        northContainer.add(adminActionBar, BorderLayout.SOUTH);
        add(northContainer, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        addBtn.addActionListener(e -> handleAddItem());
        editItemBtn.addActionListener(e -> handleEditItem());
        deleteBtn.addActionListener(e -> handleDeleteItem());
        exportBtn.addActionListener(e -> handleExport());
        importBtn.addActionListener(e -> handleImport());
        manageUsersBtn.addActionListener(e -> showUserManagementPopup());
        historyBtn.addActionListener(e -> showHistoryPopup());

        refreshTable();
    }

    private boolean confirmAuth(String act) {
        JPanel p = new JPanel(new GridLayout(2, 2));
        JTextField idF = new JTextField(); JPasswordField passF = new JPasswordField();
        p.add(new JLabel("Admin ID:")); p.add(idF); p.add(new JLabel("Password:")); p.add(passF);
        int res = JOptionPane.showConfirmDialog(this, p, "Auth Required: " + act, JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                int id = Integer.parseInt(idF.getText());
                String ps = new String(passF.getPassword());
                if (AuthManager.validate(id, ps) && AuthManager.isSuperAdmin(id)) return true;
                else JOptionPane.showMessageDialog(this, "Unauthorized.");
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Invalid ID."); }
        }
        return false;
    }

    private void handleExport() {
        String[] options = {"Text File", "System Backup"};
        int choice = JOptionPane.showOptionDialog(this, "Select Export Format:", "Export",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == -1) return;

        JFileChooser c = new JFileChooser();
        if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = c.getSelectedFile();
            boolean success;
            if (choice == 0) {
                if (!file.getName().toLowerCase().endsWith(".txt")) file = new File(file.getAbsolutePath() + ".txt");
                success = utils.FileHandler.exportToText(manager.getInventory(), manager.getStudents(), file);
            } else {
                if (!file.getName().toLowerCase().endsWith(".dat")) file = new File(file.getAbsolutePath() + ".dat");
                success = utils.FileHandler.exportBackup(manager.getInventory(), manager.getStudents(), file);
            }
            if (success) JOptionPane.showMessageDialog(this, "Back-up Export successful!");
        }
    }

    private void handleImport() {
        if (confirmAuth("Import")) {
            JFileChooser c = new JFileChooser();
            if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                Object[] data = utils.FileHandler.importBackup(c.getSelectedFile());
                if (data != null) {
                    manager.getInventory().clear();
                    manager.getInventory().addAll((List<model.LibraryItem>) data[0]);
                    manager.getStudents().clear();
                    manager.getStudents().addAll((List<model.Student>) data[1]);
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "Back-up Import Successful!");
                }
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

            String selectedType = (String) typeBox.getSelectedItem();
            LibraryItem newItem;
            if ("Journal".equalsIgnoreCase(selectedType)) newItem = new Journal(id, title, author, year);
            else if ("Magazine".equalsIgnoreCase(selectedType)) newItem = new Magazine(id, title, author, year);
            else newItem = new Book(id, title, author, year);

            newItem.setTotalCopies(total);
            newItem.setAvailableCopies(total);
            manager.addItem(newItem);

            clearInputFields();
            refreshTable();
            JOptionPane.showMessageDialog(this, "Item Added successfully!");
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid! Ensure all fields are filled."); }
    }

    private void handleEditItem() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to edit.");
            return;
        }
        int mRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(mRow, 0);

        if (!confirmAuth("Edit Item")) return;

        LibraryItem item = manager.getInventory().stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
        if (item == null) return;

        // Populate fields with current data
        JTextField tEdit = new JTextField(item.getTitle());
        JTextField qEdit = new JTextField(String.valueOf(item.getTotalCopies()));
        JComboBox<String> typeEdit = new JComboBox<>(new String[]{"Book", "Journal", "Magazine"});
        typeEdit.setSelectedItem(item.getType());

        // Calculation check for the UI message
        int currentlyBorrowed = item.getTotalCopies() - item.getAvailableCopies();
        String borrowNote = currentlyBorrowed > 0 ? "\n(Note: " + currentlyBorrowed + " units are currently borrowed)" : "";

        Object[] msg = {
                "Edit Title:", tEdit,
                "Edit Type:", typeEdit,
                "New Total Quantity:" + borrowNote, qEdit
        };

        if (JOptionPane.showConfirmDialog(this, msg, "Edit Item: " + id, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                int newTotal = Integer.parseInt(qEdit.getText().trim());
                String newType = (String) typeEdit.getSelectedItem();
                String newTitle = tEdit.getText().trim();

                // Pass to the smart manager logic we implemented earlier
                manager.updateItem(id, newType, newTitle, item.getAuthor(), item.getYear(), newTotal, "Admin Manual Edit");

                refreshTable();
                JOptionPane.showMessageDialog(this, "Item updated successfully!");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a number.");
            }
        }
    }

    private void handleDeleteItem() {
        int row = table.getSelectedRow();
        if (row != -1 && confirmAuth("Delete")) {
            int mRow = table.convertRowIndexToModel(row);
            manager.removeItem((String) model.getValueAt(mRow, 0));
            refreshTable();
        }
    }

    private void clearInputFields() {
        titleIn.setText(""); authIn.setText(""); yearIn.setText(""); totalIn.setText("");
        typeBox.setSelectedIndex(0);
    }

    private void showUserManagementPopup() {
        if (confirmAuth("Users")) new UserManagementDialog((Frame) SwingUtilities.getWindowAncestor(this)).setVisible(true);
    }

    private void showHistoryPopup() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "System Activity Tracker (Audit Log)", true);
        d.setLayout(new BorderLayout());

        String[] hCols = {"User", "Time", "Action", "Details"};
        DefaultTableModel hMod = new DefaultTableModel(hCols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        for (SystemLog log : manager.getSystemLogs()) {
            hMod.addRow(new Object[]{log.getUserId(), log.getTimestamp(), log.getAction(), log.getDetails()});
        }

        JTable hTab = new JTable(hMod);
        hTab.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String action = (String) table.getValueAt(row, 2);
                if ("STOCK_REDUCTION".equals(action) || "DELETE_ITEM".equals(action) || "STOCK_UPDATE".equals(action)) {
                    c.setForeground(Color.RED);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(isSelected ? table.getSelectionForeground() : Color.BLACK);
                }
                return c;
            }
        });

        d.add(new JScrollPane(hTab), BorderLayout.CENTER);

        JButton exportLogBtn = new JButton("📥 Export Audit History ");
        exportLogBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(d) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter out = new PrintWriter(chooser.getSelectedFile() + ".txt")) {
                    out.println("SLCAS SYSTEM AUDIT LOG - ACTIVITY TRACKER");
                    out.println("------------------------------------------");
                    for (int i = 0; i < hMod.getRowCount(); i++) {
                        out.printf("[%s] User: %s | Action: %s | Details: %s%n",
                                hMod.getValueAt(i,1), hMod.getValueAt(i,0),
                                hMod.getValueAt(i,2), hMod.getValueAt(i,3));
                    }
                    JOptionPane.showMessageDialog(d, "Log Exported Successfully.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(d, "Export Failed.");
                }
            }
        });

        d.add(exportLogBtn, BorderLayout.SOUTH);
        d.setSize(950, 550);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    public void refreshTable() {
        model.setRowCount(0);
        for (LibraryItem i : manager.getInventory()) {
            model.addRow(new Object[]{i.getId(), i.getType(), i.getTitle(), i.getAuthor(), i.getYear(), i.getAvailableCopies(), i.getTotalCopies()});
        }
    }

    public void applyFilter(String text) {
        if (text == null || text.trim().isEmpty()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
    }
}