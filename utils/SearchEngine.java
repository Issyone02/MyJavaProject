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

        // --- BASE CASE 1: The "Stop" Condition ---
        // If the index has reached the size of the list, we've checked everything.
        if (index >= items.size()) {
            return null; // Return null to indicate "Not Found"
        }

        // --- PROCESSING STEP ---
        // Grab the item at the current position
        LibraryItem current = items.get(index);
        String targetValue = "";

        // Use a Switch statement to pull the correct data based on user choice
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

        // --- BASE CASE 2: The "Success" Condition ---
        // Perform a case-insensitive "contains" check for partial matches
        if (targetValue != null && query != null) {
            String lowerTarget = targetValue.toLowerCase();
            String lowerQuery = query.toLowerCase();

            // If the title/author contains the search text, we've found our winner!
            if (lowerTarget.contains(lowerQuery)) {
                return current;
            }
        }

        // --- RECURSIVE STEP ---
        // If we haven't found it yet and haven't hit the end, call this exact
        // function again, but increment the index by 1 to check the next item.
        return recursiveSearch(items, query, criteria, index + 1);
    }
}