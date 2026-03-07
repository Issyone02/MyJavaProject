package model;

import java.io.Serializable;

/**
 * Magazine - Represents a magazine or periodical in the library
 *
 * Magazines are general-interest publications that can be borrowed by students.
 * This is one of three main item types in the library system (Book, Magazine, Journal).
 *
 * Examples include:
 * - General interest magazines
 * - Trade publications
 * - News magazines
 * - Hobby and lifestyle magazines
 *
 * Extends LibraryItem to inherit common properties.
 * Implements Serializable for persistent storage.
 */
public class Magazine extends LibraryItem implements Serializable {
    // Serial version UID for serialization compatibility
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor - Creates an empty magazine object
     * Used primarily for deserialization (loading from disk)
     */
    public Magazine() {
        super();
    }

    /**
     * Primary constructor - Creates a new magazine with full details
     *
     * @param id     Unique identifier for this magazine (auto-generated)
     * @param title  Magazine title/name
     * @param author Publisher or editor name
     * @param year   Publication year or issue year
     */
    public Magazine(String id, String title, String author, int year) {
        // Call parent constructor to initialize common fields
        super(id, title, author, year);
    }

    /**
     * Returns the type identifier for this library item
     *
     * Used throughout the system to:
     * - Display correct item type in UI
     * - Enable type-based filtering
     * - Support polymorphic behavior
     *
     * @return Always returns "Magazine" for magazine instances
     */
    @Override
    public String getType() {
        return "Magazine";
    }
}