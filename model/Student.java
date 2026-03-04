package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Student implements Serializable {
    // The unique ID that identifies this student
    private String studentId;
    //The Student's full name
    private String name;
    // Everything the student is currently borrowing
    private List<BorrowRecord> currentLoans = new ArrayList<>();
    // A record of everything they have borrowed in the past
    private List<BorrowRecord> history = new ArrayList<>();

    // A blank constructor - needed for deserialisation when loading saved data
    public Student() {}

    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }

    /**
     * Copy Constructor for Deep Cloning (Crucial for Sequential Undo/Redo)
     */
    public Student(Student other) {
        this.studentId = other.studentId;
        this.name = other.name;

        // We create new lists and copy the records to ensure history is isolated
        for (BorrowRecord record : other.currentLoans) {
            /**
             * Each borrowed item is copied individually so
             * changes to clone do not affect the original
             */
            this.currentLoans.add(new BorrowRecord(record));
        }
        for (BorrowRecord record : other.history) {
            this.history.add(new BorrowRecord(record));
        }
    }

    public void addBorrowedItem(LibraryItem item) {
        BorrowRecord record = new BorrowRecord(item);
        currentLoans.add(record);
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<BorrowRecord> getCurrentLoans() { return currentLoans; }
    public List<BorrowRecord> getHistory() { return history; }
}