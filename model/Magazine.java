package model;

public class Magazine extends LibraryItem {
    public Magazine() {}
    public Magazine(String id, String title, String author, int year) {
        super(id, title, author, year);
    }
    @Override
    public String getType() { return "Magazine"; }
}