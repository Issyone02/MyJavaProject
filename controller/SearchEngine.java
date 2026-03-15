package controller;

import model.LibraryItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Library catalogue search — two student-implemented algorithms:
 *   1. Linear Search  — O(n), scans every item sequentially
 *   2. Binary Search  — O(log n), used when the list is sorted by a known field
 *
 * The system automatically detects whether the catalogue is sorted and by which
 * field, then selects the appropriate algorithm.
 */
public class SearchEngine {

    // Holds search results + which algorithm was used
    public static class SearchResult {
        public final List<LibraryItem> matches;
        public final String algorithm;

        public SearchResult(List<LibraryItem> matches, String algorithm) {
            this.matches   = matches;
            this.algorithm = algorithm;
        }
    }

    // Searches the catalogue and returns all matching items + algorithm name.
    // Automatically picks Binary Search if sorted, Linear Search if not.
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
    // Checks every item's Title, Author, and Type for a partial match.

    public static List<LibraryItem> linearSearchAll(List<LibraryItem> items, String query) {
        List<LibraryItem> results = new ArrayList<>();
        if (items == null || query == null) return results;
        String q = query.trim().toLowerCase();

        for (LibraryItem item : items) {
            if (item == null) continue;
            if (matches(item, q)) results.add(item);
        }
        return results;
    }

    // ── Binary Search — O(log n) + expand ────────────────────────────────────
    // Finds one match via binary search on the sorted field, then expands
    // left and right to collect all adjacent matches.
    // Also checks Title/Author/Type on each candidate so partial matches work.

    public static List<LibraryItem> binarySearchAll(List<LibraryItem> items, String query, String sortedField) {
        if (items == null || query == null) return new ArrayList<>();
        String q = query.trim().toLowerCase();
        Set<String> seenIds = new HashSet<>();
        List<LibraryItem> results = new ArrayList<>();

        // Binary search to find any item where the sorted field contains the query
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

        // Also check other fields via linear scan for items not yet found
        for (LibraryItem item : items) {
            if (item != null && !seenIds.contains(item.getId()) && matches(item, q)) {
                results.add(item);
            }
        }

        return results;
    }

    // Standard binary search — returns index of any item where sortedField contains query
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

    // ── Sort detection ───────────────────────────────────────────────────────

    // Returns the field name the list is sorted by, or null if unsorted
    public static String detectSortedField(List<LibraryItem> items) {
        if (isSortedBy(items, "ID"))       return "ID";
        if (isSortedBy(items, "Title"))    return "Title";
        if (isSortedBy(items, "Author"))   return "Author";
        if (isSortedBy(items, "Type"))     return "Type";
        if (isSortedByNumeric(items, "Year"))     return "Year";
        if (isSortedByNumeric(items, "Available")) return "Available";
        if (isSortedByNumeric(items, "Total"))    return "Total";
        return null;
    }

    // Checks if the list is sorted ascending by the given field (string comparison)
    public static boolean isSortedBy(List<LibraryItem> items, String field) {
        if (items == null || items.size() < 2) return true;
        for (int i = 1; i < items.size(); i++) {
            String prev = getFieldValue(items.get(i - 1), field);
            String curr = getFieldValue(items.get(i), field);
            if (prev != null && curr != null) {
                if (prev.toLowerCase().compareTo(curr.toLowerCase()) > 0) return false;
            }
        }
        return true;
    }

    // Checks if the list is sorted ascending by the given numeric field
    public static boolean isSortedByNumeric(List<LibraryItem> items, String field) {
        if (items == null || items.size() < 2) return true;
        for (int i = 1; i < items.size(); i++) {
            int prev = getNumericFieldValue(items.get(i - 1), field);
            int curr = getNumericFieldValue(items.get(i), field);
            if (prev > curr) return false;
        }
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    // Returns true if any field (ID, Title, Author, Type, Year) contains the query
    private static boolean matches(LibraryItem item, String lowerQuery) {
        if (contains(item.getId(), lowerQuery))                       return true;
        if (contains(item.getTitle(), lowerQuery))                    return true;
        if (contains(item.getAuthor(), lowerQuery))                   return true;
        if (contains(item.getType(), lowerQuery))                     return true;
        if (String.valueOf(item.getYear()).contains(lowerQuery))      return true;
        return false;
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private static String getFieldValue(LibraryItem item, String field) {
        if (item == null) return null;
        return switch (field) {
            case "ID"     -> item.getId();
            case "Author" -> item.getAuthor();
            case "Type"   -> item.getType();
            case "Year"   -> String.valueOf(item.getYear());
            case "Available" -> String.valueOf(item.getAvailableCopies());
            case "Total"  -> String.valueOf(item.getTotalCopies());
            default       -> item.getTitle();
        };
    }

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
