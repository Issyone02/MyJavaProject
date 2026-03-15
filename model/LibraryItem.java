package model;

import java.io.Serializable;
import java.time.Year;

/** Abstract base for all catalogue items. Subclasses implement getType(). */
public abstract class LibraryItem implements Serializable, Borrowable {
    private static final long serialVersionUID = 1L;
    private static final int MIN_YEAR = 1000;

    private String id, title, author;
    private int    year;
    private int    totalCopies     = 1;
    private int    availableCopies = 1;

    public LibraryItem() {}

    /** Creates a library item with ID, title, author, and year. Validates all fields. */
    public LibraryItem(String id, String title, String author, int year) {
        setId(id); setTitle(title); setAuthor(author); setYear(year);
    }

    /** Returns the type of library item (Book, Magazine, Journal). Must be implemented by subclasses. */
    public abstract String getType();

    // ── Validated setters ────────────────────────────────────────────────────

    /** Sets item ID with validation. Throws exception for empty ID. */
    public void setId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Item ID cannot be empty.");
        this.id = id.trim();
    }
    /** Sets item title with validation. Throws exception for empty title. */
    public void setTitle(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title cannot be empty.");
        this.title = title.trim();
    }
    /** Sets item author with validation. Throws exception for empty author. */
    public void setAuthor(String author) {
        if (author == null || author.isBlank()) throw new IllegalArgumentException("Author cannot be empty.");
        this.author = author.trim();
    }
    /** Sets publication year with validation. Must be between year 1000 and next year. */
    public void setYear(int year) {
        int max = Year.now().getValue() + 1;
        if (year < MIN_YEAR || year > max)
            throw new IllegalArgumentException("Year must be " + MIN_YEAR + "–" + max + ". Got: " + year);
        this.year = year;
    }

    /** Sets total copies while preserving borrowed count. Adjusts available copies accordingly. */
    public void setTotalCopies(int n) {
        if (n < 1) throw new IllegalArgumentException("Total copies must be at least 1.");
        int borrowed = Math.max(0, totalCopies - availableCopies);
        totalCopies     = n;
        availableCopies = Math.max(0, n - borrowed);
    }

    /** Sets available copies with validation. Clamps value to range [0, totalCopies]. */
    public void setAvailableCopies(int n) {
        if (n < 0) throw new IllegalArgumentException("Available copies cannot be negative.");
        availableCopies = Math.min(n, totalCopies);
    }

    // ── Borrowable implementation ────────────────────────────────────────────

    /** Attempts to borrow an item. Returns false if no copies are available. */
    @Override
    public boolean checkout() {
        if (getAvailableCopies() <= 0) return false;
        setAvailableCopies(getAvailableCopies() - 1);
        return true;
    }

    /** Returns a borrowed item, incrementing available copies. */
    @Override
    public void checkin() { setAvailableCopies(getAvailableCopies() + 1); }

    /** Checks if at least one copy is available for borrowing. */
    @Override
    public boolean isAvailable() { return getAvailableCopies() > 0; }

    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public String getAuthor()          { return author; }
    public int    getYear()            { return year; }
    public int    getTotalCopies()     { return totalCopies; }
    public int    getAvailableCopies() { return availableCopies; }

    /** Returns formatted string representation with ID, title, author, year, type, and availability. */
    @Override
    public String toString() {
        return "[" + id + "] " + title + " by " + author + " (" + year + ") — "
             + getType() + " [" + availableCopies + "/" + totalCopies + "]";
    }
}
