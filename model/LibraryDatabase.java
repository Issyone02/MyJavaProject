package model;

import java.io.Serializable;
import java.util.*;

/** Central data store: ArrayList catalogue, ArrayList members, Queue waitlist, LibraryItem[] frequency cache. */
public class LibraryDatabase implements Serializable {
    private static final long serialVersionUID = 1L;

    private ArrayList<LibraryItem> catalogue;
    private ArrayList<UserAccount> members;
    private Queue<String> waitlist;
    private LibraryItem[] accessCache;
    private static final int CACHE_SIZE = 10;

    /** Creates empty database with initialized collections and cache. */
    public LibraryDatabase() {
        this.catalogue   = new ArrayList<>();
        this.members     = new ArrayList<>();
        this.waitlist    = new LinkedList<>();
        this.accessCache = new LibraryItem[CACHE_SIZE];
    }

    // ── Catalogue operations ─────────────────────────────────────────────────

    /** Adds an item to the catalogue if not null. */
    public void addItem(LibraryItem item) {
        if (item != null) catalogue.add(item);
    }

    /** Removes item by ID (case-insensitive). Returns true if item was found and removed. */
    public boolean removeItem(String id) {
        return catalogue.removeIf(i -> i != null && i.getId().equalsIgnoreCase(id));
    }

    /** Finds an item by ID and records it in the access cache for frequency tracking. */
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

    /**
     * Sets the catalogue of items.
     * If the provided catalogue is null, an empty catalogue is used instead.
     * @param catalogue the new catalogue
     */
    public void setCatalogue(ArrayList<LibraryItem> catalogue) {
        this.catalogue = (catalogue != null) ? catalogue : new ArrayList<>();
    }

    // ── Member (student) operations ──────────────────────────────────────────

    /**
     * Adds a member to the database if they do not already exist.
     * Returns false if a member with the same ID already exists.
     * @param student the member to add
     * @return true if the member was added, false otherwise
     */
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

    /** Adds entry to waitlist queue if not null or blank. */
    public void enqueueWaitlist(String entry) {
        if (entry != null && !entry.isBlank()) waitlist.offer(entry);
    }

    public Queue<String> getWaitlist() { return waitlist; }

    public void setWaitlist(Queue<String> waitlist) {
        this.waitlist = (waitlist != null) ? waitlist : new LinkedList<>();
    }

    /** Access cache (fixed-size Array) - tracks most frequently accessed items. */
    private int[] accessCount = new int[CACHE_SIZE];


    /** Records one access for the given item in the frequency cache.
     *
     * If the item is already in the cache, its count is incremented and
     * array is re-sorted so the most-accessed item stays at index 0.
     *
     * If the item is not yet cached:
     *   - While the cache has empty slots, the item is placed in the first empty slot.
     *   - Once the cache is full, the new item (count = 1) always displaces the item
     *     in the last slot — which, after sorting, is the current least-frequently
     *     accessed item. This is the standard frequency-based eviction policy.
     */
    public void recordAccess(LibraryItem item) {
        if (item == null) return;

        // Check if item is already cached — if so, increment its count
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (accessCache[i] != null && accessCache[i].getId().equals(item.getId())) {
                accessCache[i] = item;  // keep reference fresh
                accessCount[i]++;
                sortCacheDescending();
                return;
            }
        }

        // Item not cached — find the first empty slot
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (accessCache[i] == null) {
                accessCache[i] = item;
                accessCount[i] = 1;
                sortCacheDescending();
                return;
            }
        }

        // Cache is full — replace the slot with the lowest count (last after sort)
        int lastIdx = CACHE_SIZE - 1;
        accessCache[lastIdx] = item;
        accessCount[lastIdx] = 1;
        sortCacheDescending();
    }

    /** Sorts accessCache[] and accessCount[] together by count, descending.
     * Uses insertion sort — stable, O(n), appropriate for this fixed size of 10.
     * Both arrays are swapped in tandem so each item stays paired with its count.
     */
    private void sortCacheDescending() {
        for (int i = 1; i < CACHE_SIZE; i++) {
            LibraryItem keyItem  = accessCache[i];
            int         keyCount = accessCount[i];
            int j = i - 1;
            while (j >= 0 && accessCount[j] < keyCount) {
                accessCache[j + 1]  = accessCache[j];
                accessCount[j + 1]  = accessCount[j];
                j--;
            }
            accessCache[j + 1]  = keyItem;
            accessCount[j + 1]  = keyCount;
        }
    }

    /** Returns the cached items in descending frequency order, nulls excluded.
     * Index 0 is the most frequently accessed item.
     */
    public List<LibraryItem> getMostAccessedItems() {
        List<LibraryItem> result = new ArrayList<>();
        for (LibraryItem item : accessCache) {
            if (item != null) result.add(item);
        }
        return result;
    }

    /** Returns the access count for the item at the given cache index.
     * Used by ViewPanel to display hit counts alongside item titles.
     */
    public int getAccessCount(int cacheIndex) {
        if (cacheIndex < 0 || cacheIndex >= CACHE_SIZE) return 0;
        return accessCount[cacheIndex];
    }

}
