package controller;

import model.LibraryItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Catalogue search — Linear Search (unsorted) and Binary Search (sorted). Auto-selects based on sort state. */
public class SearchEngine {

    /** Holds search results and the name of the algorithm that was used. */
    public static class SearchResult {
        public final List<LibraryItem> matches;
        public final String algorithm;

        public SearchResult(List<LibraryItem> matches, String algorithm) {
            this.matches   = matches;
            this.algorithm = algorithm;
        }
    }

    /**
     * Searches the catalogue and returns all matches.
     * Automatically picks Binary Search if the catalogue is sorted, otherwise Linear Search.
     */
    public static SearchResult searchAll(List<LibraryItem> items, String query) {
        if (items == null || query == null || query.trim().isEmpty())
            return new SearchResult(new ArrayList<>(), "—");

        String sortedField = detectSortedField(items);

        if (sortedField != null) {
            List<LibraryItem> matches = binarySearchAll(items, query, sortedField);
            return new SearchResult(matches, "Binary Search (sorted by " + sortedField + ")");
        }

        List<LibraryItem> matches = linearSearchAll(items, query);
        return new SearchResult(matches, "Linear Search (unsorted)");
    }

    // ── Linear Search — O(n) ─────────────────────────────────────────────────
    // Checks every item's Title, Author, ID, Type, and Year for a partial match

    private static List<LibraryItem> linearSearchAll(List<LibraryItem> items, String query) {
        List<LibraryItem> results = new ArrayList<>();
        if (items == null || query == null) return results;
        String q = query.trim().toLowerCase();
        for (LibraryItem item : items) {
            if (item != null && matches(item, q)) results.add(item);
        }
        return results;
    }

    // ── Binary Search — O(log n) + expand ────────────────────────────────────
    // Finds one match via binary search on the sorted field, then expands left
    // and right to collect all adjacent matches. Other fields are checked linearly.

    private static List<LibraryItem> binarySearchAll(List<LibraryItem> items, String query,
                                                      String sortedField) {
        if (items == null || query == null) return new ArrayList<>();
        String q = query.trim().toLowerCase();
        Set<String> seenIds = new HashSet<>();
        List<LibraryItem> results = new ArrayList<>();

        int foundIdx = binaryFindAny(items, q, sortedField);

        if (foundIdx >= 0) {
            // Expand left from foundIdx
            for (int i = foundIdx; i >= 0; i--) {
                String val = getFieldValue(items.get(i), sortedField);
                if (val == null || !val.toLowerCase().contains(q)) break;
                if (seenIds.add(items.get(i).getId())) results.add(items.get(i));
            }
            // Expand right from foundIdx+1
            for (int i = foundIdx + 1; i < items.size(); i++) {
                String val = getFieldValue(items.get(i), sortedField);
                if (val == null || !val.toLowerCase().contains(q)) break;
                if (seenIds.add(items.get(i).getId())) results.add(items.get(i));
            }
        }

        // Linear scan for matches on other fields not yet collected
        for (LibraryItem item : items) {
            if (item != null && !seenIds.contains(item.getId()) && matches(item, q))
                results.add(item);
        }

        return results;
    }

    // Standard binary search — returns the index of any item where sortedField contains query
    private static int binaryFindAny(List<LibraryItem> items, String query, String field) {
        int low = 0, high = items.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            String midVal = getFieldValue(items.get(mid), field);
            if (midVal == null) midVal = "";
            midVal = midVal.toLowerCase();
            if (midVal.contains(query)) return mid;
            if (midVal.compareTo(query) < 0) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    // ── Sort detection (public — used by SearchSortPanel to show hint label) ─

    /** Returns the field the list is currently sorted by, or null if unsorted. */
    public static String detectSortedField(List<LibraryItem> items) {
        if (isSortedByString(items, "ID"))        return "ID";
        if (isSortedByString(items, "Title"))     return "Title";
        if (isSortedByString(items, "Author"))    return "Author";
        if (isSortedByString(items, "Type"))      return "Type";
        if (isSortedByNumeric(items, "Year"))      return "Year";
        if (isSortedByNumeric(items, "Available")) return "Available";
        if (isSortedByNumeric(items, "Total"))     return "Total";
        return null;
    }

    // Checks ascending sort by a string field (case-insensitive)
    private static boolean isSortedByString(List<LibraryItem> items, String field) {
        if (items == null || items.size() < 2) return true;
        for (int i = 1; i < items.size(); i++) {
            String prev = getFieldValue(items.get(i - 1), field);
            String curr = getFieldValue(items.get(i), field);
            if (prev != null && curr != null &&
                    prev.toLowerCase().compareTo(curr.toLowerCase()) > 0) return false;
        }
        return true;
    }

    // Checks ascending sort by a numeric field
    private static boolean isSortedByNumeric(List<LibraryItem> items, String field) {
        if (items == null || items.size() < 2) return true;
        for (int i = 1; i < items.size(); i++) {
            if (getNumericFieldValue(items.get(i - 1), field)
                    > getNumericFieldValue(items.get(i), field)) return false;
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Checks if the given LibraryItem matches the search query across all searchable fields.
     * 
     * @param item the LibraryItem to check
     * @param lowerQuery the search query in lowercase
     * @return true if the item matches the query, false otherwise
     */
    private static boolean matches(LibraryItem item, String lowerQuery) {
        return contains(item.getId(),    lowerQuery)
            || contains(item.getTitle(), lowerQuery)
            || contains(item.getAuthor(),lowerQuery)
            || contains(item.getType(), lowerQuery)
            || String.valueOf(item.getYear()).contains(lowerQuery);
    }

    /**
     * Performs a case-insensitive containment check with null safety.
     * 
     * @param value the string to check
     * @param query the search query
     * @return true if the value contains the query, false otherwise
     */
    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    /** Returns field value as string based on field name. Used for text searches. */
    private static String getFieldValue(LibraryItem item, String field) {
        if (item == null) return null;
        return switch (field) {
            case "ID"        -> item.getId();
            case "Author"    -> item.getAuthor();
            case "Type"      -> item.getType();
            case "Year"      -> String.valueOf(item.getYear());
            case "Available" -> String.valueOf(item.getAvailableCopies());
            case "Total"     -> String.valueOf(item.getTotalCopies());
            default          -> item.getTitle();
        };
    }

    /** Returns field value as integer for numeric comparisons. Used for sorting. */
    private static int getNumericFieldValue(LibraryItem item, String field) {
        if (item == null) return 0;
        return switch (field) {
            case "Year"      -> item.getYear();
            case "Available" -> item.getAvailableCopies();
            case "Total"     -> item.getTotalCopies();
            default          -> 0;
        };
    }
}
