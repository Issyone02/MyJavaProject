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
     * Logic: Captures items currently borrowed and ensures they remain borrowed
     * after the total quantity is changed.
     */
    public void setTotalCopies(int n) {
        // 1. Calculate how many items are currently with students (Borrowed)
        // Example: Total 10, Available 7 -> Borrowed = 3
        int currentBorrowed = this.totalCopies - this.availableCopies;

        // 2. Update the total to the new value (e.g., 15)
        this.totalCopies = n;

        // 3. Calculate new available count
        // New Total (15) - Still Borrowed (3) = New Available (12)
        int newAvailable = n - currentBorrowed;

        // 4. Safety Check: If user reduces total below the number of items currently out
        if (newAvailable < 0) {
            // This means we have more items borrowed than we now have in total.
            // We set available to 0 until items are returned.
            this.availableCopies = 0;
        } else {
            this.availableCopies = newAvailable;
        }
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