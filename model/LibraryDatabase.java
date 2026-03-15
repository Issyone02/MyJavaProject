package model;

import java.io.Serializable;
import java.util.*;

/**
 * Central data store for the library system (composition).
 * Holds all four required data structures:
 *   - ArrayList<LibraryItem>  catalogue   (item inventory)
 *   - ArrayList<Student>      members     (student records)
 *   - Queue<String>           waitlist    (FIFO reservation queue)
 *   - LibraryItem[]           accessCache (fixed-size most-accessed cache)
 *
 * No business logic lives here — LibraryManager orchestrates operations.
 */
public class LibraryDatabase implements Serializable {
    private static final long serialVersionUID = 1L;

    private ArrayList<LibraryItem> catalogue;
    private ArrayList<UserAccount> members;
    private Queue<String> waitlist;
    private LibraryItem[] accessCache;
    private static final int CACHE_SIZE = 10;

    public LibraryDatabase() {
        this.catalogue   = new ArrayList<>();
        this.members     = new ArrayList<>();
        this.waitlist    = new LinkedList<>();
        this.accessCache = new LibraryItem[CACHE_SIZE];
    }

    // ── Catalogue operations ─────────────────────────────────────────────────

    public void addItem(LibraryItem item) {
        if (item != null) catalogue.add(item);
    }

    public boolean removeItem(String id) {
        return catalogue.removeIf(i -> i != null && i.getId().equalsIgnoreCase(id));
    }

    // Finds an item by ID and records it in the access cache
    public LibraryItem findItemById(String id) {
        if (id == null) return null;
        String trimmed = id.trim();
        for (LibraryItem item : catalogue) {
            if (item != null && trimmed.equalsIgnoreCase(item.getId())) {
                recordAccess(item);
                return item;
            }
        }
        return null;
    }

    public ArrayList<LibraryItem> getCatalogue() { return catalogue; }

    public void setCatalogue(ArrayList<LibraryItem> catalogue) {
        this.catalogue = (catalogue != null) ? catalogue : new ArrayList<>();
    }

    // ── Student/Member operations ────────────────────────────────────────────

    // Returns false if a student with the same ID already exists
    public boolean addMember(UserAccount student) {
        if (student == null) return false;
        boolean exists = members.stream()
                .anyMatch(s -> s.getStudentId().equalsIgnoreCase(student.getStudentId().trim()));
        if (exists) return false;
        members.add(student);
        return true;
    }

    public void removeMember(String studentId) {
        members.removeIf(s -> s.getStudentId().equals(studentId));
    }

    public UserAccount findMemberById(String id) {
        if (id == null) return null;
        return members.stream()
                .filter(s -> s.getStudentId().equalsIgnoreCase(id.trim()))
                .findFirst()
                .orElse(null);
    }

    public ArrayList<UserAccount> getMembers() { return members; }

    public void setMembers(ArrayList<UserAccount> members) {
        this.members = (members != null) ? members : new ArrayList<>();
    }

    // ── Waitlist (Queue) operations ──────────────────────────────────────────

    public void enqueueWaitlist(String entry) {
        if (entry != null && !entry.isBlank()) waitlist.offer(entry);
    }

    public String dequeueWaitlist() { return waitlist.poll(); }
    public String peekWaitlist()    { return waitlist.peek(); }
    public Queue<String> getWaitlist() { return waitlist; }

    public void setWaitlist(Queue<String> waitlist) {
        this.waitlist = (waitlist != null) ? waitlist : new LinkedList<>();
    }

    // ── Access cache (fixed-size Array) ──────────────────────────────────────

    // Puts the item at position [0]; shifts existing entries right.
    // Once full, the oldest entry drops off the end.
    public void recordAccess(LibraryItem item) {
        if (item == null) return;

        // If already in cache, move it to front
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (accessCache[i] != null && accessCache[i].getId().equals(item.getId())) {
                System.arraycopy(accessCache, 0, accessCache, 1, i);
                accessCache[0] = item;
                return;
            }
        }

        // Not in cache — shift everything right and insert at front
        System.arraycopy(accessCache, 0, accessCache, 1, CACHE_SIZE - 1);
        accessCache[0] = item;
    }

    // Returns cached items with nulls filtered out
    public List<LibraryItem> getMostAccessedItems() {
        List<LibraryItem> result = new ArrayList<>();
        for (LibraryItem item : accessCache) {
            if (item != null) result.add(item);
        }
        return result;
    }

    public LibraryItem[] getAccessCache() { return accessCache; }
    public int getCacheSize() { return CACHE_SIZE; }
}
