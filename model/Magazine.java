package model;

import java.io.Serializable;

/**
 * Represents a Magazine in the library catalogue.
 * Extends LibraryItem (common fields) and implements Borrowable (copy management).
 */
public class Magazine extends LibraryItem implements Serializable, Borrowable {
    private static final long serialVersionUID = 1L;

    public Magazine() { super(); }

    public Magazine(String id, String title, String author, int year) {
        super(id, title, author, year);
    }

    @Override
    public String getType() { return "Magazine"; }

    @Override
    public boolean checkout() {
        if (getAvailableCopies() <= 0) return false;
        setAvailableCopies(getAvailableCopies() - 1);
        return true;
    }

    @Override
    public void checkin() {
        setAvailableCopies(getAvailableCopies() + 1);
    }

    @Override
    public boolean isAvailable() {
        return getAvailableCopies() > 0;
    }
}
