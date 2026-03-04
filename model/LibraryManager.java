package model;

import java.util.*;
import java.io.Serializable;
import utils.FileHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LibraryManager implements Serializable {
    // All the items the Library currently holds
    private List<LibraryItem> inventory = new ArrayList<>();
    // Every Student registered in the system
    private List<Student> students = new ArrayList<>();
    // Student waiting for an unavailable item
    private List<String> waitlist = new ArrayList<>();
    // A running Log of every action taken in the system
    private List<SystemLog> systemLogs = new ArrayList<>();

    // Stores past states so we can roll back to them
    private final Stack<LibraryState> undoHistory = new Stack<>();
    // Stores undone states so we can bring them forward later
    private final Stack<LibraryState> redoHistory = new Stack<>();

    public LibraryManager() {
        // Capture the initial empty state instantly
        // so there's always at least one snapshot to fall back on
        saveState(false);
    }

    private static class LibraryState implements Serializable {
    // A snapshot of the inventory at a specific point in time
        final List<LibraryItem> inv;
    // A snapshot of the student list at the same point
        final List<Student> std;
    // A snapshot of the waitlist at the same point
        final List<String> wait;

        LibraryState(List<LibraryItem> i, List<Student> s, List<String> w) {
            this.inv = new ArrayList<>(i);
            this.wait = new ArrayList<>(w);


            this.std = new ArrayList<>();
            for (Student student : s) {
    // Students need a deep copy because their loan and history lists can change
                this.std.add(new Student(student));
            }
        }
    }

    public void saveState() {
    /**
     *  Convenience overload - It is assumed that this is a normal action
     *  not an undo or redo
     */
        saveState(false);
    }

    public void saveState(boolean isUndoOrRedo) {
        // Take a snapshot and push it on to the undo stack
        undoHistory.push(new LibraryState(inventory, students, waitlist));
        // Keeps the stack from growing forever(50 snapshots)
        if (undoHistory.size() > 50) undoHistory.remove(0);
        // If this is a fresh action, wipe the redo history
        if (!isUndoOrRedo) redoHistory.clear();
    }

    public void undo() { undo("SYSTEM"); }

    public void undo(String userName) {
        // We need more than one state
        if (undoHistory.size() > 1) {
            // Move the current state to redo so we can come back to it if needed
            redoHistory.push(undoHistory.pop());
            // Peek at the state just before the one we removed
            LibraryState previous = undoHistory.peek();
            //Restore the inventory to what it was before
            this.inventory = new ArrayList<>(previous.inv);
            // Restore the student list too
            this.students = new ArrayList<>(previous.std);
            // Restore waitlist too
            this.waitlist = new ArrayList<>(previous.wait);
            // Record that an undo happened and who initiated it
            addLog(userName, "UNDO", "State reverted.");
        }
    }

    public void redo(String userName) {
                //Only proceed, if there's actually something to redo
        if (!redoHistory.isEmpty()) {
                // Grab the most recently undone state
            LibraryState nextState = redoHistory.pop();
                // Put it back to the undo stack so it can be undone  again
            undoHistory.push(nextState);
                // Restore the inventory to the Redone state
            this.inventory = new ArrayList<>(nextState.inv);
                // Restore Student
            this.students = new ArrayList<>(nextState.std);
                // Restore  the Wait-list
            this.waitlist = new ArrayList<>(nextState.wait);
            // Log who performed the redo action
            addLog(userName, "REDO", "State restored.");
        }
    }



    public Student findStudentById(String id) {
        return students.stream()
                // Case-insensitive match e.g 'S001' and 's001' work
                .filter(s -> s.getStudentId().equalsIgnoreCase(id))
        // Returns empty if no student matches - callers should check
                .findFirst()
                .orElse(null);
    }

    public void removeStudent(String id) {
        // Save before removing so the action can be undone
        saveState(false);
        // Remove the student whose ID matches exactly
        students.removeIf(s -> s.getStudentId().equals(id));
    }

    public void updateStudent(String id, String newName) {
        // Save current state before making changes
        saveState(false);
        for (Student s : students) {
            if (s.getStudentId().equals(id)) {
        // Found the right student — update their name
                s.setName(newName);
        // No need to keep looping once we've found the match
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
        // Snapshot the current state before making any changes
        saveState(false);
        for (int i = 0; i < inventory.size(); i++) {
            LibraryItem oldItem = inventory.get(i);
            // 1. Capture the current 'Borrowed' gap (Total - Available)
            if (oldItem.getId().equals(id)) {

                // How many copies are currently out with students
                int borrowedGap = oldItem.getTotalCopies() - oldItem.getAvailableCopies();


                LibraryItem newItem;
                if (newType.equalsIgnoreCase("Book")) {
                // Build a new Book with the updated details
                    newItem = new Book(id, newTitle, newAuthor, newYear);
                } else if (newType.equalsIgnoreCase("Magazine")) {
                    // Build a new Magazine instead
                    newItem = new Magazine(id, newTitle, newAuthor, newYear);
                } else {
                // Default to Journal if neither of the above
                    newItem = new Journal(id, newTitle, newAuthor, newYear);
                }

                // Apply the new total quantity
                newItem.setTotalCopies(newTotal);

                // Subtract currently borrowed copies so the available count stays accurate
                newItem.setAvailableCopies(newTotal - borrowedGap);

                // Swap the old item out with the freshly built one
                inventory.set(i, newItem);
                // Record the change in the system log
                addLog("SYSTEM", "STOCK_UPDATE", "ID: " + id + " | Type: " + newType + " | New Total: " + newTotal);
                // Item found and updated — stop looping
                break;
            }
        }
    }

    // Returns the logs in a read-only wrapper so nothing outside this class can modify them
    public List<SystemLog> getSystemLogs() { return Collections.unmodifiableList(systemLogs); }

    // Saves state then sorts inventory alphabetically by title, ignoring case
    public void mergeSortByTitle() { saveState(false); inventory.sort((a,b) -> a.getTitle().compareToIgnoreCase(b.getTitle())); }
    // Saves state then sorts inventory from oldest to newest by publication year
    public void selectionSortByYear() { saveState(false); inventory.sort(Comparator.comparingInt(LibraryItem::getYear)); }
    // Saves state then groups inventory by item type (Book, Magazine, Journal)
    public void sortByType() { saveState(false); inventory.sort(Comparator.comparing(LibraryItem::getType)); }

    // Saves state then wipes the entire waitlist clean
    public void clearWaitlist() { saveState(false); this.waitlist.clear(); }
    // Saves state then adds a new item to the inventory
    public void addItem(LibraryItem item) { saveState(false); inventory.add(item); }
}   // Saves state then removes the item with the matching ID
    public void removeItem(String id) { saveState(false); inventory.removeIf(i -> i.getId().equals(id)); }
// Saves state then registers a new student in the system
public void addStudent(Student s) { saveState(false); students.add(s); }

    public void addLog(String u, String a, String d) {
    // Create a new log entry with the user, action, and details
    SystemLog newLog = new SystemLog(u, a, d);
        // Add it to the in-memory log list
        systemLogs.add(newLog);

        // Capture the exact time this log was written
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        // Format a structured message for the external log file
        String stealthMessage = String.format("ACTION | User: %s | Task: %s | Info: %s | Time: %s", u, a, d, time);
        // Write it silently to the file — this runs in the background without showing anything to the user
        FileHandler.logStealthActivity(stealthMessage);
    }

    // Returns the full list of library items
    public List<LibraryItem> getInventory() { return inventory; }
    // Returns all registered students
    public List<Student> getStudents() { return students; }
    // Returns the current waitlist
    public List<String> getWaitlist() { return waitlist; }
}