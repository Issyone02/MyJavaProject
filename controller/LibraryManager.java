package controller;

import java.util.*;
import java.io.Serializable;
import model.*;
import utils.FileHandler;

/**
 * Core business logic controller for the library system.
 *
 * Owns a LibraryDatabase (composition) which holds all four data structures:
 *   ArrayList (catalogue), Queue (waitlist), Stack (undo/redo), Array (cache).
 *
 * Sorting algorithms :
 *   - Insertion Sort  — O(n²) worst, O(n) best, stable
 *   - Merge Sort      — O(n log n) all cases, stable, recursive
 *   - Quick Sort      — O(n log n) avg, in-place, recursive
 */
public class LibraryManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private LibraryDatabase db;

    // Undo/redo stacks — Stack data structure
    private final Stack<LibraryState> undoHistory = new Stack<>();
    private final Stack<LibraryState> redoHistory = new Stack<>();

    private List<SystemLog> systemLogs = new ArrayList<>();

    // Snapshot of all mutable data, stored on undo/redo stacks
    private static class LibraryState implements Serializable {
        final List<LibraryItem> inv;
        final List<UserAccount> std;
        final Queue<String>     wait;

        LibraryState(List<LibraryItem> i, List<UserAccount> s, Queue<String> w) {
            this.inv  = new ArrayList<>(i);
            this.wait = new LinkedList<>(w);
            this.std  = new ArrayList<>();
            for (UserAccount student : s) this.std.add(new UserAccount(student));
        }
    }

    // Loads all persisted data on startup
    public LibraryManager() {
        db = new LibraryDatabase();
        db.setCatalogue(new ArrayList<>(FileHandler.loadData()));
        db.setMembers(new ArrayList<>(FileHandler.loadStudents()));
        db.setWaitlist(FileHandler.loadWaitlist());
        saveState(false);
    }

    // ── Undo / Redo ──────────────────────────────────────────────────────────

    // Saves a snapshot and writes to disk. Pass true from undo/redo to keep redo stack.
    public void saveState(boolean isUndoOrRedo) {
        undoHistory.push(new LibraryState(db.getCatalogue(), db.getMembers(), db.getWaitlist()));
        if (undoHistory.size() > 50) undoHistory.remove(0);
        if (!isUndoOrRedo) redoHistory.clear();
        FileHandler.saveAll(db.getCatalogue(), db.getMembers(), db.getWaitlist());
    }

    public void undo(String userName) {
        if (!undoHistory.isEmpty()) {
            redoHistory.push(new LibraryState(db.getCatalogue(), db.getMembers(), db.getWaitlist()));
            restoreFromState(undoHistory.pop());
            addLog(userName, "UNDO", "Global state reverted.");
        }
    }

    public void redo(String userName) {
        if (!redoHistory.isEmpty()) {
            undoHistory.push(new LibraryState(db.getCatalogue(), db.getMembers(), db.getWaitlist()));
            restoreFromState(redoHistory.pop());
            addLog(userName, "REDO", "Global state restored.");
        }
    }

    private void restoreFromState(LibraryState state) {
        db.setCatalogue(new ArrayList<>(state.inv));
        db.setMembers(new ArrayList<>(state.std));
        db.setWaitlist(new LinkedList<>(state.wait));
        FileHandler.saveAll(db.getCatalogue(), db.getMembers(), db.getWaitlist());
    }

    // ── Item CRUD ────────────────────────────────────────────────────────────

    public void addItem(LibraryItem item) {
        saveState(false);
        db.addItem(item);
    }

    public boolean removeItem(String id) {
        saveState(false);
        return db.removeItem(id);
    }

    // Updates an item's fields while preserving the number of currently borrowed copies
    public void updateItem(String userId, String id, String type, String title,
                           String author, int year, int total, String reason) {
        saveState(false);
        List<LibraryItem> catalogue = db.getCatalogue();
        for (int i = 0; i < catalogue.size(); i++) {
            if (catalogue.get(i).getId().equals(id)) {
                int borrowed = catalogue.get(i).getTotalCopies()
                             - catalogue.get(i).getAvailableCopies();

                // Polymorphism: creates the right subtype from the type string
                LibraryItem newItem;
                if      (type.equalsIgnoreCase("Book"))     newItem = new Book(id, title, author, year);
                else if (type.equalsIgnoreCase("Magazine")) newItem = new Magazine(id, title, author, year);
                else                                         newItem = new Journal(id, title, author, year);

                newItem.setTotalCopies(total);
                newItem.setAvailableCopies(Math.max(0, total - borrowed));
                catalogue.set(i, newItem);
                addLog(userId, "UPDATE", "Item " + id + " updated: " + reason);
                break;
            }
        }
    }

    //  processes any LibraryItem subtype uniformly
    public String processItem(LibraryItem item) {
        if (item == null) return "Item: null";
        return item.getType() + ": " + item.getTitle() + " (" + item.getId() + ")";
    }

    public LibraryItem getItemById(String id) {
        return db.findItemById(id);
    }

    // ── Borrow / Return ──────────────────────────────────────────────────────

    // Checks for duplicate loans, then uses Borrowable.checkout() to decrement copies
    public boolean borrowItem(String userId, UserAccount s, LibraryItem item) {
        if (s == null || item == null) return false;

        boolean alreadyHasItem = s.getCurrentLoans().stream()
                .anyMatch(r -> r.getItem().getId().equalsIgnoreCase(item.getId().trim()));
        if (alreadyHasItem) {
            addLog(userId, "BORROW_DENIED", s.getName() + " already has " + item.getTitle());
            return false;
        }

        if (!(item instanceof Borrowable borrowable)) return false;

        if (!borrowable.checkout()) {
            addLog(userId, "BORROW_DENIED", "No copies available for " + item.getTitle());
            return false;
        }

        saveState(false);
        s.addBorrowedItem(item);
        db.recordAccess(item);
        addLog(userId, "BORROW", s.getName() + " borrowed " + item.getTitle());
        return true;
    }

    public void returnItem(String userId, UserAccount s, LibraryItem item) {
        if (s == null || item == null) return;
        if (!s.returnItem(item)) return;

        if (item instanceof Borrowable borrowable) borrowable.checkin();

        saveState(false);
        addLog(userId, "RETURN", item.getTitle() + " returned by " + s.getName());
    }

    /** Removes a loan whose item has been deleted from the catalogue (no copy checkin). */
    public void removeOrphanedLoan(String userId, UserAccount s, LibraryItem item) {
        if (s == null || item == null) return;
        if (!s.returnItem(item)) return;
        saveState(false);
        addLog(userId, "REMOVE_ORPHAN_LOAN", item.getTitle() + " (deleted) removed from " + s.getName() + "'s loans");
    }

    // ── Student CRUD ─────────────────────────────────────────────────────────

    public boolean addStudent(UserAccount s) {
        boolean added = db.addMember(s);
        if (added) saveState(false);
        return added;
    }

    public void removeStudent(String id) {
        saveState(false);
        db.removeMember(id);
    }

    public void updateStudent(String id, String newName) {
        saveState(false);
        UserAccount s = db.findMemberById(id);
        if (s != null) s.setName(newName);
    }

    public UserAccount findStudentById(String id) {
        return db.findMemberById(id);
    }

    // ── Waitlist (Queue) ─────────────────────────────────────────────────────

    public void addToWaitlist(String userId, UserAccount s, LibraryItem item) {
        saveState(false);
        db.enqueueWaitlist(s.getName() + " (" + s.getStudentId() + ") -> " + item.getTitle());
        addLog(userId, "WAITLIST", "Added " + s.getName() + " to waitlist for " + item.getTitle());
    }

    public void moveWaitlistEntry(String userId, int index, boolean up) {
        List<String> temp = new ArrayList<>(db.getWaitlist());
        int swapWith = up ? index - 1 : index + 1;
        if (swapWith >= 0 && swapWith < temp.size()) {
            saveState(false);
            Collections.swap(temp, index, swapWith);
            db.setWaitlist(new LinkedList<>(temp));
        }
    }

    public void removeWaitlistEntry(String userId, int index) {
        List<String> temp = new ArrayList<>(db.getWaitlist());
        if (index >= 0 && index < temp.size()) {
            saveState(false);
            temp.remove(index);
            db.setWaitlist(new LinkedList<>(temp));
        }
    }

    // Atomically borrows an item and removes the waitlist entry — single undo step.
    public boolean fulfillWaitlistEntry(String userId, UserAccount student, LibraryItem item, int waitlistIdx) {
        if (student.getCurrentLoans().stream().anyMatch(r -> r.getItem().getId().equalsIgnoreCase(item.getId()))) return false;
        if (!(item instanceof Borrowable b) || !b.checkout()) return false;
        saveState(false);
        student.addBorrowedItem(item);
        db.recordAccess(item);
        List<String> temp = new ArrayList<>(db.getWaitlist());
        temp.remove(waitlistIdx);
        db.setWaitlist(new LinkedList<>(temp));
        addLog(userId, "FULFILL_WAITLIST", student.getName() + " issued " + item.getTitle() + " from waitlist");
        return true;
    }

    public void clearWaitlist(String userId) {
        saveState(false);
        db.getWaitlist().clear();
        addLog(userId, "CLEAR", "Waitlist cleared.");
    }

    // ── Sorting Algorithms ───────────────────────────────────────────────────

    private java.util.Comparator<LibraryItem> comparatorFor(String field) {
        return switch (field) {
            case "ID"        -> Comparator.comparing(i -> i.getId().toLowerCase());
            case "Type"      -> Comparator.comparing(i -> i.getType().toLowerCase());
            case "Author"    -> Comparator.comparing(i -> i.getAuthor().toLowerCase());
            case "Year"      -> Comparator.comparingInt(LibraryItem::getYear);
            case "Available" -> Comparator.comparingInt(LibraryItem::getAvailableCopies);
            case "Total"     -> Comparator.comparingInt(LibraryItem::getTotalCopies);
            default          -> Comparator.comparing(i -> i.getTitle().toLowerCase()); // "Title"
        };
    }

    /**
     * Insertion Sort — O(n²) worst/average, O(n) best case, stable.
     * Builds the sorted section one element at a time. Shifts each element
     * left until it reaches its correct position. Fast on nearly-sorted data.
     */
    public void insertionSortBy(String field) {
        List<LibraryItem> list = db.getCatalogue();
        saveState(false);
        java.util.Comparator<LibraryItem> cmp = comparatorFor(field);
        int n = list.size();
        for (int i = 1; i < n; i++) {
            LibraryItem key = list.get(i);
            int j = i - 1;
            while (j >= 0 && cmp.compare(list.get(j), key) > 0) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    /**
     * Merge Sort — O(n log n) all cases, stable, recursive.
     * Splits the list in half, sorts each half recursively, then merges.
     * Uses temporary arrays during the merge step (O(n) extra space).
     */
    public void mergeSortBy(String field) {
        List<LibraryItem> list = db.getCatalogue();
        if (list.size() < 2) return;
        saveState(false);
        mergeSortGeneric(list, 0, list.size() - 1, comparatorFor(field));
    }

    private void mergeSortGeneric(List<LibraryItem> list, int left, int right,
                                  java.util.Comparator<LibraryItem> cmp) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            mergeSortGeneric(list, left, mid, cmp);       // sort left half
            mergeSortGeneric(list, mid + 1, right, cmp);  // sort right half
            mergeGeneric(list, left, mid, right, cmp);     // merge both halves
        }
    }

    private void mergeGeneric(List<LibraryItem> list, int left, int mid, int right,
                               java.util.Comparator<LibraryItem> cmp) {
        int n1 = mid - left + 1;
        int n2 = right - mid;
        LibraryItem[] L = new LibraryItem[n1];
        LibraryItem[] R = new LibraryItem[n2];
        for (int i = 0; i < n1; i++) L[i] = list.get(left + i);
        for (int j = 0; j < n2; j++) R[j] = list.get(mid + 1 + j);

        int i = 0, j = 0, k = left;
        while (i < n1 && j < n2) {
            if (cmp.compare(L[i], R[j]) <= 0) list.set(k++, L[i++]);
            else                               list.set(k++, R[j++]);
        }
        while (i < n1) list.set(k++, L[i++]);
        while (j < n2) list.set(k++, R[j++]);
    }

    /**
     * Quick Sort — O(n log n) average, O(n²) worst case, recursive.
     * Picks a pivot (median-of-three), partitions elements around it,
     * then recursively sorts both partitions. In-place (O(log n) stack space).
     */
    public void quickSortBy(String field) {
        List<LibraryItem> list = db.getCatalogue();
        if (list.size() < 2) return;
        saveState(false);
        quickSortGeneric(list, 0, list.size() - 1, comparatorFor(field));
    }

    private void quickSortGeneric(List<LibraryItem> list, int low, int high,
                                   java.util.Comparator<LibraryItem> cmp) {
        if (low < high) {
            int pivotIdx = partition(list, low, high, cmp);
            quickSortGeneric(list, low, pivotIdx - 1, cmp);
            quickSortGeneric(list, pivotIdx + 1, high, cmp);
        }
    }

    private int partition(List<LibraryItem> list, int low, int high,
                          java.util.Comparator<LibraryItem> cmp) {
        // Median-of-three: pick the middle value of low/mid/high as pivot
        int mid = low + (high - low) / 2;
        if (cmp.compare(list.get(mid), list.get(low)) < 0)
            Collections.swap(list, low, mid);
        if (cmp.compare(list.get(high), list.get(low)) < 0)
            Collections.swap(list, low, high);
        if (cmp.compare(list.get(mid), list.get(high)) < 0)
            Collections.swap(list, mid, high);
        LibraryItem pivot = list.get(high);

        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (cmp.compare(list.get(j), pivot) <= 0) {
                i++;
                LibraryItem temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
            }
        }
        LibraryItem temp = list.get(i + 1);
        list.set(i + 1, list.get(high));
        list.set(high, temp);
        return i + 1;
    }
    // ── Logging ──────────────────────────────────────────────────────────────

    public void addLog(String userId, String action, String details) {
        systemLogs.add(new SystemLog(userId, action, details));
        FileHandler.logStealthActivity("User: " + userId + " | " + action + " | " + details);
    }

    // ── Getters (used by GUI panels) ─────────────────────────────────────────

    public List<LibraryItem> getInventory()        { return db.getCatalogue(); }
    public List<UserAccount> getStudents()          { return db.getMembers(); }
    public Queue<String>     getWaitlist()          { return db.getWaitlist(); }
    public List<LibraryItem> getMostAccessedItems() { return db.getMostAccessedItems(); }
    public List<SystemLog>   getSystemLogs()        { return systemLogs; }
    public LibraryDatabase   getDatabase()          { return db; }
}
