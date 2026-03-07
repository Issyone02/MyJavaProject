package model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * BorrowRecord - Tracks a single borrow transaction
 *
 * Records complete lifecycle of a loan:
 * - When item was borrowed
 * - When it's due back
 * - When it was returned (if applicable)
 *
 * Used in Student class to track current loans and history.
 */
public class BorrowRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private LibraryItem item;        // The borrowed item
    private LocalDate borrowDate;    // When borrowed
    private LocalDate dueDate;       // When due back
    private LocalDate returnDate;    // When returned (null if still out)

    /** Default constructor - Creates record for today */
    public BorrowRecord() {
        this.borrowDate = LocalDate.now();
        this.dueDate = borrowDate.plusDays(1);  // 1-day loan period
    }

    /** Constructor with item */
    public BorrowRecord(LibraryItem item) {
        this(item, LocalDate.now());
    }

    /** Constructor with item and custom borrow date */
    public BorrowRecord(LibraryItem item, LocalDate borrowDate) {
        this.item = item;
        this.borrowDate = (borrowDate != null) ? borrowDate : LocalDate.now();
        this.dueDate = this.borrowDate.plusDays(1);  // 1-day loan period
    }

    /** Copy constructor - For undo/redo functionality */
    public BorrowRecord(BorrowRecord other) {
        this.item = other.item;
        this.borrowDate = other.borrowDate;
        this.dueDate = other.dueDate;
        this.returnDate = other.returnDate;
    }

    /**
     * Checks if this loan is overdue
     * @return true if past due date and not yet returned
     */
    public boolean isOverdue() {
        if (returnDate != null) return false;  // Already returned
        return LocalDate.now().isAfter(dueDate);
    }

    // Getters and Setters
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