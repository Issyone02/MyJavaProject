package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Student - Represents a registered library user
 *
 * Tracks:
 * - Student identification (ID and name)
 * - Current borrowed items
 * - Borrowing history
 * - Status (Active, Waiting, Inactive)
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;                           // Unique ID (must be unique!)
    private String name;                                 // Student full name
    private List<BorrowRecord> currentLoans = new ArrayList<>();   // Active loans
    private List<BorrowRecord> history = new ArrayList<>();        // Past loans


    /** Default constructor */
    public Student() {}

    /** Primary constructor */
    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }


    /**
     * Copy Constructor for Deep Cloning.
     * Crucial for the Undo/Redo system to prevent state contamination.
     */
    public Student(Student other) {
        if (other == null) return;
        this.studentId = other.studentId;
        this.name = other.name;

        // Deep copy of active loans
        for (BorrowRecord record : other.currentLoans) {
            this.currentLoans.add(new BorrowRecord(record));
        }
        // Deep copy of loan history
        for (BorrowRecord record : other.history) {
            this.history.add(new BorrowRecord(record));
        }
    }

    /**
     * NEW: Determines student status in real-time.
     @param systemWaitlist Global waitlist to check against
      * @return "Active", "Waiting in Queue", or "Inactive"
     */
    public String calculateStatus(List<String> systemWaitlist) {
        // Check if has active loans
        if (!currentLoans.isEmpty()) {
            return "Active (" + currentLoans.size() + " items)";
        }


        // Check if student ID exists in any waitlist entry string
        String searchTag = "(" + this.studentId + ")";
        for (String entry : systemWaitlist) {
            if (entry.contains(searchTag)) {
                return "Waiting in Queue";
            }
        }

        return "Inactive";
    }


    /**
     * Adds a newly borrowed item to current loans
     * @param item Item being borrowed
     */
    public void addBorrowedItem(LibraryItem item) {
        BorrowRecord record = new BorrowRecord(item);
        currentLoans.add(record);
    }


    // Standard Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<BorrowRecord> getCurrentLoans() { return currentLoans; }
    public List<BorrowRecord> getHistory() { return history; }
}