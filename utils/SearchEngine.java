package utils;

import model.LibraryItem;
import java.util.List;

/**
 * SearchEngine handles recursive searching through the library inventory.
 * Supports partial matching (case-insensitive) to make the search "smart."
 */
public class SearchEngine {

    /**
     * Recursive search method: A "Divide and Conquer" style approach to finding data.
     * This fulfills the academic requirement for recursive algorithms.
     * * @param items    The master list of library items to scan.
     * @param query    The text the librarian typed into the search bar.
     * @param criteria The category being searched ("Title", "Author", or "Type").
     * @param index    The current "pointer" or position in the list.
     * @return The first LibraryItem that matches the search, or null if nothing is found.
     */
    public static LibraryItem recursiveSearch(List<LibraryItem> items, String query, String criteria, int index) {


        if (index >= items.size()) {
            return null; // Return null to indicate "Not Found"
        }



        LibraryItem current = items.get(index);
        String targetValue = "";


        switch (criteria) {
            case "Title":
                targetValue = current.getTitle();
                break;
            case "Author":
                targetValue = current.getAuthor();
                break;
            case "Type":
                targetValue = current.getType();
                break;
            default:
                targetValue = current.getTitle(); // Default to Title if input is weird
        }


        if (targetValue != null && query != null) {
            String lowerTarget = targetValue.toLowerCase();
            String lowerQuery = query.toLowerCase();


            if (lowerTarget.contains(lowerQuery)) {
                return current;
            }
        }

        // --- RECURSIVE STEP ---

        return recursiveSearch(items, query, criteria, index + 1);
    }
}