package gui;

import model.*;
import utils.AuthManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.stream.Collectors;

public class StudentPanel extends JPanel {
    private final LibraryManager manager;
    private final int currentUserId;
    private final JTable table;
    private final DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    public StudentPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"ID", "Name", "No of Borrowed Items", "Borrowed Titles", "Due Dates"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);

        // --- CRITICAL: Initialize Sorter ---
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row != -1 && (col == 3 || col == 4)) {
                    int modelRow = table.convertRowIndexToModel(row);
                    String studentName = (String) model.getValueAt(modelRow, 1);
                    String content = (String) model.getValueAt(modelRow, col);
                    showDetailsPopup(((col == 3) ? "Items: " : "Deadlines: ") + studentName, content);
                }
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Register Student");
        JButton editBtn = new JButton("Edit Name");
        JButton deleteBtn = new JButton("Delete Student");
        deleteBtn.setBackground(new Color(255, 150, 150));

        boolean isSuper = AuthManager.isSuperAdmin(currentUserId);
        deleteBtn.setEnabled(isSuper);
        editBtn.setEnabled(isSuper);
        btnPanel.add(addBtn); btnPanel.add(editBtn); btnPanel.add(deleteBtn);

        JLabel header = new JLabel("STUDENT RECORDS", JLabel.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> handleAddStudent());
        editBtn.addActionListener(e -> handleEditStudent());
        deleteBtn.addActionListener(e -> handleDeleteStudent());

        refreshTable();
    }

    // --- GLOBAL SEARCH LOGIC ---
    public void applyFilter(String text) {
        if (sorter == null) return;
        if (text == null || text.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Searches Name, ID, and borrowed titles simultaneously
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    public void refreshTable() {
        model.setRowCount(0);
        for (Student s : manager.getStudents()) {
            String titles = s.getCurrentLoans().stream().map(r -> r.getItem().getTitle()).collect(Collectors.joining(", "));
            String dueDates = s.getCurrentLoans().stream().map(r -> r.getDueDate().toString()).collect(Collectors.joining(", "));
            model.addRow(new Object[]{s.getStudentId(), s.getName(), s.getCurrentLoans().size(),
                    titles.isEmpty() ? "None" : titles, dueDates.isEmpty() ? "N/A" : dueDates});
        }
    }

    private void showDetailsPopup(String title, String content) {
        if (content == null || content.equals("None") || content.equals("N/A")) return;
        JTextArea textArea = new JTextArea(10, 30);
        textArea.setText("• " + content.replace(", ", "\n• "));
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleAddStudent() {
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        Object[] message = { "ID (Numbers):", idField, "Name:", nameField };
        if (JOptionPane.showConfirmDialog(this, message, "Register", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            if (!id.isEmpty() && !name.isEmpty()) {
                manager.addStudent(new Student(id, name));
                manager.addLog(String.valueOf(currentUserId), "STUDENT_REG", "Registered: " + name + " (" + id + ")");
                refreshTable();
            }
        }
    }

    private void handleEditStudent() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int modelRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(modelRow, 0);
        String oldName = (String) model.getValueAt(modelRow, 1);
        String newName = JOptionPane.showInputDialog(this, "Update Name:", oldName);
        if (newName != null && !newName.trim().isEmpty()) {
            manager.updateStudent(id, newName.trim());
            manager.addLog(String.valueOf(currentUserId), "STUDENT_EDIT", "Student renamed: " + id);
            refreshTable();
        }
    }

    private void handleDeleteStudent() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int modelRow = table.convertRowIndexToModel(row);
            String id = (String) model.getValueAt(modelRow, 0);
            if (JOptionPane.showConfirmDialog(this, "Delete " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                manager.removeStudent(id);
                manager.addLog(String.valueOf(currentUserId), "STUDENT_DEL", "Deleted student ID: " + id);
                refreshTable();
            }
        }
    }
}