package gui;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class StudentManagementDialog extends JDialog {
    private final LibraryManager manager;
    private final DefaultTableModel model;
    private final JTable table;

    public StudentManagementDialog(Frame parent, LibraryManager manager) {
        super(parent, "System Student Database", true);
        this.manager = manager;

        setSize(600, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // --- TABLE ---
        String[] cols = {"Student ID", "Full Name", "Active Loans"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // --- INPUT PANEL ---
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();

        inputPanel.add(new JLabel("ID:"));
        inputPanel.add(idField);
        inputPanel.add(new JLabel("Full Name:"));
        inputPanel.add(nameField);

        // --- BUTTONS ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("Add Student");
        JButton delBtn = new JButton("Remove Selected");
        JButton closeBtn = new JButton("Close");

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        btnPanel.add(closeBtn);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // Listeners
        addBtn.addActionListener(e -> {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            if (!id.isEmpty() && !name.isEmpty()) {
                manager.addStudent(new Student(id, name));
                refresh();
                idField.setText(""); nameField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Please fill all fields.");
            }
        });

        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String id = (String) model.getValueAt(row, 0);
                if (JOptionPane.showConfirmDialog(this, "Delete student " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION) == 0) {
                    manager.removeStudent(id);
                    refresh();
                }
            }
        });

        closeBtn.addActionListener(e -> dispose());

        refresh();
    }

    private void refresh() {
        model.setRowCount(0);
        for (Student s : manager.getStudents()) {
            model.addRow(new Object[]{s.getStudentId(), s.getName(), s.getCurrentLoans().size()});
        }
    }
}