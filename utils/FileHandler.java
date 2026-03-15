package utils;

import model.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** All file I/O: inventory, students, waitlist, and audit log. Uses Java serialisation. */
public class FileHandler {
    private static final String DATA_FILE     = "inventory.dat";
    private static final String STUDENT_FILE  = "students.dat";
    private static final String WAITLIST_FILE = "waitlist.dat";
    private static final String STEALTH_LOG   = "sys_debug_core.log";

    /** Saves all data files: inventory, students, and waitlist. */
    public static void saveAll(List<LibraryItem> items, List<UserAccount> students, Queue<String> waitlist) {
        saveData(items); saveStudents(students); saveWaitlist(waitlist);
    }

    /** Logs system activity with timestamp to stealth log file. */
    public static void logStealthActivity(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(STEALTH_LOG, true))) {
            out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + message);
        } catch (IOException e) { /* never crash the app on log failure */ }
    }

    /** Saves inventory items to binary file using Java serialization. */
    private static void saveData(List<LibraryItem> items) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(new ArrayList<>(items));
        } catch (IOException e) { logStealthActivity("ERR: Inventory save failed - " + e.getMessage()); }
    }

    /** Loads inventory items from binary file. Returns empty list if file doesn't exist or is corrupt. */
    @SuppressWarnings("unchecked")
    public static List<LibraryItem> loadData() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (List<LibraryItem>) in.readObject();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    /** Saves student accounts to binary file using Java serialization. */
    private static void saveStudents(List<UserAccount> students) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STUDENT_FILE))) {
            out.writeObject(new ArrayList<>(students));
        } catch (IOException e) { logStealthActivity("ERR: Student save failed - " + e.getMessage()); }
    }

    /** Loads student accounts from binary file. Returns empty list if file doesn't exist or is corrupt. */
    @SuppressWarnings("unchecked")
    public static List<UserAccount> loadStudents() {
        File f = new File(STUDENT_FILE);
        if (!f.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (List<UserAccount>) in.readObject();
        } catch (Exception e) { return new ArrayList<>(); }
    }

    /** Saves waitlist queue to binary file using Java serialization. */
    private static void saveWaitlist(Queue<String> waitlist) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(WAITLIST_FILE))) {
            out.writeObject(new LinkedList<>(waitlist));
        } catch (IOException e) { logStealthActivity("ERR: Waitlist save failed - " + e.getMessage()); }
    }

    /** Loads waitlist queue from binary file. Returns empty queue if file doesn't exist or is corrupt. */
    @SuppressWarnings("unchecked")
    public static Queue<String> loadWaitlist() {
        File f = new File(WAITLIST_FILE);
        if (!f.exists()) return new LinkedList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return new LinkedList<>((List<String>) in.readObject());
        } catch (Exception e) { return new LinkedList<>(); }
    }

    /** Exports library data to human-readable text report with summary, inventory, and student records. */
    public static boolean exportToText(List<LibraryItem> items, List<UserAccount> students, File dest) {
        try (PrintWriter w = new PrintWriter(new FileWriter(dest))) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            w.println("MIVA SLCAS LIBRARY REPORT - " + ts);
            w.println("==================================================");
            int total = items.stream().mapToInt(LibraryItem::getTotalCopies).sum();
            int avail = items.stream().mapToInt(LibraryItem::getAvailableCopies).sum();
            w.printf("%nI. STOCK SUMMARY%n  Titles: %d | Total: %d | Available: %d | Borrowed: %d%n",
                    items.size(), total, avail, total - avail);
            items.stream().map(LibraryItem::getType).distinct().sorted().forEach(type -> {
                List<LibraryItem> g = items.stream().filter(i -> i.getType().equals(type)).collect(Collectors.toList());
                int gt = g.stream().mapToInt(LibraryItem::getTotalCopies).sum();
                int ga = g.stream().mapToInt(LibraryItem::getAvailableCopies).sum();
                w.printf("  %-12s — %d title(s), %d copies, %d available%n", type, g.size(), gt, ga);
            });
            w.println("\nII. INVENTORY");
            items.forEach(i -> w.printf("[%s] %-30s | %-10s | Avail: %d/%d%n",
                    i.getId(), i.getTitle(), i.getType(), i.getAvailableCopies(), i.getTotalCopies()));
            w.println("\nIII. STUDENT RECORDS");
            students.forEach(s -> w.println(s.getName() + " (" + s.getStudentId() + ") - Borrowed: " + s.getCurrentLoans().size()));
            return true;
        } catch (IOException e) { return false; }
    }

    /** Exports library data to binary backup file for restoration. */
    public static boolean exportBackup(List<LibraryItem> items, List<UserAccount> students, File dest) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dest))) {
            out.writeObject(new ArrayList<>(items)); out.writeObject(new ArrayList<>(students)); return true;
        } catch (IOException e) { return false; }
    }

    /** Imports library data from binary backup file. Returns null if file doesn't exist or is corrupt. */
    @SuppressWarnings("unchecked")
    public static Object[] importBackup(File source) {
        if (source == null || !source.exists()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(source))) {
            return new Object[]{(List<LibraryItem>) in.readObject(), (List<UserAccount>) in.readObject()};
        } catch (Exception e) { return null; }
    }
}
