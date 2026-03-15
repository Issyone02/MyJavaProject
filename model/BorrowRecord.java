package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Records a single borrow transaction — item, dates, and return status. */
public class BorrowRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    static final int LOAN_PERIOD_DAYS = 14;

    private LibraryItem item;
    private LocalDate   borrowDate, dueDate;
    private LocalDate   returnDate; // null = still out

    /** Creates a borrow record with item and today's date. */
    public BorrowRecord(LibraryItem item) { this(item, LocalDate.now()); }

    /** Creates a borrow record with item and specified borrow date. Sets due date 14 days later. */
    public BorrowRecord(LibraryItem item, LocalDate borrowDate) {
        this.item       = item;
        this.borrowDate = borrowDate != null ? borrowDate : LocalDate.now();
        this.dueDate    = this.borrowDate.plusDays(LOAN_PERIOD_DAYS);
    }

    /** Copy constructor — used by undo/redo deep-copy. */
    public BorrowRecord(BorrowRecord other) {
        this.item = other.item; this.borrowDate = other.borrowDate;
        this.dueDate = other.dueDate; this.returnDate = other.returnDate;
    }

    /** Returns true if item is overdue (not returned and past due date). */
    public boolean isOverdue()        { return returnDate == null && LocalDate.now().isAfter(dueDate); }
    /** Returns number of days overdue. Returns 0 if not overdue. */
    public long    getDaysOverdue()    { return isOverdue() ? ChronoUnit.DAYS.between(dueDate, LocalDate.now()) : 0; }
    /** Returns days remaining until due date. Returns 0 if already returned. */
    public long    getDaysRemaining()  { return returnDate != null ? 0 : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), dueDate)); }

    public LibraryItem getItem()       { return item; }
    public LocalDate   getBorrowDate() { return borrowDate; }
    public LocalDate   getDueDate()    { return dueDate; }
    public LocalDate   getReturnDate() { return returnDate; }
    /** Sets the return date when item is returned. null means still borrowed. */
    public void        setReturnDate(LocalDate d) { this.returnDate = d; }

    /** Returns formatted string showing item title and current status. */
    @Override
    public String toString() {
        String status = returnDate != null ? "Returned " + returnDate
                : isOverdue() ? "OVERDUE by " + getDaysOverdue() + " day(s)" : "Due " + dueDate;
        return (item != null ? item.getTitle() : "Unknown") + " — " + status;
    }
}
