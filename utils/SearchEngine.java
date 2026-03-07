package utils;

import model.LibraryItem;
import java.util.List;

/**
 * SearchEngine - Provides recursive search through library inventory
 *
 * Implements recursive search algorithm for finding items by:
 * - Title (partial match)
 * - Author (partial match)
 * - Type (exact match)
 *
 * Uses divide-and-conquer approach to fulfill academic algorithm requirements.
 */
public class SearchEngine {

    /**
     * Recursive search method - Searches library items one by one
     *
     * Algorithm: Linear recursive search
     * - Base case: Reached end of list (return null)
     * - Recursive case: Check current item, then search rest of list
     *
     * @param items    List of library items to search
     * @param query    Search text (case-insensitive)
     * @param criteria Field to search ("Title", "Author", or "Type")
     * @param index    Current position in list (start with 0)
     * @return First matching item, or null if not found
     */
    public static LibraryItem recursiveSearch(List<LibraryItem> items, String query, String criteria, int index) {
        // Base case: Reached end of list
        if (index >= items.size()) {
            return null;  // Not found
        }

        // Get current item
        LibraryItem current = items.get(index);
        String targetValue = "";

        // Extract the field to search based on criteria
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
                targetValue = current.getTitle();  // Default to title
        }

        // Check if current item matches query (case-insensitive partial match)
        if (targetValue != null && query != null) {
            String lowerTarget = targetValue.toLowerCase();
            String lowerQuery = query.toLowerCase();

            if (lowerTarget.contains(lowerQuery)) {
                return current;  // Found it!
            }
        }

        // Recursive step: Search the rest of the list
        return recursiveSearch(items, query, criteria, index + 1);
    }
}