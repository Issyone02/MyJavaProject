package gui;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class BorrowPanel extends JPanel {
    private final LibraryManager manager;
    private final int currentUserId;
    private final JTable table;
    private final DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    public BorrowPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Item Classification on Table
        String[] columns = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Buttons for borrow / Return in Student Record Page
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton borrowBtn = new JButton("Borrow Selected Item");
        JButton returnBtn = new JButton("Return Selected Item");
        borrowBtn.setPreferredSize(new Dimension(220, 45));
        returnBtn.setPreferredSize(new Dimension(220, 45));

        buttonPanel.add(borrowBtn);
        buttonPanel.add(returnBtn);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        borrowBtn.addActionListener(e -> handleBorrowAction());
        returnBtn.addActionListener(e -> handleReturnAction());

        refreshTable();
    }

    // An Item must be selected before borrowing, else it returns a message below
    private void handleBorrowAction() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to borrow.");
            return;
        }
        int mRow = table.convertRowIndexToModel(row);
        String itemId = (String) model.getValueAt(mRow, 0);
        LibraryItem item = manager.getInventory().stream().filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);

        // A pop-up to enter student's name who is borrowing the item
        String sId = JOptionPane.showInputDialog(this, "Enter Student ID borrowing this Item:");

        // If the ID field is empty and you click ok, a pop-up to type in student's name.
        if (sId == null || sId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type in a Student's ID.");
            return;
        }

        // If the ID is not in the Student registered list, and you click ok, a pop-up that ID not found.
        Student student = manager.findStudentById(sId.trim());
        if (student == null) {
            JOptionPane.showMessageDialog(this, "This Student is not found. Please register student first.");
            return;
        }

        // THis checks Item availability in the inventory and executes accordingly
        if (item != null && item.getAvailableCopies() > 0) {
            // Use the boolean result from the manager
            boolean success = manager.borrowItem(String.valueOf(currentUserId), student, item);

            // If item is available, borrow is successful
            if (success) {
                JOptionPane.showMessageDialog(this, "Item borrow was Successful!");
                refreshTable();
            } else {
                // It checks if the Student has already borrowed the same item
                JOptionPane.showMessageDialog(this,
                        "Access Denied: " + student.getName() + ". You already have this item in your possession!.\n You must return thi Item first before you can borrow again.",
                        "Duplicate Loan Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (item != null) {
            // If Student has not borrowed an item and the Item is not available, this pushes the student to Wait list queue.
            handleWaitlistAddition(student, item);
        }
    }

    // An Item must be selected before returning, else it returns a message below
    private void handleReturnAction() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to return.");
            return;
        }
        int mRow = table.convertRowIndexToModel(row);
        String itemId = (String) model.getValueAt(mRow, 0);
        LibraryItem item = manager.getInventory().stream().filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);

        // A pop-up to enter student's name who is returning the item
        String sId = JOptionPane.showInputDialog(this, "Enter Student ID returning this item:");

        // If the ID is not in the Student registered list, and you click ok, a pop-up that ID not found.
        if (sId == null) {
            JOptionPane.showMessageDialog(this, "This Student is not found. Please register student first.");
            return;
        }
        Student student = manager.findStudentById(sId.trim());

        // This confirms that student and item are valid, then it processes the return
        if (student != null && item != null) {
            manager.returnItem(String.valueOf(currentUserId), student, item);

            // Immediately check the live waitlist state
            processWaitlist(item);
            refreshTable();
        }
    }

    private void processWaitlist(LibraryItem item) {
        String target = "-> " + item.getTitle();
        // Fetch LIVE waitlist from manager to ensure current order is respected
        java.util.List<String> currentWaitlist = manager.getWaitlist();

        String nextStudentEntry = currentWaitlist.stream()
                .filter(entry -> entry.contains(target))
                .findFirst()
                .orElse(null);

        // This checks for available student in waitlist and processes the action
        if (nextStudentEntry != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Waitlist Alert!\n" + nextStudentEntry + "\nDo you want to assign this Item to the student on queue now?",
                    "Waitlist Management", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                try {
                    // Extract Student ID from the format: Name (ID) -> Title
                    String id = nextStudentEntry.substring(nextStudentEntry.indexOf("(") + 1, nextStudentEntry.indexOf(")"));
                    Student s = manager.findStudentById(id);

                    if (s != null) {
                        // Attempt to borrow - this now returns a boolean based on possession check
                        boolean success = manager.borrowItem(String.valueOf(currentUserId), s, item);

                        if (success) {
                            // Only remove from waitlist if the borrow transaction actually went through
                            manager.removeWaitlistEntry(String.valueOf(currentUserId), currentWaitlist.indexOf(nextStudentEntry));
                            JOptionPane.showMessageDialog(this, "Waitlist fulfilled successfully.");
                        } else {
                            // If borrowItem returned false, it means they already have the book
                            JOptionPane.showMessageDialog(this,
                                    "Cannot fulfill waitlist: " + s.getName() + " already has a copy of this item.",
                                    "Waitlist Skipped", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error during auto-assign: " + e.getMessage());
                }
            }
        }
    }

    private void handleWaitlistAddition(Student s, LibraryItem item) {
        if (JOptionPane.showConfirmDialog(this, "This Item is Unavailable right Now. Do you want to add this student to waitlist Queue?", "Waitlist", JOptionPane.YES_NO_OPTION) == 0) {
            manager.addToWaitlist(String.valueOf(currentUserId), s, item);
            JOptionPane.showMessageDialog(this, "This Student:" + s.getName() + "has been added to waitlist.");
        }
    }

    public void refreshTable() {
        model.setRowCount(0);
        for (LibraryItem i : manager.getInventory()) {
            model.addRow(new Object[]{i.getId(), i.getType(), i.getTitle(), i.getAuthor(), i.getYear(), i.getAvailableCopies(), i.getTotalCopies()});
        }
    }

    public void applyFilter(String text) {
        if (sorter != null) sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
    }
}