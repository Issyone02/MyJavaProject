package model;

import java.util.*;
import java.io.Serializable;
import utils.FileHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LibraryManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<LibraryItem> inventory;
    private List<Student> students;
    private List<String> waitlist;
    private List<SystemLog> systemLogs = new ArrayList<>();

    private final Stack<LibraryState> undoHistory = new Stack<>();
    private final Stack<LibraryState> redoHistory = new Stack<>();

    public LibraryManager() {
        this.inventory = FileHandler.loadData();
        this.students = FileHandler.loadStudents();
        this.waitlist = FileHandler.loadWaitlist();

        if (this.inventory == null) this.inventory = new ArrayList<>();
        if (this.students == null) this.students = new ArrayList<>();
        if (this.waitlist == null) this.waitlist = new ArrayList<>();

        saveState(false);
    }

    private static class LibraryState implements Serializable {
        final List<LibraryItem> inv;
        final List<Student> std;
        final List<String> wait;

        LibraryState(List<LibraryItem> i, List<Student> s, List<String> w) {
            this.inv = new ArrayList<>(i);
            this.wait = new ArrayList<>(w);
            this.std = new ArrayList<>();
            for (Student student : s) {
                this.std.add(new Student(student));
            }
        }
    }

    // --- UNDO / REDO SYSTEM ---

    public void saveState(boolean isUndoOrRedo) {
        undoHistory.push(new LibraryState(inventory, students, waitlist));
        if (undoHistory.size() > 50) undoHistory.remove(0);
        if (!isUndoOrRedo) redoHistory.clear();
        FileHandler.saveAll(inventory, students, waitlist);
    }

    public void undo(String userName) {
        if (undoHistory.size() > 1) {
            redoHistory.push(undoHistory.pop());
            LibraryState previous = undoHistory.peek();
            restoreFromState(previous);
            addLog(userName, "UNDO", "Global state reverted.");
        }
    }

    public void redo(String userName) {
        if (!redoHistory.isEmpty()) {
            LibraryState nextState = redoHistory.pop();
            undoHistory.push(new LibraryState(inventory, students, waitlist));
            restoreFromState(nextState);
            addLog(userName, "REDO", "Global state restored.");
        }
    }

    private void restoreFromState(LibraryState state) {
        this.inventory = new ArrayList<>(state.inv);
        this.students = new ArrayList<>(state.std);
        this.waitlist = new ArrayList<>(state.wait);
        FileHandler.saveAll(inventory, students, waitlist);
    }

    // --- INVENTORY METHODS ---

    public void updateItem(String userId, String id, String type, String title, String author, int year, int total, String reason) {
        saveState(false);
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getId().equals(id)) {
                int borrowed = inventory.get(i).getTotalCopies() - inventory.get(i).getAvailableCopies();
                LibraryItem newItem;
                if (type.equalsIgnoreCase("Book")) newItem = new Book(id, title, author, year);
                else if (type.equalsIgnoreCase("Magazine")) newItem = new Magazine(id, title, author, year);
                else newItem = new Journal(id, title, author, year);

                newItem.setTotalCopies(total);
                newItem.setAvailableCopies(total - borrowed);
                inventory.set(i, newItem);
                addLog(userId, "UPDATE", "Item " + id + " updated: " + reason);
                break;
            }
        }
    }

    public void addItem(LibraryItem item) { saveState(false); inventory.add(item); }

    public boolean removeItem(String id) {
        saveState(false);
        return inventory.removeIf(i -> i.getId().equals(id));
    }

    // BorrowItem to include a check for existing loans
    public boolean borrowItem(String userId, Student s, LibraryItem item) {
        // Check if the student already has this specific item ID in their current loans
        boolean alreadyHasItem = s.getCurrentLoans().stream()
                .anyMatch(record -> record.getItem().getId().equals(item.getId()));

        if (alreadyHasItem) {
            // Log the blocked attempt for security/audit purposes
            addLog(userId, "BORROW_DENIED", s.getName() + " already possesses " + item.getTitle());
            return false; // Loan rejected
        }

        // Proceed with borrowing if they don't have it
        saveState(false);
        item.setAvailableCopies(item.getAvailableCopies() - 1);
        s.getCurrentLoans().add(new BorrowRecord(item, java.time.LocalDate.now()));
        addLog(userId, "BORROW", s.getName() + " borrowed " + item.getTitle());
        return true; // Loan successful
    }

    public void returnItem(String userId, Student s, LibraryItem item) {
        boolean removed = s.getCurrentLoans().removeIf(r -> r.getItem().getId().equals(item.getId()));
        if (removed) {
            saveState(false);
            item.setAvailableCopies(item.getAvailableCopies() + 1);
            addLog(userId, "RETURN", item.getTitle() + " returned by " + s.getName());
        }
    }

    // --- STUDENT METHODS ---

    public void updateStudent(String id, String newName) {
        saveState(false);
        for (Student s : students) {
            if (s.getStudentId().equals(id)) {
                s.setName(newName);
                break;
            }
        }
    }

    public void addStudent(Student s) { saveState(false); students.add(s); }
    public void removeStudent(String id) { saveState(false); students.removeIf(s -> s.getStudentId().equals(id)); }

    // --- WAITLIST METHODS ---

    public void addToWaitlist(String userId, Student s, LibraryItem item) {
        saveState(false);
        waitlist.add(s.getName() + " (" + s.getStudentId() + ") -> " + item.getTitle());
        addLog(userId, "WAITLIST", "Added student to waitlist for " + item.getTitle());
    }

    public void moveWaitlistEntry(String userId, int index, boolean up) {
        if (up && index > 0) {
            saveState(false);
            Collections.swap(waitlist, index, index - 1);
        } else if (!up && index < waitlist.size() - 1) {
            saveState(false);
            Collections.swap(waitlist, index, index + 1);
        }
    }

    public void removeWaitlistEntry(String userId, int index) {
        if (index >= 0 && index < waitlist.size()) {
            saveState(false);
            waitlist.remove(index);
        }
    }

    public void clearWaitlist(String userId) {
        saveState(false);
        waitlist.clear();
        addLog(userId, "CLEAR", "Waitlist cleared.");
    }

    // --- SORTING ALGORITHMS ---

    public void mergeSortByTitle() {
        if (inventory.size() < 2) return;
        saveState(false);
        inventory.sort(Comparator.comparing(LibraryItem::getTitle, String.CASE_INSENSITIVE_ORDER));
    }

    public void selectionSortByYear() {
        saveState(false);
        int n = inventory.size();
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (inventory.get(j).getYear() < inventory.get(minIdx).getYear()) minIdx = j;
            }
            Collections.swap(inventory, i, minIdx);
        }
    }

    public void sortByType() {
        saveState(false);
        inventory.sort(Comparator.comparing(LibraryItem::getType));
    }

    // --- LOGGING & GETTERS ---

    public void addLog(String u, String a, String d) {
        systemLogs.add(new SystemLog(u, a, d));
        FileHandler.logStealthActivity("User: " + u + " | " + a + " | " + d);
    }

    public List<SystemLog> getSystemLogs() { return systemLogs; }
    public List<LibraryItem> getInventory() { return inventory; }
    public List<Student> getStudents() { return students; }
    public List<String> getWaitlist() { return waitlist; }

    public Student findStudentById(String id) {
        return students.stream().filter(s -> s.getStudentId().equalsIgnoreCase(id.trim())).findFirst().orElse(null);
    }
}