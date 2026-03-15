package controller;

import model.LibraryItem;
import java.util.*;

/** All catalogue sorting algorithms — Insertion, Merge, and Quick Sort. Delegated to by LibraryManager. */
public class SortEngine {

    // Private constructor — this class is a pure static utility
    private SortEngine() {}

    // ── Field → Comparator ────────────────────────────────────────────────────

    /**
     * Returns a comparator for the given catalogue field name.
     * String fields are compared case-insensitively; numeric fields use natural order.
     * Unknown field names default to Title.
     */
    public static Comparator<LibraryItem> comparatorFor(String field) {
        return switch (field) {
            case "ID"        -> Comparator.comparing((LibraryItem i) -> i.getId().toLowerCase());
            case "Type"      -> Comparator.comparing((LibraryItem i) -> i.getType().toLowerCase());
            case "Author"    -> Comparator.comparing((LibraryItem i) -> i.getAuthor().toLowerCase());
            case "Year"      -> Comparator.comparingInt(LibraryItem::getYear);
            case "Available" -> Comparator.comparingInt(LibraryItem::getAvailableCopies);
            case "Total"     -> Comparator.comparingInt(LibraryItem::getTotalCopies);
            default          -> Comparator.comparing((LibraryItem i) -> i.getTitle().toLowerCase());
        };
    }

    // ── 1. Insertion Sort ─────────────────────────────────────────────────────

    /**
     * Insertion Sort — O(n²) worst/average, O(n) best, stable.
     * Builds the sorted section one element at a time by shifting each key
     * left until it reaches its correct position in the already-sorted region.
     *
     * @param list  the catalogue list to sort in-place
     * @param field field name to sort by (see comparatorFor)
     */
    public static void insertionSort(List<LibraryItem> list, String field) {
        if (list == null || list.size() < 2) return;
        Comparator<LibraryItem> cmp = comparatorFor(field);
        int n = list.size();
        for (int i = 1; i < n; i++) {
            LibraryItem key = list.get(i);
            int j = i - 1;
            // Shift elements greater than key one position to the right
            while (j >= 0 && cmp.compare(list.get(j), key) > 0) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    // ── 2. Merge Sort ─────────────────────────────────────────────────────────

    /**
     * Merge Sort — O(n log n) all cases, stable, recursive.
     * Splits the list in half, sorts each half recursively, then merges.
     *
     * @param list  the catalogue list to sort in-place
     * @param field field name to sort by (see comparatorFor)
     */
    public static void mergeSort(List<LibraryItem> list, String field) {
        if (list == null || list.size() < 2) return;
        mergeSortRecursive(list, 0, list.size() - 1, comparatorFor(field));
    }

    // Recursive step: splits [left, right] at the midpoint, sorts each half, merges
    private static void mergeSortRecursive(List<LibraryItem> list, int left, int right,
                                           Comparator<LibraryItem> cmp) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSortRecursive(list, left, mid, cmp);
            mergeSortRecursive(list, mid + 1, right, cmp);
            merge(list, left, mid, right, cmp);
        }
    }

    // Merge step: copies both sub-arrays into L[] and R[], then writes back in sorted order
    private static void merge(List<LibraryItem> list, int left, int mid, int right,
                               Comparator<LibraryItem> cmp) {
        int n1 = mid - left + 1, n2 = right - mid;
        LibraryItem[] L = new LibraryItem[n1];
        LibraryItem[] R = new LibraryItem[n2];
        for (int i = 0; i < n1; i++) L[i] = list.get(left + i);
        for (int j = 0; j < n2; j++) R[j] = list.get(mid + 1 + j);
        int i = 0, j = 0, k = left;
        while (i < n1 && j < n2)
            list.set(k++, cmp.compare(L[i], R[j]) <= 0 ? L[i++] : R[j++]);
        while (i < n1) list.set(k++, L[i++]);
        while (j < n2) list.set(k++, R[j++]);
    }

    // ── 3. Quick Sort ─────────────────────────────────────────────────────────

    /**
     * Quick Sort — O(n log n) average, O(n²) worst, in-place, recursive.
     * Uses median-of-three pivot selection to avoid worst-case on already-sorted input.
     *
     * @param list  the catalogue list to sort in-place
     * @param field field name to sort by (see comparatorFor)
     */
    public static void quickSort(List<LibraryItem> list, String field) {
        if (list == null || list.size() < 2) return;
        quickSortRecursive(list, 0, list.size() - 1, comparatorFor(field));
    }

    // Recursive step: partitions [low, high] around a pivot, then recurses on both sides
    private static void quickSortRecursive(List<LibraryItem> list, int low, int high,
                                            Comparator<LibraryItem> cmp) {
        if (low < high) {
            int p = partition(list, low, high, cmp);
            quickSortRecursive(list, low, p - 1, cmp);
            quickSortRecursive(list, p + 1, high, cmp);
        }
    }

    // Partition: median-of-three pivot, moves all elements ≤ pivot left of the pivot's final index
    private static int partition(List<LibraryItem> list, int low, int high,
                                  Comparator<LibraryItem> cmp) {
        int mid = low + (high - low) / 2;
        if (cmp.compare(list.get(mid),  list.get(low))  < 0) Collections.swap(list, low, mid);
        if (cmp.compare(list.get(high), list.get(low))  < 0) Collections.swap(list, low, high);
        if (cmp.compare(list.get(mid),  list.get(high)) < 0) Collections.swap(list, mid, high);
        LibraryItem pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++)
            if (cmp.compare(list.get(j), pivot) <= 0)
                Collections.swap(list, ++i, j);
        Collections.swap(list, i + 1, high);
        return i + 1;
    }
}
