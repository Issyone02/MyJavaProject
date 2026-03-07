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

        String[] columns = {"ID", "Name", "System Status", "Borrowed Count", "Titles", "Due Dates"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row != -1 && (col == 4 || col == 5)) {
                    int modelRow = table.convertRowIndexToModel(row);
                    String studentName = (String) model.getValueAt(modelRow, 1);
                    String content = (String) model.getValueAt(modelRow, col);
                    showDetailsPopup(((col == 4) ? "Items: " : "Deadlines: ") + studentName, content);
                }
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Register Student");
        JButton editBtn = new JButton("Edit Name");
        JButton deleteBtn = new JButton("Delete Student");
        deleteBtn.setBackground(new Color(255, 150, 150));
        deleteBtn.setForeground(Color.WHITE);

        boolean isSuper = AuthManager.isSuperAdmin(currentUserId);
        editBtn.setVisible(isSuper);
        deleteBtn.setVisible(isSuper);

        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);

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

    private boolean confirmAdminPassword() {
        JPasswordField pf = new JPasswordField();
        int res = JOptionPane.showConfirmDialog(this, pf, "Admin Password Required:", JOptionPane.OK_CANCEL_OPTION);
        return (res == JOptionPane.OK_OPTION) && AuthManager.validate(currentUserId, new String(pf.getPassword()));
    }

    public void applyFilter(String text) {
        if (sorter != null) sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
    }

    public void refreshTable() {
        model.setRowCount(0);
        for (Student s : manager.getStudents()) {
            String status = s.calculateStatus(manager.getWaitlist());
            String titles = s.getCurrentLoans().stream().map(r -> r.getItem().getTitle()).collect(Collectors.joining(", "));
            String dueDates = s.getCurrentLoans().stream().map(r -> r.getDueDate().toString()).collect(Collectors.joining(", "));

            model.addRow(new Object[]{
                    s.getStudentId(), s.getName(), status,
                    s.getCurrentLoans().size(),
                    titles.isEmpty() ? "None" : titles,
                    dueDates.isEmpty() ? "N/A" : dueDates
            });
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
        Object[] message = { "ID:", idField, "Full Name:", nameField };
        if (JOptionPane.showConfirmDialog(this, message, "Register Student", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            if (!id.isEmpty() && !name.isEmpty()) {
                // Manager handles saveState and persistence
                manager.addStudent(new Student(id, name));
                manager.addLog(String.valueOf(currentUserId), "STUDENT_REG", name);
                refreshTable();
            }
        }
    }

    private void handleEditStudent() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int mRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(mRow, 0);
        String oldName = (String) model.getValueAt(mRow, 1);

        String newName = JOptionPane.showInputDialog(this, "Update Name:", oldName);
        if (newName != null && !newName.trim().isEmpty() && confirmAdminPassword()) {
            manager.updateStudent(id, newName.trim());
            refreshTable();
        }
    }

    private void handleDeleteStudent() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int mRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(mRow, 0);

        if (JOptionPane.showConfirmDialog(this, "Delete Student?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (confirmAdminPassword()) {
                manager.removeStudent(id);
                refreshTable();
            }
        }
    }
}