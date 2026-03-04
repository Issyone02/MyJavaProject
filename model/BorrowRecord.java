package model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * BorrowRecord: Tracks the lifecycle of a single borrow transaction.
 */
public class BorrowRecord implements Serializable {
    //The actual LibraryItem being borrowed
    private LibraryItem item;
    // The date the Item was taken out
    private LocalDate borrowDate;
    // The date the item needs to be returned back
    private LocalDate dueDate;
    /**
     * The date the item was actually returned -
     * This stays empty until the student returns it
     */
        private LocalDate returnDate;

    public BorrowRecord() {
        // Defaults the borrow date to today
        this.borrowDate = LocalDate.now();
        // Keep the borrow period to 1 day
        this.dueDate = borrowDate.plusDays(1); // Keep your 1-day logic as requested
    }

    public BorrowRecord(LibraryItem item) {
        /**
         * Delegates to the fuller constructor below -
         * Using today as the borrow date
         */
        this(item, LocalDate.now());
    }

    public BorrowRecord(LibraryItem item, LocalDate borrowDate) {
        this.item = item;
        /**
         *  Use the given date, but fall back to today
         *  if null is passed in
         */
        this.borrowDate = (borrowDate != null) ? borrowDate : LocalDate.now();
        this.dueDate = this.borrowDate.plusDays(1);
    }

    // Copy Constructor for Deep Cloning
    public BorrowRecord(BorrowRecord other) {
        // Reference to the item
        this.item = other.item;
        // Copy the borrow date
        this.borrowDate = other.borrowDate;
        this.dueDate = other.dueDate;
        this.returnDate = other.returnDate;
    }

    public boolean isOverdue() {
        /**
         * If the item is already returned,
         * it can not be overdue
         */
        if (returnDate != null) return false;
        // Otherwise, check if today has gone past the due date
        return LocalDate.now().isAfter(dueDate);
    }

    // Standard Getters and Getters for all fields in this record
    public LibraryItem getItem() { return item; }
    public void setItem(LibraryItem item) { this.item = item; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    /**
     * Shows the item title and due date, with a fallback if the item is
     * somehow missing
     */
    @Override
    public String toString() {
        return (item != null ? item.getTitle() : "Unknown Item") + " (Due: " + dueDate + ")";
    }
}