package model;

import java.io.Serializable;

public class Magazine extends LibraryItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public Magazine() {
        super();
    }

    public Magazine(String id, String title, String author, int year) {
        super(id, title, author, year);
    }

    @Override
    public String getType() {
        return "Magazine";
    }
}