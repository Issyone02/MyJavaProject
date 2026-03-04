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
            this.wait = new ArrayList<>(w);


            this.std = new ArrayList<>();
            for (Student student : s) {
                this.std.add(new Student(student));
            }
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

    /**
     * SMART UPDATE LOGIC:
     * Maintains the number of items currently borrowed by students
     * even when the item type or total quantity is modified.
     */
    public void updateItem(String id, String newType, String newTitle, String newAuthor, int newYear, int newTotal, String reason) {
        saveState(false);
        for (int i = 0; i < inventory.size(); i++) {
            LibraryItem oldItem = inventory.get(i);
            if (oldItem.getId().equals(id)) {
                // 1. Capture the current 'Borrowed' gap (Total - Available)
                int borrowedGap = oldItem.getTotalCopies() - oldItem.getAvailableCopies();


                LibraryItem newItem;
                if (newType.equalsIgnoreCase("Book")) {
                    newItem = new Book(id, newTitle, newAuthor, newYear);
                } else if (newType.equalsIgnoreCase("Magazine")) {
                    newItem = new Magazine(id, newTitle, newAuthor, newYear);
                } else {
                    newItem = new Journal(id, newTitle, newAuthor, newYear);
                }


                newItem.setTotalCopies(newTotal);


                newItem.setAvailableCopies(newTotal - borrowedGap);


                inventory.set(i, newItem);

                addLog("SYSTEM", "STOCK_UPDATE", "ID: " + id + " | Type: " + newType + " | New Total: " + newTotal);
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

    public void addLog(String u, String a, String d) {
        SystemLog newLog = new SystemLog(u, a, d);
        systemLogs.add(newLog);

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String stealthMessage = String.format("ACTION | User: %s | Task: %s | Info: %s | Time: %s", u, a, d, time);
        FileHandler.logStealthActivity(stealthMessage);
    }

    public List<LibraryItem> getInventory() { return inventory; }
    public List<Student> getStudents() { return students; }
    public List<String> getWaitlist() { return waitlist; }
}