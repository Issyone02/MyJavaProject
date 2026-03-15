package model;

/**
 * Represents a Book in the library catalogue.
 * Extends LibraryItem (common fields) and implements Borrowable (copy management).
 */
public class Book extends LibraryItem implements Borrowable {
    private static final long serialVersionUID = 1L;

    public Book() { super(); }

    public Book(String id, String title, String author, int year) {
        super(id, title, author, year);
    }

    @Override
    public String getType() { return "Book"; }

    /** Decrements available copies; returns false if none are free. */
    @Override
    public boolean checkout() {
        if (getAvailableCopies() <= 0) return false;
        setAvailableCopies(getAvailableCopies() - 1);
        return true;
    }

    /** Returns one copy back to the shelf. */
    @Override
    public void checkin() {
        setAvailableCopies(getAvailableCopies() + 1);
    }

    @Override
    public boolean isAvailable() {
        return getAvailableCopies() > 0;
    }
}
