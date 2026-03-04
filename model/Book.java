package model;

public class Book extends LibraryItem {
    public Book() {}
    public Book(String id, String title, String author, int year) {
        super(id, title, author, year);
    }
    @Override
    public String getType() { return "Book"; }
}