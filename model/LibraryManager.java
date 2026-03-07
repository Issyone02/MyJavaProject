package model;

import java.util.*;
import java.io.Serializable;
import utils.FileHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LibraryManager - Core business logic controller for the Smart Library System
 * Manages all library operations including inventory, students, borrowing, and system state
 * Implements undo/redo functionality for all state-changing operations
 */
public class LibraryManager implements Serializable {
    private static final long serialVersionUID = 1L;

    // Core data structures for library management
    private List<LibraryItem> inventory;      // All library items (books, magazines, journals)
    private List<Student> students;           // Registered students in the system
    private List<String> waitlist;            // Students waiting for unavailable items
    private List<SystemLog> systemLogs = new ArrayList<>();  // Audit trail of all system operations

    // Undo/Redo stacks to track system state changes
    private final Stack<LibraryState> undoHistory = new Stack<>();
    private final Stack<LibraryState> redoHistory = new Stack<>();

    /**
     * Constructor - Initializes the library manager by loading all persisted data
     * Creates empty collections if no saved data exists
     */
    public LibraryManager() {
        // Load persisted data from disk
        this.inventory = FileHandler.loadData();
        this.students = FileHandler.loadStudents();
        this.waitlist = FileHandler.loadWaitlist();

        // Initialize empty collections if no data was found
        if (this.inventory == null) this.inventory = new ArrayList<>();
        if (this.students == null) this.students = new ArrayList<>();
        if (this.waitlist == null) this.waitlist = new ArrayList<>();

        // Save initial state to enable undo/redo from the start
        saveState(false);
    }

    /**
     * LibraryState - Immutable snapshot of library state for undo/redo operations
     * Stores deep copies of all collections to prevent reference issues
     */
    private static class LibraryState implements Serializable {
        final List<LibraryItem> inv;    // Inventory snapshot
        final List<Student> std;         // Students snapshot
        final List<String> wait;         // Waitlist snapshot

        /**
         * Creates a deep copy snapshot of current library state
         * @param i Current inventory list
         * @param s Current students list
         * @param w Current waitlist
         */
        LibraryState(List<LibraryItem> i, List<Student> s, List<String> w) {
            // Create deep copies to ensure state independence
            this.inv = new ArrayList<>(i);
            this.wait = new ArrayList<>(w);
            this.std = new ArrayList<>();
            // Deep copy students to preserve their loan records
            for (Student student : s) {
                this.std.add(new Student(student));
            }
        }
    }

    // ==================== UNDO / REDO SYSTEM ====================

    /**
     * Saves current state to undo history
     * Maintains a rolling history of up to 50 states
     * @param isUndoOrRedo If true, preserves redo history; if false, clears it
     */
    public void saveState(boolean isUndoOrRedo) {
        // Push current state onto undo stack
        undoHistory.push(new LibraryState(inventory, students, waitlist));

        // Limit history to prevent memory overflow (keep last 50 states)
        if (undoHistory.size() > 50) undoHistory.remove(0);

        // Clear redo history unless this is an undo/redo operation itself
        if (!isUndoOrRedo) redoHistory.clear();

        // Persist current state to disk
        FileHandler.saveAll(inventory, students, waitlist);
    }

    /**
     * Reverts to previous system state (Undo operation)
     * Moves current state to redo stack before reverting
     * @param userName User performing the undo (for audit log)
     */
    public void undo(String userName) {
        // Ensure we have at least 2 states (current + previous)
        if (undoHistory.size() > 1) {
            // Move current state to redo stack
            redoHistory.push(undoHistory.pop());

            // Get the previous state (without removing it)
            LibraryState previous = undoHistory.peek();

            // Restore system to previous state
            restoreFromState(previous);

            // Log the undo operation
            addLog(userName, "UNDO", "Global state reverted.");
        }
    }

    /**
     * Restores a previously undone state (Redo operation)
     * Moves state from redo stack back to undo stack
     * @param userName User performing the redo (for audit log)
     */
    public void redo(String userName) {
        // Check if there are any states to redo
        if (!redoHistory.isEmpty()) {
            // Get the next state from redo stack
            LibraryState nextState = redoHistory.pop();

            // Save current state before restoring (for potential undo)
            undoHistory.push(new LibraryState(inventory, students, waitlist));

            // Restore the next state
            restoreFromState(nextState);

            // Log the redo operation
            addLog(userName, "REDO", "Global state restored.");
        }
    }

    /**
     * Restores library state from a snapshot
     * Creates new instances to avoid reference issues
     * @param state The state snapshot to restore
     */
    private void restoreFromState(LibraryState state) {
        // Create fresh copies of all collections from the state
        this.inventory = new ArrayList<>(state.inv);
        this.students = new ArrayList<>(state.std);
        this.waitlist = new ArrayList<>(state.wait);

        // Persist restored state to disk
        FileHandler.saveAll(inventory, students, waitlist);
    }

    // ==================== INVENTORY METHODS ====================

    /**
     * Updates an existing library item's details
     * Preserves the number of currently borrowed copies
     * @param userId User making the change
     * @param id Item ID to update
     * @param type New item type (Book, Magazine, or Journal)
     * @param title New title
     * @param author New author
     * @param year New publication year
     * @param total New total quantity
     * @param reason Reason for the update (for audit trail)
     */
    public void updateItem(String userId, String id, String type, String title, String author, int year, int total, String reason) {
        // Save state before making changes
        saveState(false);

        // Find and update the item
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getId().equals(id)) {
                // Calculate current borrowed count to preserve it
                int borrowed = inventory.get(i).getTotalCopies() - inventory.get(i).getAvailableCopies();

                // Create new item with updated details based on type
                LibraryItem newItem;
                if (type.equalsIgnoreCase("Book")) newItem = new Book(id, title, author, year);
                else if (type.equalsIgnoreCase("Magazine")) newItem = new Magazine(id, title, author, year);
                else newItem = new Journal(id, title, author, year);

                // Set quantities (maintaining borrowed count)
                newItem.setTotalCopies(total);
                newItem.setAvailableCopies(total - borrowed);

                // Replace old item with updated one
                inventory.set(i, newItem);

                // Log the update operation
                addLog(userId, "UPDATE", "Item " + id + " updated: " + reason);
                break;
            }
        }
    }

    /**
     * Adds a new item to the library inventory
     * @param item The library item to add
     */
    public void addItem(LibraryItem item) {
        saveState(false);
        inventory.add(item);
    }

    /**
     * Removes an item from the library inventory
     * @param id ID of the item to remove
     * @return true if item was found and removed, false otherwise
     */
    public boolean removeItem(String id) {
        saveState(false);
        return inventory.removeIf(i -> i.getId().equals(id));
    }

    /**
     * Processes a borrow request for a library item
     * Prevents duplicate borrowing of the same item by the same student
     * @param userId User processing the transaction
     * @param s Student borrowing the item
     * @param item Item being borrowed
     * @return true if loan successful, false if denied (e.g., already borrowed)
     */
    public boolean borrowItem(String userId, Student s, LibraryItem item) {
        // Check if student already has this specific item checked out
        boolean alreadyHasItem = s.getCurrentLoans().stream()
                .anyMatch(record -> record.getItem().getId().equalsIgnoreCase(item.getId().trim()));

        if (alreadyHasItem) {
            // Log the blocked attempt for security/audit purposes
            addLog(userId, "BORROW_DENIED", s.getName() + " already possesses " + item.getTitle());
            return false; // Loan rejected - student already has this item
        }

        // Proceed with borrowing if validation passes
        saveState(false);

        // Decrease available copies
        item.setAvailableCopies(item.getAvailableCopies() - 1);

        // Add borrow record to student's active loans
        s.getCurrentLoans().add(new BorrowRecord(item, java.time.LocalDate.now()));

        // Log successful borrow
        addLog(userId, "BORROW", s.getName() + " borrowed " + item.getTitle());
        return true; // Loan successful
    }

    /**
     * Processes the return of a borrowed item
     * @param userId User processing the return
     * @param s Student returning the item
     * @param item Item being returned
     */
    public void returnItem(String userId, Student s, LibraryItem item) {
        // Remove the borrow record from student's loans
        boolean removed = s.getCurrentLoans().removeIf(r -> r.getItem().getId().equals(item.getId()));

        if (removed) {
            // Save state and update availability
            saveState(false);
            item.setAvailableCopies(item.getAvailableCopies() + 1);

            // Log the return
            addLog(userId, "RETURN", item.getTitle() + " returned by " + s.getName());
        }
    }

    // ==================== STUDENT METHODS ====================

    /**
     * Updates a student's information
     * @param id Student ID
     * @param newName New name for the student
     */
    public void updateStudent(String id, String newName) {
        saveState(false);
        for (Student s : students) {
            if (s.getStudentId().equals(id)) {
                s.setName(newName);
                break;
            }
        }
    }

    /**
     * Adds a new student to the system with duplicate ID checking
     * @param s The student to add
     * @return true if student was added, false if ID already exists
     */
    public boolean addStudent(Student s) {
        // CRITICAL FIX #1: Check for duplicate student ID before adding
        // Prevents data integrity issues and duplicate entries
        boolean idExists = students.stream()
                .anyMatch(existing -> existing.getStudentId().equalsIgnoreCase(s.getStudentId().trim()));

        if (idExists) {
            // Reject the addition - student ID already exists
            return false;
        }

        // ID is unique - proceed with adding the student
        saveState(false);
        students.add(s);
        return true;
    }

    /**
     * Removes a student from the system
     * @param id Student ID to remove
     */
    public void removeStudent(String id) {
        saveState(false);
        students.removeIf(s -> s.getStudentId().equals(id));
    }

    // ==================== WAITLIST METHODS ====================

    /**
     * Adds a student to the waitlist for an unavailable item
     * @param userId User adding to waitlist
     * @param s Student to add
     * @param item Item they're waiting for
     */
    public void addToWaitlist(String userId, Student s, LibraryItem item) {
        saveState(false);
        // Format: "Student Name (ID) -> Item Title"
        waitlist.add(s.getName() + " (" + s.getStudentId() + ") -> " + item.getTitle());
        addLog(userId, "WAITLIST", "Added student to waitlist for " + item.getTitle());
    }

    /**
     * Moves a waitlist entry up or down in priority
     * @param userId User making the change
     * @param index Current position of the entry
     * @param up true to move up, false to move down
     */
    public void moveWaitlistEntry(String userId, int index, boolean up) {
        if (up && index > 0) {
            saveState(false);
            Collections.swap(waitlist, index, index - 1);
        } else if (!up && index < waitlist.size() - 1) {
            saveState(false);
            Collections.swap(waitlist, index, index + 1);
        }
    }

    /**
     * Removes a specific waitlist entry
     * @param userId User removing the entry
     * @param index Position of entry to remove
     */
    public void removeWaitlistEntry(String userId, int index) {
        if (index >= 0 && index < waitlist.size()) {
            saveState(false);
            waitlist.remove(index);
        }
    }

    /**
     * Clears all waitlist entries
     * @param userId User clearing the waitlist
     */
    public void clearWaitlist(String userId) {
        saveState(false);
        waitlist.clear();
        addLog(userId, "CLEAR", "Waitlist cleared.");
    }

    // ==================== SORTING ALGORITHMS ====================

    /**
     * Sorts inventory by title using merge sort (case-insensitive)
     * Merge sort is stable and efficient for large datasets (O(n log n))
     */
    public void mergeSortByTitle() {
        if (inventory.size() < 2) return;
        saveState(false);
        inventory.sort(Comparator.comparing(LibraryItem::getTitle, String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Sorts inventory by publication year using selection sort
     * Selection sort is O(n²) but simple and works well for small datasets
     */
    public void selectionSortByYear() {
        saveState(false);
        int n = inventory.size();

        // Selection sort algorithm
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            // Find the minimum year in remaining unsorted portion
            for (int j = i + 1; j < n; j++) {
                if (inventory.get(j).getYear() < inventory.get(minIdx).getYear()) minIdx = j;
            }
            // Swap current position with minimum
            Collections.swap(inventory, i, minIdx);
        }
    }

    /**
     * Sorts inventory by item type (Book, Journal, Magazine)
     */
    public void sortByType() {
        saveState(false);
        inventory.sort(Comparator.comparing(LibraryItem::getType));
    }

    // ==================== LOGGING & GETTERS ====================

    /**
     * Adds an entry to the system audit log
     * All major operations are logged for security and accountability
     * @param u User ID or username
     * @param a Action type (e.g., "BORROW", "RETURN", "ADD_ITEM")
     * @param d Detailed description of the action
     */
    public void addLog(String u, String a, String d) {
        systemLogs.add(new SystemLog(u, a, d));
        FileHandler.logStealthActivity("User: " + u + " | " + a + " | " + d);
    }

    /**
     * Returns all system logs (for audit trail display)
     */
    public List<SystemLog> getSystemLogs() { return systemLogs; }

    /**
     * Returns current library inventory
     */
    public List<LibraryItem> getInventory() { return inventory; }

    /**
     * Returns all registered students
     */
    public List<Student> getStudents() { return students; }

    /**
     * Returns current waitlist
     */
    public List<String> getWaitlist() { return waitlist; }

    /**
     * Finds a student by their ID (case-insensitive)
     * @param id Student ID to search for
     * @return Student object if found, null otherwise
     */
    public Student findStudentById(String id) {
        return students.stream()
                .filter(s -> s.getStudentId().equalsIgnoreCase(id.trim()))
                .findFirst()
                .orElse(null);
    }
}