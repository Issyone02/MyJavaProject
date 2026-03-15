package model;

/** A Journal in the library catalogue. */
public class Journal extends LibraryItem {
    private static final long serialVersionUID = 1L;

    public Journal() { super(); }

    public Journal(String id, String title, String author, int year) {
        super(id, title, author, year);
    }

    @Override
    public String getType() { return "Journal"; }
}
