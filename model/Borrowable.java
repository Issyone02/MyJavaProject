package model;

/**
 * Updated interface to support Student-based borrowing
 * and quantity deduction.
 */
public interface Borrowable {
    /**
     * Handles the deduction of available copies and
     * link the item to the student's history.
     * @param student The student borrowing the item.
     * @return true if borrowing was successful (copies were available).
     */
    boolean borrowItem(Student student);

    /**
     * Handles the increment of available copies when returned.
     * @param student The student returning the item.
     */
    void returnItem(Student student);
}