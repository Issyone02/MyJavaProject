package model;

import java.io.Serializable;

/**
 * Abstract base class for all library items.
 * Fixed: Synchronized Total and Available copy logic to prevent column mismatch.
 */
public abstract class LibraryItem implements Serializable {
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
        // Initial state: 1 total unit, 1 available unit.
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
     * Updates the total physical units.
     * Logic: Maintains the existing "Borrowed" gap by applying the change to Available count.
     */
    public void setTotalCopies(int n) {
        // Calculate the change in stock
        int difference = n - this.totalCopies;

        // Update total
        this.totalCopies = n;

        // Apply the same change to available copies (Synchronized Scaling)
        int newAvailable = this.availableCopies + difference;

        // Use the safety-checked setter to finalize the value
        setAvailableCopies(newAvailable);
    }

    /**
     * Updates available units with strict boundary checks.
     */
    public void setAvailableCopies(int n) {
        // Safety guard: Available units cannot exceed physical total or be negative
        if (n > totalCopies) {
            this.availableCopies = totalCopies;
        } else if (n < 0) {
            this.availableCopies = 0;
        } else {
            this.availableCopies = n;
        }
    }
}