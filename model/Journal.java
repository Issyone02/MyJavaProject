package model;

public class Journal extends LibraryItem {
    public Journal() {}
    public Journal(String id, String title, String author, int year) {
        super(id, title, author, year);
    }
    @Override
    public String getType() { return "Journal"; }
}