package model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * BorrowRecord: Tracks the lifecycle of a single loan transaction.
 */
public class BorrowRecord implements Serializable {
    private LibraryItem item;     // The specific book/magazine being loaned
    private LocalDate borrowDate; // When the item was taken
    private LocalDate dueDate;    // When the item must be returned
    private LocalDate returnDate; // The actual date of return (null if still out)

    // Default constructor: Sets the timeline to "Today" with a 14-day window
    public BorrowRecord() {
        this.borrowDate = LocalDate.now();
        this.dueDate = borrowDate.plusDays(1);
    }

    // Overloaded constructor for a specific item
    public BorrowRecord(LibraryItem item) {
        this(item, LocalDate.now());
    }

    // Full constructor allowing custom start dates
    public BorrowRecord(LibraryItem item, LocalDate borrowDate) {
        this.item = item;
        this.borrowDate = (borrowDate != null) ? borrowDate : LocalDate.now();
        this.dueDate = this.borrowDate.plusDays(1); // Standard 2-week loan period
    }

    /**
     * Overdue Logic: Checks if the current date has passed the deadline.
     */
    public boolean isOverdue() {
        if (returnDate != null) return false; // If returned, it's no longer overdue
        return LocalDate.now().isAfter(dueDate); // Compares today against the deadline
    }

    // Standard Getters and Setters for record attributes
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
        return item.getTitle() + " (Due: " + dueDate + ")";
    }
}