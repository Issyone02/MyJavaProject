package model;

/** Implemented by all borrowable item types (Book, Magazine, Journal). */
public interface Borrowable {
    /** Attempts to borrow an item. Returns false if no copies available. */
    boolean checkout();   // returns false if no copies available
    
    /** Returns a borrowed item, incrementing available copies. */
    void    checkin();
    
    /** Checks if at least one copy is available for borrowing. */
    boolean isAvailable();
}
