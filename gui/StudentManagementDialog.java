package gui;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * StudentManagementDialog - Dialog for managing student records in the library system
 * Provides functionality to add, view, and remove students
 * Features duplicate ID checking to prevent data integrity issues
 */
public class StudentManagementDialog extends JDialog {
    // Core components
    private final LibraryManager manager;          // Reference to library manager
    private final DefaultTableModel model;         // Table data model
    private final JTable table;                    // Table displaying students

    /**
     * Constructor - Initializes the student management dialog
     * @param parent Parent frame for modal positioning
     * @param manager Library manager instance
     */
    public StudentManagementDialog(Frame parent, LibraryManager manager) {
        super(parent, "System Student Database", true);  // Modal dialog
        this.manager = manager;

        // Set dialog properties
        setSize(600, 500);
        setLocationRelativeTo(parent);  // Center on parent
        setLayout(new BorderLayout(10, 10));

        // ==================== TABLE SETUP ====================
        // Define table columns
        String[] cols = {"Student ID", "Full Name", "Active Loans"};

        // Create non-editable table model
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;  // Prevent direct cell editing
            }
        };

        // Initialize table with single row selection
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // ==================== INPUT PANEL ====================
        // Panel for adding new students
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Input fields
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();

        // Add labeled input fields
        inputPanel.add(new JLabel("Student ID:"));
        inputPanel.add(idField);
        inputPanel.add(new JLabel("Full Name:"));
        inputPanel.add(nameField);

        // ==================== BUTTON PANEL ====================
        // Action buttons at the bottom
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton addBtn = new JButton("Add Student");
        JButton delBtn = new JButton("Remove Selected");
        JButton closeBtn = new JButton("Close");

        // Add buttons to panel
        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        btnPanel.add(closeBtn);

        // ==================== LAYOUT ASSEMBLY ====================
        add(inputPanel, BorderLayout.NORTH);          // Input fields at top
        add(new JScrollPane(table), BorderLayout.CENTER);  // Table in center
        add(btnPanel, BorderLayout.SOUTH);            // Buttons at bottom

        // ==================== EVENT LISTENERS ====================

        /**
         * Add Student Button - Validates and adds new student
         * CRITICAL FIX #1: Now includes duplicate ID checking
         */
        addBtn.addActionListener(e -> {
            // Get trimmed input values
            String id = idField.getText().trim();
            String name = nameField.getText().trim();

            // Validate that both fields are filled
            if (!id.isEmpty() && !name.isEmpty()) {
                // Attempt to add student (returns false if ID already exists)
                boolean success = manager.addStudent(new Student(id, name));

                if (success) {
                    // Student added successfully
                    refresh();  // Update table display
                    idField.setText("");   // Clear input fields
                    nameField.setText("");

                    // Show success confirmation
                    JOptionPane.showMessageDialog(this,
                            "Student '" + name + "' (ID: " + id + ") added successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // Duplicate ID detected - show error
                    JOptionPane.showMessageDialog(this,
                            "ERROR: Student ID '" + id + "' already exists in the system!\n\n" +
                                    "Please use a different ID or check existing records.",
                            "Duplicate Student ID",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Empty fields - show validation message
                JOptionPane.showMessageDialog(this,
                        "Please fill in both Student ID and Full Name fields.",
                        "Incomplete Information",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        /**
         * Delete Button - Removes selected student from system
         * Requires confirmation to prevent accidental deletions
         */
        delBtn.addActionListener(e -> {
            // Get selected row index
            int row = table.getSelectedRow();

            if (row != -1) {
                // Student is selected - get their ID
                String id = (String) model.getValueAt(row, 0);
                String name = (String) model.getValueAt(row, 1);

                // Confirm deletion with user
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete student:\n\n" +
                                "ID: " + id + "\n" +
                                "Name: " + name + "\n\n" +
                                "This action cannot be undone.",
                        "Confirm Deletion",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    // User confirmed - proceed with deletion
                    manager.removeStudent(id);
                    refresh();  // Update table

                    // Show success message
                    JOptionPane.showMessageDialog(this,
                            "Student removed successfully.",
                            "Deleted",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                // No student selected
                JOptionPane.showMessageDialog(this,
                        "Please select a student from the table to remove.",
                        "No Selection",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        /**
         * Close Button - Closes the dialog
         */
        closeBtn.addActionListener(e -> dispose());

        // ==================== INITIALIZATION ====================
        // Load initial data into table
        refresh();
    }

    /**
     * Refreshes the table display with current student data
     * Clears existing rows and reloads from manager
     */
    private void refresh() {
        // Clear all existing rows
        model.setRowCount(0);

        // Add a row for each student
        for (Student s : manager.getStudents()) {
            model.addRow(new Object[]{
                    s.getStudentId(),                    // Student ID
                    s.getName(),                         // Full name
                    s.getCurrentLoans().size()           // Number of active loans
            });
        }
    }
}