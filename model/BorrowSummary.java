package model;

import java.io.Serializable;
import java.util.List;

/** Read-only DTO carrying all statistics needed by the Reports dialog. */
public class BorrowSummary implements Serializable {
    public final int bookTitles, magTitles, journalTitles;
    public final int bookCopies, magCopies, journalCopies;
    public final int totalCopies, borrowedCount, waitlistCount;
    public final List<String> mostBorrowedLines; // e.g. "  2x  Clean Code by R. Martin [Book]"
    public final List<String> overdueLines;      // e.g. "  Alice (STU001) — Clean Code — 3 day(s) overdue"

    /** Creates a borrow summary with all statistics and formatted text lines. */
    public BorrowSummary(int bookTitles, int magTitles, int journalTitles,
                         int bookCopies, int magCopies, int journalCopies,
                         int totalCopies, int borrowedCount, int waitlistCount,
                         List<String> mostBorrowedLines, List<String> overdueLines) {
        this.bookTitles = bookTitles; this.magTitles = magTitles; this.journalTitles = journalTitles;
        this.bookCopies = bookCopies; this.magCopies = magCopies; this.journalCopies = journalCopies;
        this.totalCopies = totalCopies; this.borrowedCount = borrowedCount; this.waitlistCount = waitlistCount;
        this.mostBorrowedLines = List.copyOf(mostBorrowedLines);
        this.overdueLines      = List.copyOf(overdueLines);
    }
}
