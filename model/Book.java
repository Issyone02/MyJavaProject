package model;

import java.io.Serializable;

public class Book extends LibraryItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public Book() {
        super();
    }

    public Book(String id, String title, String author, int year) {
        super(id, title, author, year);
    }

    @Override
    public String getType() {
        return "Book";
    }
}