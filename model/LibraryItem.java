package model;

import java.io.Serializable;
import java.time.Year;

/**
 * Abstract base class for all library items (Book, Magazine, Journal).
 *
 * All fields are private with validated setters (encapsulation).
 * getType() is abstract — each subclass returns its own type string (polymorphism).
 */
public abstract class LibraryItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MIN_YEAR = 1000;

    private String id;
    private String title;
    private String author;
    private int    year;
    private int    totalCopies     = 1;
    private int    availableCopies = 1;

    public LibraryItem() {}

    public LibraryItem(String id, String title, String author, int year) {
        setId(id);
        setTitle(title);
        setAuthor(author);
        setYear(year);
    }

    // Each subclass returns "Book", "Magazine", or "Journal"
    public abstract String getType();

    // True when at least one copy is currently checked out
    public boolean isBorrowed() {
        return availableCopies < totalCopies;
    }

    // ── Validated setters — reject bad data before it enters the system ───

    public void setId(String id) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("Item ID cannot be empty.");
        this.id = id.trim();
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Title cannot be empty.");
        this.title = title.trim();
    }

    public void setAuthor(String author) {
        if (author == null || author.isBlank())
            throw new IllegalArgumentException("Author cannot be empty.");
        this.author = author.trim();
    }

    public void setYear(int year) {
        int maxYear = Year.now().getValue() + 1;
        if (year < MIN_YEAR || year > maxYear)
            throw new IllegalArgumentException(
                    "Year must be between " + MIN_YEAR + " and " + maxYear + ". Got: " + year);
        this.year = year;
    }

    // Adjusts total copies while preserving the number currently borrowed
    public void setTotalCopies(int n) {
        if (n < 1) throw new IllegalArgumentException("Total copies must be at least 1.");
        int borrowed = Math.max(0, totalCopies - availableCopies);
        totalCopies     = n;
        availableCopies = Math.max(0, n - borrowed);
    }

    // Clamps to [0, totalCopies] so available can never exceed physical stock
    public void setAvailableCopies(int n) {
        if (n < 0) throw new IllegalArgumentException("Available copies cannot be negative.");
        availableCopies = Math.min(n, totalCopies);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()               { return id; }
    public String getTitle()            { return title; }
    public String getAuthor()           { return author; }
    public int    getYear()             { return year; }
    public int    getTotalCopies()      { return totalCopies; }
    public int    getAvailableCopies()  { return availableCopies; }

    @Override
    public String toString() {
        return "[" + id + "] " + title + " by " + author + " (" + year + ") — "
             + getType() + " [" + availableCopies + "/" + totalCopies + "]";
    }
}
