package model;

import java.io.Serializable;

/**
 * Abstract base class for all library items.
 * Handles synchronized Total and Available copy logic.
 */
public abstract class LibraryItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id, title, author;
    private int year;
    private int totalCopies = 1;
    private int availableCopies = 1;

    public LibraryItem() {}

    public LibraryItem(String id, String title, String author, int year) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year;
        this.totalCopies = 1;
        this.availableCopies = 1;
    }

    public boolean isBorrowed() {
        return availableCopies < totalCopies;
    }

    public abstract String getType();

    // --- GETTERS ---
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getYear() { return year; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }

    // --- SETTERS ---
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setYear(int year) { this.year = year; }

    /**
     * Updates the total physical units while preserving current loan counts.
     */
    public void setTotalCopies(int n) {
        // Calculate items currently out on loan
        int currentBorrowed = Math.max(0, this.totalCopies - this.availableCopies);

        this.totalCopies = n;

        // Calculate new available count based on new total minus items still out
        int newAvailable = n - currentBorrowed;

        // Boundary check: if total is reduced below current loans
        if (newAvailable < 0) {
            this.availableCopies = 0;
        } else {
            this.availableCopies = newAvailable;
        }
    }

    /**
     * Updates available units with strict boundary checks.
     */
    public void setAvailableCopies(int n) {
        if (n > totalCopies) {
            this.availableCopies = totalCopies;
        } else if (n < 0) {
            this.availableCopies = 0;
        } else {
            this.availableCopies = n;
        }
    }
}