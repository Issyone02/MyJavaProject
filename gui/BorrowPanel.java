package gui;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

/**
 * BorrowPanel - Manages book borrowing and return transactions
 *
 * This panel serves as the main interface for processing library loans and returns.
 * It provides functionality for:
 * - Borrowing items from the library inventory
 * - Returning borrowed items
 * - Managing waitlists for unavailable items
 * - Automatic waitlist processing when items are returned
 *
 * Key features:
 * - Duplicate loan prevention (students can't borrow same item twice)
 * - Automatic waitlist notification and assignment
 * - Real-time inventory availability updates
 * - Student validation before transactions
 */
public class BorrowPanel extends JPanel {
    // Core components
    private final LibraryManager manager;          // Business logic controller
    private final int currentUserId;               // Current user for audit logging
    private final JTable table;                    // Displays inventory items
    private final DefaultTableModel model;         // Table data model
    private TableRowSorter<DefaultTableModel> sorter;  // For filtering/searching

    /**
     * Constructor - Initializes the borrow/return panel
     *
     * Sets up the UI with:
     * - Inventory table showing all items
     * - Borrow and Return action buttons
     * - Search/filter capability
     *
     * @param manager Library manager for data operations
     * @param userId  Current user ID for transaction logging
     */
    public BorrowPanel(LibraryManager manager, int userId) {
        this.manager = manager;
        this.currentUserId = userId;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ==================== TABLE SETUP ====================
        // Define table columns for inventory display
        String[] columns = {"ID", "Type", "Title", "Author", "Year", "Available", "Total"};

        // Create non-editable table model (read-only display)
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;  // Prevent direct editing
            }
        };

        // Initialize table with sorting capability
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // ==================== ACTION BUTTONS ====================
        // Panel containing borrow and return buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        // Create large, prominent buttons for main actions
        JButton borrowBtn = new JButton("Borrow Selected Item");
        JButton returnBtn = new JButton("Return Selected Item");
        borrowBtn.setPreferredSize(new Dimension(220, 45));  // Large button size
        returnBtn.setPreferredSize(new Dimension(220, 45));

        // Add buttons to panel
        buttonPanel.add(borrowBtn);
        buttonPanel.add(returnBtn);

        // ==================== LAYOUT ASSEMBLY ====================
        add(new JScrollPane(table), BorderLayout.CENTER);  // Table in center
        add(buttonPanel, BorderLayout.SOUTH);              // Buttons at bottom

        // ==================== EVENT LISTENERS ====================
        // Attach handlers to buttons
        borrowBtn.addActionListener(e -> handleBorrowAction());
        returnBtn.addActionListener(e -> handleReturnAction());

        // Load initial inventory data
        refreshTable();
    }

    /**
     * Handles the borrow transaction workflow
     *
     * Process flow:
     * 1. Validate item selection
     * 2. Request student ID
     * 3. Validate student exists
     * 4. Check item availability
     * 5. Verify student doesn't already have this item (duplicate prevention)
     * 6. Process loan or add to waitlist
     *
     * Business rules:
     * - Item must be selected
     * - Student must be registered
     * - Student cannot borrow same item twice (duplicate loan prevention)
     * - If unavailable, offer waitlist option
     */
    private void handleBorrowAction() {
        // Step 1: Validate item selection
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to borrow.");
            return;
        }

        // Convert view index to model index (handles sorting)
        int mRow = table.convertRowIndexToModel(row);
        String itemId = (String) model.getValueAt(mRow, 0);

        // Find the actual item object in inventory
        LibraryItem item = manager.getInventory().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElse(null);

        // Step 2: Request student ID from user
        String sId = JOptionPane.showInputDialog(this, "Enter Student ID borrowing this Item:");

        // Step 3: Validate student ID was provided
        if (sId == null || sId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type in a Student's ID.");
            return;
        }

        // Step 4: Find student in system
        Student student = manager.findStudentById(sId.trim());
        if (student == null) {
            // Student not found - must be registered first
            JOptionPane.showMessageDialog(this,
                    "This Student is not found. Please register student first.");
            return;
        }

        // Step 5: Check item availability
        if (item != null && item.getAvailableCopies() > 0) {
            // Item is available - attempt to borrow
            // manager.borrowItem returns false if student already has this item
            boolean success = manager.borrowItem(String.valueOf(currentUserId), student, item);

            if (success) {
                // Borrow successful - item wasn't already borrowed by this student
                JOptionPane.showMessageDialog(this, "Item borrow was Successful!");
                refreshTable();  // Update display to show new availability
            } else {
                // Borrow denied - duplicate loan prevention triggered
                // Student already has a copy of this specific item
                JOptionPane.showMessageDialog(this,
                        "Access Denied: " + student.getName() +
                                ". You already have this item in your possession!\n" +
                                "You must return this Item first before you can borrow again.",
                        "Duplicate Loan Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else if (item != null) {
            // Item exists but no copies available - offer waitlist
            handleWaitlistAddition(student, item);
        }
    }

    /**
     * Handles the return transaction workflow
     *
     * Process flow:
     * 1. Validate item selection
     * 2. Request student ID
     * 3. Validate student exists
     * 4. Process return transaction
     * 5. Check and process waitlist if applicable
     *
     * Important: After a successful return, the system automatically checks
     * if anyone is waiting for this item and offers to assign it to them.
     */
    private void handleReturnAction() {
        // Step 1: Validate item selection
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to return.");
            return;
        }

        // Get item details from table
        int mRow = table.convertRowIndexToModel(row);
        String itemId = (String) model.getValueAt(mRow, 0);
        LibraryItem item = manager.getInventory().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElse(null);

        // Step 2: Request student ID
        String sId = JOptionPane.showInputDialog(this, "Enter Student ID returning this item:");

        // Step 3: Validate student ID provided
        if (sId == null) {
            JOptionPane.showMessageDialog(this,
                    "This Student is not found. Please register student first.");
            return;
        }

        // Find student in system
        Student student = manager.findStudentById(sId.trim());

        // Step 4: Process return if both student and item are valid
        if (student != null && item != null) {
            // Execute the return transaction
            manager.returnItem(String.valueOf(currentUserId), student, item);

            // Step 5: Automatically check waitlist for this item
            // This ensures fair first-come-first-served distribution
            processWaitlist(item);

            // Update display to show new availability
            refreshTable();
        }
    }

    /**
     * Processes waitlist for a newly returned item
     *
     * When an item is returned, this method:
     * 1. Checks if anyone is waiting for this specific item
     * 2. Finds the first person in the waitlist queue
     * 3. Offers to immediately assign the item to them
     * 4. Validates they don't already have a copy (duplicate prevention)
     * 5. Completes the transaction if accepted
     *
     * This ensures fair "first-come-first-served" waitlist management.
     *
     * @param item The item that was just returned and is now available
     */
    private void processWaitlist(LibraryItem item) {
        // Build search string for this item in waitlist format
        // Format is: "Student Name (ID) -> Item Title"
        String target = "-> " + item.getTitle();

        // Get LIVE waitlist to ensure we have current data
        // (Important: don't use stale cached data)
        java.util.List<String> currentWaitlist = manager.getWaitlist();

        // Find first student waiting for this specific item
        String nextStudentEntry = currentWaitlist.stream()
                .filter(entry -> entry.contains(target))
                .findFirst()
                .orElse(null);

        // If someone is waiting, offer to assign the item to them
        if (nextStudentEntry != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Waitlist Alert!\n" + nextStudentEntry +
                            "\nDo you want to assign this Item to the student on queue now?",
                    "Waitlist Management",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                try {
                    // Extract Student ID from waitlist entry
                    // Format is: "Name (ID) -> Title"
                    // We extract the ID from between the parentheses
                    String id = nextStudentEntry.substring(
                            nextStudentEntry.indexOf("(") + 1,
                            nextStudentEntry.indexOf(")"));

                    Student s = manager.findStudentById(id);

                    if (s != null) {
                        // Attempt to borrow for the waitlist student
                        // Returns false if they already have this item
                        boolean success = manager.borrowItem(
                                String.valueOf(currentUserId), s, item);

                        if (success) {
                            // Transaction successful - remove from waitlist
                            manager.removeWaitlistEntry(
                                    String.valueOf(currentUserId),
                                    currentWaitlist.indexOf(nextStudentEntry));

                            JOptionPane.showMessageDialog(this,
                                    "Waitlist fulfilled successfully.");
                        } else {
                            // Student already has this item - skip and notify
                            // This shouldn't normally happen but good to handle
                            JOptionPane.showMessageDialog(this,
                                    "Cannot fulfill waitlist: " + s.getName() +
                                            " already has a copy of this item.",
                                    "Waitlist Skipped",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception e) {
                    // Handle any parsing errors from waitlist entry format
                    JOptionPane.showMessageDialog(this,
                            "Error during auto-assign: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles adding a student to the waitlist
     *
     * Called when a student tries to borrow an item that has no available copies.
     * Offers to add them to a queue so they can be notified when it becomes available.
     *
     * @param s    Student requesting the item
     * @param item Item they want to borrow
     */
    private void handleWaitlistAddition(Student s, LibraryItem item) {
        // Confirm with user before adding to waitlist
        if (JOptionPane.showConfirmDialog(this,
                "This Item is Unavailable right Now. " +
                        "Do you want to add this student to waitlist Queue?",
                "Waitlist",
                JOptionPane.YES_NO_OPTION) == 0) {

            // Add student to waitlist for this item
            manager.addToWaitlist(String.valueOf(currentUserId), s, item);

            // Confirm addition
            JOptionPane.showMessageDialog(this,
                    "This Student: " + s.getName() + " has been added to waitlist.");
        }
    }

    /**
     * Refreshes the inventory table with current data
     *
     * Clears all existing rows and reloads from the library manager.
     * Shows current availability for all items.
     * Called after any transaction that changes inventory state.
     */
    public void refreshTable() {
        // Clear existing data
        model.setRowCount(0);

        // Add a row for each inventory item
        for (LibraryItem i : manager.getInventory()) {
            model.addRow(new Object[]{
                    i.getId(),                  // Item ID
                    i.getType(),                // Type (Book/Magazine/Journal)
                    i.getTitle(),               // Title
                    i.getAuthor(),              // Author
                    i.getYear(),                // Publication year
                    i.getAvailableCopies(),     // Currently available
                    i.getTotalCopies()          // Total owned
            });
        }
    }

    /**
     * Applies search filter to the inventory table
     *
     * Called by global search in MainWindow when user types in search box.
     * Filters table rows in real-time based on search text.
     *
     * @param text Search text to filter by (case-insensitive)
     */
    public void applyFilter(String text) {
        if (sorter != null) {
            // Apply case-insensitive regex filter
            // (?i) makes the regex case-insensitive
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }
}