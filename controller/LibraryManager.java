package controller;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import model.*;
import utils.AuthManager;
import utils.FileHandler;
import utils.IDGenerator;

/** MVC Controller — business logic, persistence, and observer notification. Implements LibraryController. */
public class LibraryManager implements LibraryController, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    // ── Fields ────────────────────────────────────────────────────────────────

    private LibraryDatabase db;
    private final transient List<LibraryChangeListener> changeListeners = new ArrayList<>();
    private final Stack<LibraryState> undoHistory = new Stack<>();
    private final Stack<LibraryState> redoHistory = new Stack<>();
    private List<SystemLog> systemLogs = new ArrayList<>();

    // Snapshot for undo/redo
    private static class LibraryState implements Serializable {
        private static final long serialVersionUID = 1L;
        final List<LibraryItem> inv;
        final List<UserAccount> std;
        final Queue<String>     wait;
        LibraryState(List<LibraryItem> i, List<UserAccount> s, Queue<String> w) {
            this.inv  = new ArrayList<>(i);
            this.wait = new LinkedList<>(w);
            this.std  = new ArrayList<>();
            for (UserAccount u : s) this.std.add(new UserAccount(u));
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public LibraryManager() {
        db = new LibraryDatabase();
        db.setCatalogue(new ArrayList<>(FileHandler.loadData()));
        db.setMembers(new ArrayList<>(FileHandler.loadStudents()));
        db.setWaitlist(FileHandler.loadWaitlist());
        saveState(false);
    }

    // ── 1. Observer ───────────────────────────────────────────────────────────

    @Override
    public void addChangeListener(LibraryChangeListener l) {
        if (l != null && !changeListeners.contains(l)) changeListeners.add(l);
    }

    @Override
    public void removeChangeListener(LibraryChangeListener l) { changeListeners.remove(l); }

    private void fireChange() {
        for (LibraryChangeListener l : new ArrayList<>(changeListeners)) l.onLibraryDataChanged();
    }

    // ── 2. Item CRUD ──────────────────────────────────────────────────────────

    @Override
    public String createItem(String type, String title, String author,
                              int year, int total, String userId) {
        String id = IDGenerator.generateID();
        LibraryItem item = buildItem(type, id, title, author, year);
        item.setTotalCopies(total);
        item.setAvailableCopies(total);
        addItem(item, userId);
        return id;
    }

    @Override
    public void addItem(LibraryItem item, String userId) {
        saveState(false);
        db.addItem(item);
        addLog(userId, "ADD", item.getId() + " - " + item.getTitle());
        fireChange();
    }

    @Override
    public boolean removeItem(String id, String userId, String reason) {
        if (db.findItemById(id) == null) return false;
        saveState(false);
        db.removeItem(id);
        addLog(userId, "DELETE", id + " - " + reason);
        fireChange();
        return true;
    }

    /**
     * Replaces an item's fields while keeping the borrowed-copy count intact.
     * If 2 of 5 copies are out and the admin raises total to 7, available becomes 5 (7−2).
     */
    @Override
    public void updateItem(String userId, String id, String type, String title,
                           String author, int year, int total, String reason) {
        List<LibraryItem> catalogue = db.getCatalogue();
        for (int i = 0; i < catalogue.size(); i++) {
            LibraryItem existing = catalogue.get(i);
            if (existing.getId().equals(id)) {
                int borrowed = existing.getTotalCopies() - existing.getAvailableCopies();
                LibraryItem updated = buildItem(type, id, title, author, year);
                updated.setTotalCopies(total);
                updated.setAvailableCopies(Math.max(0, total - borrowed));
                saveState(false);
                catalogue.set(i, updated);
                addLog(userId, "UPDATE", "Item " + id + " updated: " + reason);
                fireChange();
                return;
            }
        }
        addLog(userId, "UPDATE_FAILED", "Item " + id + " not found");
    }

    @Override public LibraryItem getItemById(String id) { return db.findItemById(id); }

    // ── 3. Import / export ────────────────────────────────────────────────────

    @Override
    public boolean importFromBackup(String userId, File source) {
        Object[] data = FileHandler.importBackup(source);
        if (data == null) return false;
        @SuppressWarnings("unchecked") List<LibraryItem> items    = (List<LibraryItem>) data[0];
        @SuppressWarnings("unchecked") List<UserAccount> students = (List<UserAccount>) data[1];
        saveState(false);
        db.setCatalogue(new ArrayList<>(items));
        db.setMembers(new ArrayList<>(students));
        FileHandler.saveAll(db.getCatalogue(), db.getMembers(), db.getWaitlist());
        addLog(userId, "IMPORT", items.size() + " items and " + students.size() + " students imported.");
        fireChange();
        return true;
    }

    @Override
    public boolean exportToText(String userId, File destination) {
        boolean ok = FileHandler.exportToText(db.getCatalogue(), db.getMembers(), destination);
        if (ok) addLog(userId, "EXPORT_TEXT", destination.getName());
        return ok;
    }

    @Override
    public boolean exportBinary(String userId, File destination) {
        boolean ok = FileHandler.exportBackup(db.getCatalogue(), db.getMembers(), destination);
        if (ok) addLog(userId, "EXPORT_BINARY", destination.getName());
        return ok;
    }

    // ── 4. Session log ────────────────────────────────────────────────────────

    @Override
    public void logSession(String event, String details) {
        FileHandler.logStealthActivity(event + " | " + details);
    }

    // ── 5. Borrow / Return ────────────────────────────────────────────────────

    // saveState is called only after all validation passes — no orphan snapshots
    /** Processes borrow request with validation and state management. */
    @Override
    public boolean borrowItem(String userId, UserAccount s, LibraryItem item) {
        if (s == null || item == null) return false;
        boolean alreadyHas = s.getCurrentLoans().stream()
                .anyMatch(r -> r.getItem().getId().equalsIgnoreCase(item.getId().trim()));
        if (alreadyHas) { addLog(userId, "BORROW_DENIED", s.getName() + " already has " + item.getTitle()); return false; }
        if (!(item instanceof Borrowable)) return false;
        Borrowable b = (Borrowable) item;
        if (!b.checkout()) { addLog(userId, "BORROW_DENIED", "No copies for " + item.getTitle()); return false; }
        saveState(false);
        s.addBorrowedItem(item);
        db.recordAccess(item);
        addLog(userId, "BORROW", s.getName() + " borrowed " + item.getTitle());
        fireChange();
        return true;
    }

    /** Processes return request with validation and state management. */
    @Override
    public void returnItem(String userId, UserAccount s, LibraryItem item) {
        if (s == null || item == null) return;
        if (!s.returnItem(item)) return;
        if (item instanceof Borrowable) {
            Borrowable b = (Borrowable) item;
            b.checkin();
        }
        saveState(false);
        addLog(userId, "RETURN", item.getTitle() + " returned by " + s.getName());
        fireChange();
    }

    // Used when the catalogue item was deleted — no checkin(), no copy count to restore
    @Override
    public void removeOrphanedLoan(String userId, UserAccount s, LibraryItem item) {
        if (s == null || item == null) return;
        if (!s.returnItem(item)) return;
        saveState(false);
        addLog(userId, "REMOVE_ORPHAN_LOAN", item.getTitle() + " (deleted) removed from " + s.getName() + "'s loans");
        fireChange();
    }

    @Override
    public LibraryItem findLoanItemByTitle(String studentId, String itemTitle) {
        UserAccount s = db.findMemberById(studentId);
        if (s == null || itemTitle == null) return null;
        return s.getCurrentLoans().stream()
                .filter(r -> itemTitle.equals(r.getItem().getTitle()))
                .map(BorrowRecord::getItem).findFirst().orElse(null);
    }

    // ── 6. Student CRUD ───────────────────────────────────────────────────────

    @Override
    public boolean createStudent(String id, String name, String userId) {
        return addStudent(new UserAccount(id, name), userId);
    }

    private boolean addStudent(UserAccount s, String userId) {
        boolean added = db.addMember(s);
        if (added) { saveState(false); addLog(userId, "ADD_STUDENT", s.getName()); fireChange(); }
        return added;
    }

    @Override
    public void removeStudent(String id, String userId) {
        if (db.findMemberById(id) == null) return;
        saveState(false);
        db.removeMember(id);
        addLog(userId, "REMOVE_STUDENT", "ID: " + id);
        fireChange();
    }

    @Override
    public void updateStudent(String id, String newName, String userId) {
        UserAccount s = db.findMemberById(id);
        if (s == null) return;
        saveState(false);
        s.setName(newName);
        addLog(userId, "UPDATE_STUDENT", "ID: " + id + " renamed to " + newName);
        fireChange();
    }

    @Override public UserAccount findStudentById(String id) { return db.findMemberById(id); }

    // ── 7. Waitlist (Queue) ───────────────────────────────────────────────────

    @Override
    public void addToWaitlist(String userId, UserAccount s, LibraryItem item) {
        saveState(false);
        db.enqueueWaitlist(new WaitlistEntry(s.getName(), s.getStudentId(), item.getTitle()).format());
        addLog(userId, "WAITLIST", "Added " + s.getName() + " to waitlist for " + item.getTitle());
        fireChange();
    }

    // Queue doesn't support index access — convert to List, swap, convert back
    @Override
    public void moveWaitlistEntry(String userId, int index, boolean up) {
        List<String> temp = new ArrayList<>(db.getWaitlist());
        int swapWith = up ? index - 1 : index + 1;
        if (swapWith >= 0 && swapWith < temp.size()) {
            saveState(false);
            Collections.swap(temp, index, swapWith);
            setWaitlistFrom(temp);
            fireChange();
        }
    }

    @Override
    public void removeWaitlistEntry(String userId, int index) {
        List<String> temp = new ArrayList<>(db.getWaitlist());
        if (index >= 0 && index < temp.size()) {
            saveState(false);
            temp.remove(index);
            setWaitlistFrom(temp);
            fireChange();
        }
    }

    // Borrow + remove waitlist entry in one saveState so a single Undo reverses both
    /** Fulfills waitlist entry with validation and atomic operations. */
    @Override
    public boolean fulfillWaitlistEntry(String userId, UserAccount student,
                                         LibraryItem item, int waitlistIdx) {
        if (student == null || item == null) return false;
        if (student.getCurrentLoans().stream()
                .anyMatch(r -> r.getItem().getId().equalsIgnoreCase(item.getId()))) return false;
        if (!(item instanceof Borrowable)) return false;
        Borrowable b = (Borrowable) item;
        if (!b.checkout()) return false;
        saveState(false);
        student.addBorrowedItem(item);
        db.recordAccess(item);
        List<String> temp = new ArrayList<>(db.getWaitlist());
        temp.remove(waitlistIdx);
        setWaitlistFrom(temp);
        addLog(userId, "FULFILL_WAITLIST", student.getName() + " issued " + item.getTitle() + " from waitlist");
        fireChange();
        return true;
    }

    // ── 8. Sorting — delegated to SortEngine ─────────────────────────────────

    @Override
    public void insertionSortBy(String field) { saveState(false); SortEngine.insertionSort(db.getCatalogue(), field); fireChange(); }

    @Override
    public void mergeSortBy(String field)     { saveState(false); SortEngine.mergeSort(db.getCatalogue(), field);     fireChange(); }

    @Override
    public void quickSortBy(String field)     { saveState(false); SortEngine.quickSort(db.getCatalogue(), field);     fireChange(); }

    @Override
    public void restoreOrder(List<String> orderedItemIds) {
        if (orderedItemIds == null || orderedItemIds.isEmpty()) return;
        saveState(false);
        Map<String, Integer> pos = new HashMap<>();
        for (int i = 0; i < orderedItemIds.size(); i++) pos.put(orderedItemIds.get(i), i);
        db.getCatalogue().sort((a, b) ->
            Integer.compare(pos.getOrDefault(a.getId(), Integer.MAX_VALUE),
                            pos.getOrDefault(b.getId(), Integer.MAX_VALUE)));
        fireChange();
    }

    // ── 9. Undo / Redo ────────────────────────────────────────────────────────

    // Pass isUndoOrRedo = true from undo()/redo() to preserve the redo stack
    @Override
    public void saveState(boolean isUndoOrRedo) {
        undoHistory.push(new LibraryState(db.getCatalogue(), db.getMembers(), db.getWaitlist()));
        if (undoHistory.size() > 50) undoHistory.remove(0);
        if (!isUndoOrRedo) redoHistory.clear();
        FileHandler.saveAll(db.getCatalogue(), db.getMembers(), db.getWaitlist());
    }

    @Override
    public void undo(String userName) {
        if (!undoHistory.isEmpty()) {
            redoHistory.push(new LibraryState(db.getCatalogue(), db.getMembers(), db.getWaitlist()));
            restoreFromState(undoHistory.pop());
            addLog(userName, "UNDO", "State reverted.");
            fireChange();
        }
    }

    @Override
    public void redo(String userName) {
        if (!redoHistory.isEmpty()) {
            undoHistory.push(new LibraryState(db.getCatalogue(), db.getMembers(), db.getWaitlist()));
            restoreFromState(redoHistory.pop());
            addLog(userName, "REDO", "State restored.");
            fireChange();
        }
    }

    private void restoreFromState(LibraryState state) {
        db.setCatalogue(new ArrayList<>(state.inv));
        db.setMembers(new ArrayList<>(state.std));
        db.setWaitlist(new LinkedList<>(state.wait));
        FileHandler.saveAll(db.getCatalogue(), db.getMembers(), db.getWaitlist());
    }

    // ── 10. Audit log ─────────────────────────────────────────────────────────

    @Override
    public void addLog(String userId, String action, String details) {
        systemLogs.add(new SystemLog(userId, action, details));
        FileHandler.logStealthActivity("User: " + userId + " | " + action + " | " + details);
    }

    // ── 11. Staff management ──────────────────────────────────────────────────

    @Override
    public void addOrUpdateStaff(String operatorId, int staffId, String rawPassword,
                                  String fullName, boolean isAdmin) {
        boolean isNew = !AuthManager.isUserExists(staffId);
        AuthManager.addUser(staffId, rawPassword, fullName, isAdmin);
        addLog(operatorId, isNew ? "CREATE_STAFF" : "UPDATE_STAFF", fullName + " (ID: " + staffId + ")");
    }

    @Override
    public void removeStaff(String operatorId, int staffId) {
        if (staffId == AuthManager.SUPER_ADMIN_ID) return;
        AuthManager.removeUser(staffId);
        addLog(operatorId, "DELETE_STAFF", "ID: " + staffId);
    }

    @Override
    public void resetStaffPassword(String operatorId, int targetId, String newPassword) {
        AuthManager.resetUserPassword(targetId, newPassword);
        addLog(operatorId, "PASS_RESET", "ID: " + targetId);
    }

    @Override public Map<Integer, UserAccount> getAllStaff()   { return AuthManager.getAllUsers(); }
    @Override public boolean staffExists(int staffId)          { return AuthManager.isUserExists(staffId); }

    // ── 12. Auth helpers ──────────────────────────────────────────────────────

    @Override public boolean validatePassword(int userId, String raw) { return AuthManager.validateStatic(userId, raw); }
    @Override public boolean isAdminUser(int userId)                  { return AuthManager.isAdmin(userId); }
    @Override public String  getStaffFullName(int userId)             { return AuthManager.getInstance().getFullName(userId); }
    @Override public int     getSuperAdminId()                        { return AuthManager.SUPER_ADMIN_ID; }

    // ── 13. DTO queries ───────────────────────────────────────────────────────

    @Override
    public List<LoanView> getActiveLoans() {
        List<LoanView> result = new ArrayList<>();
        for (UserAccount s : db.getMembers()) {
            for (BorrowRecord r : s.getCurrentLoans()) {
                boolean deleted = db.findItemById(r.getItem().getId()) == null;
                String status = deleted ? "Item removed from catalogue"
                        : r.isOverdue() ? "OVERDUE \u2014 " + r.getDaysOverdue() + " day(s)"
                                        : "On time \u2014 " + r.getDaysRemaining() + " day(s) left";
                result.add(new LoanView(s.getName(), s.getStudentId(),
                        r.getItem().getId(), r.getItem().getTitle(), r.getItem().getType(),
                        r.getBorrowDate().toString(), r.getDueDate().toString(), status));
            }
        }
        return result;
    }

    @Override
    public List<OverdueLoanView> getOverdueLoans() {
        List<OverdueLoanView> result = new ArrayList<>();
        for (UserAccount s : db.getMembers())
            for (BorrowRecord r : s.getCurrentLoans())
                if (r.isOverdue())
                    result.add(new OverdueLoanView(s.getName(), s.getStudentId(),
                            r.getItem().getTitle(), r.getItem().getType(),
                            r.getDueDate().toString(), r.getDaysOverdue()));
        return result;
    }

    @Override
    public long getOverdueCount() {
        return db.getMembers().stream().flatMap(s -> s.getCurrentLoans().stream())
                .filter(BorrowRecord::isOverdue).count();
    }

    @Override
    public BorrowSummary getBorrowSummary() {
        List<LibraryItem> inventory = db.getCatalogue();
        List<UserAccount> students  = db.getMembers();

        int bookTitles = 0, magTitles = 0, journalTitles = 0;
        int bookCopies = 0, magCopies = 0, journalCopies = 0;
        for (LibraryItem item : inventory) {
            String t = item.getType().toLowerCase();
            if      (t.contains("book"))     { bookTitles++;    bookCopies    += item.getTotalCopies(); }
            else if (t.contains("magazine")) { magTitles++;     magCopies     += item.getTotalCopies(); }
            else if (t.contains("journal"))  { journalTitles++; journalCopies += item.getTotalCopies(); }
        }
        int totalCopies   = bookCopies + magCopies + journalCopies;
        int borrowedCount = students.stream().mapToInt(s -> s.getCurrentLoans().size()).sum();

        Map<String, Integer>     borrowCounts = new HashMap<>();
        Map<String, LibraryItem> itemById     = new HashMap<>();
        List<String>             overdueLines = new ArrayList<>();

        for (UserAccount s : students) {
            for (BorrowRecord r : s.getCurrentLoans()) {
                if (r.getItem() != null) { borrowCounts.merge(r.getItem().getId(), 1, Integer::sum); itemById.put(r.getItem().getId(), r.getItem()); }
                if (r.isOverdue()) overdueLines.add(String.format("  %s (%s) \u2014 %s \u2014 %d day(s) overdue",
                        s.getName(), s.getStudentId(), r.getItem().getTitle(), r.getDaysOverdue()));
            }
            for (BorrowRecord r : s.getHistory())
                if (r.getItem() != null) { borrowCounts.merge(r.getItem().getId(), 1, Integer::sum); itemById.put(r.getItem().getId(), r.getItem()); }
        }

        if (overdueLines.isEmpty()) overdueLines.add("  No overdue items.");

        List<String> mostBorrowedLines = new ArrayList<>();
        if (borrowCounts.isEmpty()) {
            mostBorrowedLines.add("  No borrow history yet.");
        } else {
            borrowCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .limit(10)
                    .forEach(e -> {
                        LibraryItem it = itemById.get(e.getKey());
                        mostBorrowedLines.add(String.format("  %dx  %s by %s [%s]",
                                e.getValue(), it.getTitle(), it.getAuthor(), it.getType()));
                    });
        }

        return new BorrowSummary(bookTitles, magTitles, journalTitles,
                bookCopies, magCopies, journalCopies,
                totalCopies, borrowedCount, db.getWaitlist().size(),
                mostBorrowedLines, overdueLines);
    }

    // Single pass over loans — builds titles, due-dates, and overdue flag together
    @Override
    public List<StudentSummary> getStudentSummaries() {
        List<StudentSummary> result = new ArrayList<>();
        for (UserAccount s : db.getMembers()) {
            List<BorrowRecord> loans = s.getCurrentLoans();
            List<String> titles = new ArrayList<>(), dueDates = new ArrayList<>();
            boolean hasOverdue = false;
            for (BorrowRecord r : loans) {
                titles.add(r.getItem().getTitle());
                if (r.isOverdue()) { dueDates.add(r.getDueDate() + " (OVERDUE!)"); hasOverdue = true; }
                else                 dueDates.add(r.getDueDate().toString());
            }
            result.add(new StudentSummary(s.getStudentId(), s.getName(), loans.size(),
                    titles.isEmpty()   ? "None" : String.join(", ", titles),
                    dueDates.isEmpty() ? "N/A"  : String.join(", ", dueDates),
                    hasOverdue));
        }
        return result;
    }

    // ── 14. Raw accessors ─────────────────────────────────────────────────────

    @Override public List<LibraryItem> getInventory()        { return db.getCatalogue(); }
    @Override public Queue<String>     getWaitlist()          { return db.getWaitlist(); }
    @Override public List<LibraryItem> getMostAccessedItems() { return db.getMostAccessedItems(); }
    @Override public List<SystemLog>   getSystemLogs()        { return systemLogs; }
    @Override public int getCacheAccessCount(int idx)         { return db.getAccessCount(idx); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Factory method to create appropriate LibraryItem subclass based on type. */
    private LibraryItem buildItem(String type, String id, String title, String author, int year) {
        if      (type.equalsIgnoreCase("Journal"))  return new Journal(id, title, author, year);
        else if (type.equalsIgnoreCase("Magazine")) return new Magazine(id, title, author, year);
        else                                         return new Book(id, title, author, year);
    }

    /** Updates waitlist with new list of entries. */
    private void setWaitlistFrom(List<String> entries) { db.setWaitlist(new LinkedList<>(entries)); }
}
