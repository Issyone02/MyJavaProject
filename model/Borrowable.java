package model;

/**
 * Interface for library items that can be checked out and returned.
 * Implemented by: Book, Magazine, Journal.
 */
public interface Borrowable {

    // Checks out one copy. Returns false if none available.
    boolean checkout();

    // Returns one copy back to the shelf.
    void checkin();

    // Returns true if at least one copy is available.
    boolean isAvailable();
}
