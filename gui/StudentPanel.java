package gui;

import model.BorrowRecord;
import controller.LibraryManager;
import model.*;
import utils.AuthManager;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel for managing students and viewing their borrowing status.
 * Overdue due-dates are shown in red bold.
 */
public class StudentPanel extends JPanel {
    private final LibraryManager manager;
    private final int currentUserId;
    private final JTable table;
    private final VirtualTableModel model;
    private final TableRowSorter<VirtualTableModel> sorter;

    public StudentPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"ID", "Name", "Borrowed", "Items", "Due Dates"};
        model  = new VirtualTableModel(columns);
        table  = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Overdue dates shown in red bold, alternating row colours
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                    c.setForeground(Color.BLACK);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));

                    if (column == 4 && value != null && !value.toString().equals("N/A")) {
                        int modelRow = table.convertRowIndexToModel(row);
                        String studentId = (String) model.getValueAt(modelRow, 0);
                        UserAccount student = manager.findStudentById(studentId);
                        if (student != null && student.getCurrentLoans().stream().anyMatch(BorrowRecord::isOverdue)) {
                            c.setForeground(Color.RED);
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        }
                    }
                }
                return c;
            }
        });

        // Click on Items or Due Dates column to see details
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row != -1 && (col == 3 || col == 4)) {
                    int modelRow = table.convertRowIndexToModel(row);
                    String studentName = (String) model.getValueAt(modelRow, 1);
                    String content = (String) model.getValueAt(modelRow, col);
                    showDetailsPopup(((col == 3) ? "Items: " : "Due: ") + studentName, content);
                }
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn    = new JButton("Add UserAccount");
        JButton editBtn   = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(new Color(255, 100, 100));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setOpaque(true);

        addBtn.setToolTipText("Register a new student");
        editBtn.setToolTipText("Edit the selected student's name");
        deleteBtn.setToolTipText("Remove the selected student");


        boolean isAdmin = AuthManager.isAdmin(currentUserId);
        editBtn.setVisible(isAdmin);
        deleteBtn.setVisible(isAdmin);

        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);

        addBtn.addActionListener(e -> handleAddUserAccount());
        editBtn.addActionListener(e -> handleEditUserAccount());
        deleteBtn.addActionListener(e -> handleDeleteUserAccount());

        add(new JLabel("STUDENT RECORDS", JLabel.CENTER), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        refreshTable();
    }

    private boolean confirmAdminPassword() {
        JPasswordField pf = new JPasswordField();
        int res = JOptionPane.showConfirmDialog(this, pf, "Password:", JOptionPane.OK_CANCEL_OPTION);
        return (res == JOptionPane.OK_OPTION) && AuthManager.validate(currentUserId, new String(pf.getPassword()));
    }

    public void applyFilter(String text) {
        if (sorter != null) sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
    }

    public void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (UserAccount s : manager.getStudents()) {
            String titles = s.getCurrentLoans().stream()
                    .map(r -> r.getItem().getTitle()).collect(Collectors.joining(", "));
            String dueDates = s.getCurrentLoans().stream()
                    .map(r -> r.isOverdue() ? r.getDueDate() + " (OVERDUE!)" : r.getDueDate().toString())
                    .collect(Collectors.joining(", "));
            rows.add(new Object[]{
                s.getStudentId(), s.getName(),
                s.getCurrentLoans().size(),
                titles.isEmpty()   ? "None" : titles,
                dueDates.isEmpty() ? "N/A"  : dueDates
            });
        }
        model.setRows(rows);
    }

    private void showDetailsPopup(String title, String content) {
        if (content == null || content.equals("None") || content.equals("N/A")) return;
        JTextArea textArea = new JTextArea(10, 30);
        textArea.setText("\u2022 " + content.replace(", ", "\n\u2022 "));
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleAddUserAccount() {
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        Object[] message = {"ID:", idField, "Name:", nameField};

        if (JOptionPane.showConfirmDialog(this, message, "Add UserAccount", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            if (!id.isEmpty() && !name.isEmpty()) {
                if (manager.addStudent(new UserAccount(id, name))) {
                    manager.addLog(String.valueOf(currentUserId), "ADD_STUDENT", name);
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "UserAccount added.");
                } else {
                    JOptionPane.showMessageDialog(this, "ID already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void handleEditUserAccount() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int mRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(mRow, 0);
        String oldName = (String) model.getValueAt(mRow, 1);

        String newName = JOptionPane.showInputDialog(this, "New name:", oldName);
        if (newName != null && !newName.trim().isEmpty() && confirmAdminPassword()) {
            manager.updateStudent(id, newName.trim());
            refreshTable();
        }
    }

    private void handleDeleteUserAccount() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int mRow = table.convertRowIndexToModel(row);
        String id = (String) model.getValueAt(mRow, 0);

        if (JOptionPane.showConfirmDialog(this, "Delete this student?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (confirmAdminPassword()) {
                manager.removeStudent(id);
                refreshTable();
            }
        }
    }
}
