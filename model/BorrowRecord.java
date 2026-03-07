package model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * BorrowRecord: Tracks the lifecycle of a single borrow transaction.
 */
public class BorrowRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private LibraryItem item;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    public BorrowRecord() {
        this.borrowDate = LocalDate.now();
        // Keep your 1-day borrowing logic
        this.dueDate = borrowDate.plusDays(1);
    }

    public BorrowRecord(LibraryItem item) {
        this(item, LocalDate.now());
    }

    public BorrowRecord(LibraryItem item, LocalDate borrowDate) {
        this.item = item;
        this.borrowDate = (borrowDate != null) ? borrowDate : LocalDate.now();
        // Keep your 1-day borrowing logic
        this.dueDate = this.borrowDate.plusDays(1);
    }

    public BorrowRecord(BorrowRecord other) {
        this.item = other.item;
        this.borrowDate = other.borrowDate;
        this.dueDate = other.dueDate;
        this.returnDate = other.returnDate;
    }

    public boolean isOverdue() {
        if (returnDate != null) return false;
        return LocalDate.now().isAfter(dueDate);
    }

    // Standard Getters and Setters
    public LibraryItem getItem() { return item; }
    public void setItem(LibraryItem item) { this.item = item; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    @Override
    public String toString() {
        return (item != null ? item.getTitle() : "Unknown Item") + " (Due: " + dueDate + ")";
    }
}