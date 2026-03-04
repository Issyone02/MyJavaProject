package gui;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;

public class BorrowPanel extends JPanel {
    private final LibraryManager manager;
    private final JTable table;
    private final DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    public BorrowPanel(LibraryManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);


        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton borrowBtn = new JButton("Borrow Selected Item");
        JButton returnBtn = new JButton("Return Selected Item");
        borrowBtn.setPreferredSize(new Dimension(200, 40));
        returnBtn.setPreferredSize(new Dimension(200, 40));
        borrowBtn.setBackground(new Color(181, 234, 215));
        returnBtn.setBackground(new Color(173, 203, 227));

        buttonPanel.add(borrowBtn);
        buttonPanel.add(returnBtn);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        borrowBtn.addActionListener(e -> handleBorrowAction());
        returnBtn.addActionListener(e -> handleReturnAction());

        refreshTable();
    }

    // --- GLOBAL SEARCH LOGIC ---
    public void applyFilter(String text) {
        if (sorter == null) return;
        if (text == null || text.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void handleBorrowAction() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) { JOptionPane.showMessageDialog(this, "Please Select an item."); return; }

        int modelRow = table.convertRowIndexToModel(viewRow);
        String itemId = (String) model.getValueAt(modelRow, 0);
        LibraryItem selectedItem = manager.getInventory().stream().filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);

        String studentId = JOptionPane.showInputDialog(this, "Enter Student ID borrowing the Item:");
        if (studentId == null || studentId.trim().isEmpty()) return;

        Student student = manager.findStudentById(studentId.trim());
        if (student == null) { JOptionPane.showMessageDialog(this, "This Student is Not Found, Please Register"); return; }

        if (selectedItem != null && selectedItem.getAvailableCopies() > 0) {
            manager.saveState();
            selectedItem.setAvailableCopies(selectedItem.getAvailableCopies() - 1);
            student.getCurrentLoans().add(new BorrowRecord(selectedItem, LocalDate.now()));
            manager.addLog("STAFF", "BORROW", "Student: " + student.getName() + " borrowed " + selectedItem.getTitle());
            JOptionPane.showMessageDialog(this, "This item  was borrowed Successfully!");
            refreshTable();
        } else if (selectedItem != null) {
            handleWaitlist(student, selectedItem);
        }
    }

    private void handleReturnAction() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) { JOptionPane.showMessageDialog(this, "Please Select an item."); return; }

        int modelRow = table.convertRowIndexToModel(viewRow);
        String itemId = (String) model.getValueAt(modelRow, 0);
        LibraryItem selectedItem = manager.getInventory().stream().filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);

        String studentId = JOptionPane.showInputDialog(this, "Enter Student ID returning item:");
        if (studentId == null) return;

        Student student = manager.findStudentById(studentId.trim());
        if (student == null) return;

        manager.saveState();
        boolean removed = student.getCurrentLoans().removeIf(r -> r.getItem().getId().equals(itemId));

        if (removed && selectedItem != null) {
            manager.addLog("STAFF", "RETURN", "Item: " + selectedItem.getTitle() + " returned by " + student.getName());
            selectedItem.setAvailableCopies(selectedItem.getAvailableCopies() + 1);
            JOptionPane.showMessageDialog(this, "This item was returned successfully.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this, "Borrow record not found for this Student.");
        }
    }

    private void handleWaitlist(Student s, LibraryItem item) {
        if (JOptionPane.showConfirmDialog(this, "Do you want to add to this Student to waitlist?", "Waitlist", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            manager.saveState();
            manager.getWaitlist().add(s.getName() + " (" + s.getStudentId() + ") waiting for: " + item.getTitle());
            manager.addLog("STAFF", "WAITLIST", s.getName() + " queued for " + item.getTitle());
        }
    }

    public void refreshTable() {
        model.setRowCount(0);
        for (LibraryItem i : manager.getInventory()) {
            model.addRow(new Object[]{i.getId(), i.getType(), i.getTitle(), i.getAuthor(), i.getYear(), i.getAvailableCopies(), i.getTotalCopies()});
        }
    }
}