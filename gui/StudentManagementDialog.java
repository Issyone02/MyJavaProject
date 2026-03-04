package gui;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class StudentManagementDialog extends JDialog {
    private final LibraryManager manager;
    private final DefaultTableModel model;
    private final JTable table;

    public StudentManagementDialog(Frame parent, LibraryManager manager, Student selectedStudent) {
        super(parent, "Student Records", true);
        this.manager = manager;

        setSize(600, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // --- TABLE ---
        String[] cols = {"Student ID", "Name", "No of Borrowed Items"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);

        // --- INPUT PANEL ---
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();

        inputPanel.add(new JLabel("Student ID:"));
        inputPanel.add(idField);
        inputPanel.add(new JLabel("Student Name:"));
        inputPanel.add(nameField);

        // --- BUTTONS ---
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Add to DB");
        JButton delBtn = new JButton("Delete from DB");
        JButton closeBtn = new JButton("Close");

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        btnPanel.add(closeBtn);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // Action Listeners
        addBtn.addActionListener(e -> {
            if (!idField.getText().isEmpty() && !nameField.getText().isEmpty()) {
                manager.addStudent(new Student(idField.getText(), nameField.getText()));
                refresh();
                idField.setText(""); nameField.setText("");
            }
        });

        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String id = (String) model.getValueAt(row, 0);
                manager.removeStudent(id);
                refresh();
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