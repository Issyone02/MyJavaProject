package utils;

import model.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles all file I/O: saving/loading inventory, students, waitlist, and logs.
 * Uses Java serialisation (.dat files) for persistence.
 */
public class FileHandler {
    private static final String DATA_FILE     = "inventory.dat";
    private static final String STUDENT_FILE  = "students.dat";
    private static final String WAITLIST_FILE = "waitlist.dat";
    private static final String STEALTH_LOG   = "sys_debug_core.log";

    // Saves all three data stores at once for consistency
    public static void saveAll(List<LibraryItem> items, List<UserAccount> students, Queue<String> waitlist) {
        saveData(items);
        saveStudents(students);
        saveWaitlist(waitlist);
    }

    // Appends a timestamped message to the debug/audit log
    public static void logStealthActivity(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(STEALTH_LOG, true))) {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println("[" + timestamp + "] " + message);
            out.flush();
        } catch (IOException e) {
            // Logging errors should never crash the app
        }
    }

    // ── Inventory ────────────────────────────────────────────────────────────

    public static void saveData(List<LibraryItem> items) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(new ArrayList<>(items));
        } catch (IOException e) {
            logStealthActivity("ERR: Inventory save failed - " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<LibraryItem> loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (List<LibraryItem>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ── Students ─────────────────────────────────────────────────────────────

    public static void saveStudents(List<UserAccount> students) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STUDENT_FILE))) {
            out.writeObject(new ArrayList<>(students));
        } catch (IOException e) {
            logStealthActivity("ERR: Student save failed - " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<UserAccount> loadStudents() {
        File file = new File(STUDENT_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (List<UserAccount>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ── Waitlist ─────────────────────────────────────────────────────────────

    public static void saveWaitlist(Queue<String> waitlist) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(WAITLIST_FILE))) {
            out.writeObject(new LinkedList<>(waitlist));
        } catch (IOException e) {
            logStealthActivity("ERR: Waitlist save failed - " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Queue<String> loadWaitlist() {
        File file = new File(WAITLIST_FILE);
        if (!file.exists()) return new LinkedList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            List<String> loaded = (List<String>) in.readObject();
            return new LinkedList<>(loaded);
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

    // ── Export / Import ──────────────────────────────────────────────────────

    // Exports inventory + students to a human-readable text file
    public static boolean exportToText(List<LibraryItem> items, List<UserAccount> students, File destination) {
        try (PrintWriter w = new PrintWriter(new FileWriter(destination))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            w.println("MIVA SLCAS LIBRARY REPORT - " + timestamp);
            w.println("==================================================");

            // Stock summary
            int totalCopies = items.stream().mapToInt(LibraryItem::getTotalCopies).sum();
            int totalAvail  = items.stream().mapToInt(LibraryItem::getAvailableCopies).sum();
            w.println("\nI. STOCK SUMMARY");
            w.printf("  Titles: %d | Total Copies: %d | Available: %d | Borrowed: %d%n",
                    items.size(), totalCopies, totalAvail, totalCopies - totalAvail);

            // Per-category breakdown
            items.stream().map(LibraryItem::getType).distinct().sorted().forEach(type -> {
                List<LibraryItem> group = items.stream().filter(i -> i.getType().equals(type)).collect(java.util.stream.Collectors.toList());
                int gTotal = group.stream().mapToInt(LibraryItem::getTotalCopies).sum();
                int gAvail = group.stream().mapToInt(LibraryItem::getAvailableCopies).sum();
                w.printf("  %-12s — %d title(s), %d cop(ies), %d available%n", type, group.size(), gTotal, gAvail);
            });

            // Inventory
            w.println("\nII. INVENTORY");
            for (LibraryItem item : items) {
                w.printf("[%s] %-30s | %-10s | Available: %d / %d%n",
                        item.getId(), item.getTitle(), item.getType(),
                        item.getAvailableCopies(), item.getTotalCopies());
            }

            // Student records
            w.println("\nIII. STUDENT RECORDS");
            for (UserAccount s : students) {
                w.println(s.getName() + " (" + s.getStudentId() + ") - Borrowed: " + s.getCurrentLoans().size());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Exports to a binary backup file (can be re-imported later)
    public static boolean exportBackup(List<LibraryItem> items, List<UserAccount> students, File destination) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(destination))) {
            out.writeObject(new ArrayList<>(items));
            out.writeObject(new ArrayList<>(students));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Imports from a binary backup file
    @SuppressWarnings("unchecked")
    public static Object[] importBackup(File source) {
        if (source == null || !source.exists()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(source))) {
            List<LibraryItem> items = (List<LibraryItem>) in.readObject();
            List<UserAccount> students = (List<UserAccount>) in.readObject();
            return new Object[]{items, students};
        } catch (Exception e) {
            return null;
        }
    }
}
