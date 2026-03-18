package gui;

import controller.LibraryController;
import utils.GuiUtils;


import model.StudentSummary;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Student management panel — register, edit, delete, and view borrowing status. */
public class StudentPanel extends JPanel {

    private final LibraryController controller;
    private final int               currentUserId;
    private final JTable            table;
    private final VirtualTableModel model;
    private final TableRowSorter<VirtualTableModel> sorter;

    public StudentPanel(LibraryController controller, int userId) {
        this.controller    = controller;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Visible columns: ID, Name, Borrowed, Items, Due Dates
        // Column 5 (hasOverdue boolean) is stored in the model but never rendered —
        // the custom renderer reads it to decide whether to colour column 4 red.
        String[] columns = {"ID", "Name", "Borrowed", "Items", "Due Dates"};
        model  = new VirtualTableModel(columns);
        table  = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                    c.setForeground(Color.BLACK);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));

                    // Overdue color logic removed per request.
                    // The text "(OVERDUE)" will be displayed as part of the string value if present in the model.
                }
                return c;
            }
        });

        // Clicking columns 3 (Items) or 4 (Due Dates) opens a scrollable popup
        // because the comma-separated values are too long to read in one cell
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row != -1 && (col == 3 || col == 4)) {
                    int    modelRow    = table.convertRowIndexToModel(row);
                    String studentName = (String) model.getValueAt(modelRow, 1);
                    String content     = (String) model.getValueAt(modelRow, col);
                    showDetailsPopup((col == 3 ? "Items: " : "Due: ") + studentName, content);
                }
            }
        });

        JPanel  btnPanel  = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn    = new JButton("Add Student");
        JButton editBtn   = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBackground(new Color(255, 100, 100));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setOpaque(true);
        addBtn.setToolTipText("Register a new student");
        editBtn.setToolTipText("Edit the selected student's name");
        deleteBtn.setToolTipText("Remove the selected student");

        boolean isAdmin = controller.isAdminUser(currentUserId);
        editBtn.setVisible(isAdmin);
        deleteBtn.setVisible(isAdmin);

        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);

        addBtn.addActionListener(e    -> handleAddStudent());
        editBtn.addActionListener(e   -> handleEditStudent());
        deleteBtn.addActionListener(e -> handleDeleteStudent());

        add(new JLabel("STUDENT RECORDS", JLabel.CENTER), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        refreshTable();
    }

    // ── Add ───────────────────────────────────────────────────────────────────

    private void handleAddStudent() {
        JTextField idField   = new JTextField();
        JTextField nameField = new JTextField();
        Object[]   message   = {"ID:", idField, "Name:", nameField};

        if (JOptionPane.showConfirmDialog(this, message, "Add Student",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String id   = idField.getText().trim();
            String name = nameField.getText().trim();
            if (!id.isEmpty() && !name.isEmpty()) {
                // Factory: controller builds UserAccount internally
                boolean added = controller.createStudent(id, name, String.valueOf(currentUserId));
                if (added) {
                    refreshTable();
                    JOptionPane.showMessageDialog(this, "Student added.");
                } else {
                    JOptionPane.showMessageDialog(this, "ID already exists!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    private void handleEditStudent() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int    mRow    = table.convertRowIndexToModel(row);
        String id      = (String) model.getValueAt(mRow, 0);
        String oldName = (String) model.getValueAt(mRow, 1);

        String newName = JOptionPane.showInputDialog(this, "New name:", oldName);
        if (newName != null && !newName.trim().isEmpty() && confirmPassword()) {
            controller.updateStudent(id, newName.trim(), String.valueOf(currentUserId));
            refreshTable();
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void handleDeleteStudent() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        int    mRow = table.convertRowIndexToModel(row);
        String id   = (String) model.getValueAt(mRow, 0);

        if (JOptionPane.showConfirmDialog(this, "Delete this student?", "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (confirmPassword()) {
                controller.removeStudent(id, String.valueOf(currentUserId));
                refreshTable();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean confirmPassword() {
        return GuiUtils.confirmPassword(this, controller, currentUserId, "Password:");
    }

    // Case-insensitive regex filter across all columns; empty string clears the filter
    public void applyFilter(String text) {
        if (sorter != null) sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
    }

    /**
     * Populates the table from StudentSummary DTOs.
     * Column layout: 0=ID  1=Name  2=Borrowed  3=Items  4=DueDates  5=hasOverdue(hidden)
     */
    public void refreshTable() {
        List<Object[]> rows = new ArrayList<>();
        for (StudentSummary s : controller.getStudentSummaries()) {
            rows.add(new Object[]{
                    s.studentId(), s.name(), s.loanCount(),
                    s.itemTitles(), s.dueDates(),
                    s.hasOverdue()   // index 5: read by renderer, not shown as a column
            });
        }
        model.setRows(rows);
    }

    // Opens a scrollable popup listing all items or due dates for a student
    private void showDetailsPopup(String title, String content) {
        if (content == null || "None".equals(content) || "N/A".equals(content)) return;
        JTextArea textArea = new JTextArea(10, 30);
        textArea.setText("\u2022 " + content.replace(", ", "\n\u2022 "));
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                title, JOptionPane.INFORMATION_MESSAGE);
    }
}