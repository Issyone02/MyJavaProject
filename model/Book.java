package model;

import java.io.Serializable;

/**
 * Book - Represents a book in the library inventory
 *
 * Books are physical items that can be borrowed by students.
 * This is one of three main item types in the library system (Book, Magazine, Journal).
 *
 * Examples include:
 * - Textbooks for academic courses
 * - Novels and fiction books
 * - Reference materials
 * - Technical manuals
 *
 * Extends LibraryItem to inherit common properties like title, author, availability.
 * Implements Serializable to allow saving to disk.
 */
public class Book extends LibraryItem implements Serializable {
    // Serial version UID for serialization compatibility
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor - Creates an empty book object
     * Used primarily for deserialization (loading from disk)
     */
    public Book() {
        super();
    }

    /**
     * Primary constructor - Creates a new book with full details
     *
     * @param id     Unique identifier for this book (auto-generated)
     * @param title  Book title
     * @param author Author name (or "Unknown" if not available)
     * @param year   Publication year
     */
    public Book(String id, String title, String author, int year) {
        // Call parent constructor to initialize common fields
        super(id, title, author, year);
    }

    /**
     * Returns the type identifier for this library item
     *
     * Used throughout the system to:
     * - Display item type in tables
     * - Filter/sort by item type
     * - Create new instances of the correct type
     *
     * @return Always returns "Book" for book instances
     */
    @Override
    public String getType() {
        return "Book";
    }
}