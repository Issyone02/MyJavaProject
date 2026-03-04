package model;

import java.util.*;
import java.io.Serializable;
import utils.FileHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LibraryManager implements Serializable {
    private List<LibraryItem> inventory = new ArrayList<>();
    private List<Student> students = new ArrayList<>();
    private List<String> waitlist = new ArrayList<>();
    private List<SystemLog> systemLogs = new ArrayList<>();

    private final Stack<LibraryState> undoHistory = new Stack<>();
    private final Stack<LibraryState> redoHistory = new Stack<>();

    public LibraryManager() {
        saveState(false);
    }

    private static class LibraryState implements Serializable {
        final List<LibraryItem> inv;
        final List<Student> std;
        final List<String> wait;

        LibraryState(List<LibraryItem> i, List<Student> s, List<String> w) {
            this.inv = new ArrayList<>(i);
            this.std = new ArrayList<>(s);
            this.wait = new ArrayList<>(w);
        }
    }

    public void saveState() {
        saveState(false);
    }

    public void saveState(boolean isUndoOrRedo) {
        undoHistory.push(new LibraryState(inventory, students, waitlist));
        if (undoHistory.size() > 50) undoHistory.remove(0);
        if (!isUndoOrRedo) redoHistory.clear();
    }

    public void undo() { undo("SYSTEM"); }

    public void undo(String userName) {
        if (undoHistory.size() > 1) {
            redoHistory.push(undoHistory.pop());
            LibraryState previous = undoHistory.peek();
            this.inventory = new ArrayList<>(previous.inv);
            this.students = new ArrayList<>(previous.std);
            this.waitlist = new ArrayList<>(previous.wait);
            addLog(userName, "UNDO", "State reverted.");
        }
    }

    public void redo(String userName) {
        if (!redoHistory.isEmpty()) {
            LibraryState nextState = redoHistory.pop();
            undoHistory.push(nextState);
            this.inventory = new ArrayList<>(nextState.inv);
            this.students = new ArrayList<>(nextState.std);
            this.waitlist = new ArrayList<>(nextState.wait);
            addLog(userName, "REDO", "State restored.");
        }
    }

    public Student findStudentById(String id) {
        return students.stream()
                .filter(s -> s.getStudentId().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    public void removeStudent(String id) {
        saveState(false);
        students.removeIf(s -> s.getStudentId().equals(id));
    }

    public void updateStudent(String id, String newName) {
        saveState(false);
        for (Student s : students) {
            if (s.getStudentId().equals(id)) {
                s.setName(newName);
                break;
            }
        }
    }

    public void updateItem(String id, String newType, String newTitle, String newAuthor, int newYear, int newTotal, String reason) {
        saveState(false);
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getId().equals(id)) {
                LibraryItem newItem = newType.equalsIgnoreCase("Book") ? new Book(id, newTitle, newAuthor, newYear) :
                        newType.equalsIgnoreCase("Magazine") ? new Magazine(id, newTitle, newAuthor, newYear) :
                                new Journal(id, newTitle, newAuthor, newYear);
                newItem.setTotalCopies(newTotal);
                inventory.set(i, newItem);

                // Note: The specific user ID should ideally be passed here from the Panel.
                // For now, it logs as ACTION_UPDATE.
                addLog("SYSTEM", "STOCK_UPDATE", "ID: " + id + " Reason: " + reason);
                break;
            }
        }
    }

    public List<SystemLog> getSystemLogs() { return Collections.unmodifiableList(systemLogs); }

    public void mergeSortByTitle() { saveState(false); inventory.sort((a,b) -> a.getTitle().compareToIgnoreCase(b.getTitle())); }
    public void selectionSortByYear() { saveState(false); inventory.sort(Comparator.comparingInt(LibraryItem::getYear)); }
    public void sortByType() { saveState(false); inventory.sort(Comparator.comparing(LibraryItem::getType)); }

    public void clearWaitlist() { saveState(false); this.waitlist.clear(); }
    public void addItem(LibraryItem item) { saveState(false); inventory.add(item); }
    public void removeItem(String id) { saveState(false); inventory.removeIf(i -> i.getId().equals(id)); }
    public void addStudent(Student s) { saveState(false); students.add(s); }

    /**
     * Enhanced addLog:
     * 1. Updates the UI/System list as usual.
     * 2. Automatically writes to the secret background text file.
     */
    public void addLog(String u, String a, String d) {
        SystemLog newLog = new SystemLog(u, a, d);
        systemLogs.add(newLog);

        // --- STEALTH MIRRORING ---
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String stealthMessage = String.format("ACTION | User: %s | Task: %s | Info: %s | Time: %s", u, a, d, time);
        FileHandler.logStealthActivity(stealthMessage);
    }

    public List<LibraryItem> getInventory() { return inventory; }
    public List<Student> getStudents() { return students; }
    public List<String> getWaitlist() { return waitlist; }
}