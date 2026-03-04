package model;
/**
 * Book is a special type of LibraryItem, so it inherits all the
 * common fields and behaviours from the parent class
 */
public class Book extends LibraryItem {
    /**
     * Empty constructor required for deserialisation
     * when loading saved data
     */
    public Book() {}
    public Book(String id, String title, String author, int year) {
        /**
         * Pass all the details up to the parent class, as such
         * there is no need to redefine them here
         */
        super(id, title, author, year);
    }
    // Tells the system this item is specifically a Book
    @Override
    public String getType() { return "Book"; }
}