package model;

import java.io.Serializable;

/**
 * Journal - Represents an academic or professional journal in the library
 *
 * Journals are scholarly publications that can be borrowed by students.
 * This is one of three main item types in the library system (Book, Magazine, Journal).
 *
 * Examples include:
 * - Academic research journals
 * - Scientific publications
 * - Professional periodicals
 * - Medical journals
 *
 * Extends LibraryItem to inherit common properties.
 * Implements Serializable to allow persistent storage.
 */
public class Journal extends LibraryItem implements Serializable {
    // Serial version UID for serialization compatibility
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor - Creates an empty journal object
     * Used primarily for deserialization (loading from disk)
     */
    public Journal() {
        super();
    }

    /**
     * Primary constructor - Creates a new journal with full details
     *
     * @param id     Unique identifier for this journal (auto-generated)
     * @param title  Journal title/name
     * @param author Publisher or editor name
     * @param year   Publication year or volume year
     */
    public Journal(String id, String title, String author, int year) {
        // Call parent constructor to initialize common fields
        super(id, title, author, year);
    }

    /**
     * Returns the type identifier for this library item
     *
     * Used to distinguish journals from books and magazines in:
     * - User interface displays
     * - Search and filter operations
     * - Type-specific business logic
     *
     * @return Always returns "Journal" for journal instances
     */
    @Override
    public String getType() {
        return "Journal";
    }
}