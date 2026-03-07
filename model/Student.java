package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String name;
    private List<BorrowRecord> currentLoans = new ArrayList<>();
    private List<BorrowRecord> history = new ArrayList<>();

    public Student() {}

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
     * Checks if the student is currently borrowing items or is on a waitlist.
     */
    public String calculateStatus(List<String> systemWaitlist) {
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