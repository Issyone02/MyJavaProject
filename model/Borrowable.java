package model;

/**
 * Borrowable - Interface for items that can be borrowed and returned
 *
 * Defines the contract for borrowing transactions.
 * Originally designed for student-based borrowing with quantity tracking.
 *
 * Note: Current system handles borrowing through LibraryManager instead.
 * This interface is kept for potential future extensions.
 */
public interface Borrowable {
    /**
     * Processes a borrow transaction for a student
     *
     * @param student The student borrowing the item
     * @return true if successful (copies available), false otherwise
     */
    boolean borrowItem(Student student);

    /**
     * Processes a return transaction
     *
     * @param student The student returning the item
     */
    void returnItem(Student student);
}