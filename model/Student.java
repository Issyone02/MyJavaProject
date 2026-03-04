package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Student implements Serializable {
    private String studentId;
    private String name;
    private List<BorrowRecord> currentLoans = new ArrayList<>();
    private List<BorrowRecord> history = new ArrayList<>();

    public Student() {}
    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }

    // Logic updated to ensure BorrowRecords are created correctly
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