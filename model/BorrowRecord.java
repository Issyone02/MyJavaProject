package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Records a single borrow transaction for a Student.
 * Tracks what was borrowed, when, when it's due, and when it was returned.
 * The standard loan period is fixed at 14 days.
 */
public class BorrowRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int LOAN_PERIOD_DAYS = 14;

    private LibraryItem item;
    private LocalDate   borrowDate;
    private LocalDate   dueDate;
    private LocalDate   returnDate;   // null = not yet returned

    public BorrowRecord() {
        this(null, LocalDate.now());
    }

    public BorrowRecord(LibraryItem item) {
        this(item, LocalDate.now());
    }

    // Full constructor — allows back-dating for data import
    public BorrowRecord(LibraryItem item, LocalDate borrowDate) {
        this.item       = item;
        this.borrowDate = (borrowDate != null) ? borrowDate : LocalDate.now();
        this.dueDate    = this.borrowDate.plusDays(LOAN_PERIOD_DAYS);
    }

    // Copy constructor — used by the undo/redo deep-copy mechanism
    public BorrowRecord(BorrowRecord other) {
        this.item       = other.item;
        this.borrowDate = other.borrowDate;
        this.dueDate    = other.dueDate;
        this.returnDate = other.returnDate;
    }

    // True when the item hasn't been returned and today is past the due date
    public boolean isOverdue() {
        return returnDate == null && LocalDate.now().isAfter(dueDate);
    }

    // How many days past the due date (0 if not overdue or already returned)
    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    // How many days left before due (0 if overdue or returned)
    public long getDaysRemaining() {
        if (returnDate != null) return 0;
        long remaining = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        return Math.max(0, remaining);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public LibraryItem getItem()                    { return item; }
    public void        setItem(LibraryItem item)    { this.item = item; }

    public LocalDate getBorrowDate()                    { return borrowDate; }
    public void      setBorrowDate(LocalDate d)         { this.borrowDate = d; }

    public LocalDate getDueDate()                       { return dueDate; }
    public void      setDueDate(LocalDate d)            { this.dueDate = d; }

    public LocalDate getReturnDate()                    { return returnDate; }
    public void      setReturnDate(LocalDate d)         { this.returnDate = d; }

    @Override
    public String toString() {
        String title  = (item != null) ? item.getTitle() : "Unknown";
        String status = (returnDate != null) ? "Returned " + returnDate
                      : isOverdue()          ? "OVERDUE by " + getDaysOverdue() + " day(s)"
                                             : "Due " + dueDate;
        return title + " — " + status;
    }
}
