package utils;

import model.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * FileHandler - Manages all file I/O operations
 *
 * Handles:
 * - Saving/loading library inventory (.dat files)
 * - Saving/loading student records
 * - Saving/loading waitlist
 * - Export/import functionality
 * - Theme preference storage
 * - Background activity logging
 *
 * All data is persisted using Java serialization for easy object storage.
 */
public class FileHandler {
    // File paths for persistent storage
    private static final String DATA_FILE = "inventory.dat";
    private static final String STUDENT_FILE = "students.dat";
    private static final String WAITLIST_FILE = "waitlist.dat";
    private static final String CONFIG_FILE = "config.txt";
    private static final String STEALTH_LOG = "sys_debug_core.log";

    /**
     * Saves all library data in one operation
     * Called by LibraryManager.saveState()
     */
    public static void saveAll(List<LibraryItem> items, List<Student> students, List<String> waitlist) {
        saveData(items);
        saveStudents(students);
        saveWaitlist(waitlist);
    }

    /**
     * Logs system activity to background file with timestamp
     * Used for audit trail and debugging
     */
    public static void logStealthActivity(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(STEALTH_LOG, true))) {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println("[" + timestamp + "] " + message);
            out.flush();
        } catch (IOException e) {
            // Fail silently - logging should not crash the app
        }
    }

    // ==================== INVENTORY PERSISTENCE ====================

    /** Saves inventory to disk */
    public static void saveData(List<LibraryItem> items) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(new ArrayList<>(items));
        } catch (IOException e) {
            logStealthActivity("ERR: Inventory save failed - " + e.getMessage());
        }
    }

    /** Loads inventory from disk */
    public static List<LibraryItem> loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return new ArrayList<>();  // Empty if no file
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (List<LibraryItem>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();  // Return empty on error
        }
    }

    // ==================== STUDENT PERSISTENCE ====================

    /** Saves student records to disk */
    public static void saveStudents(List<Student> students) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STUDENT_FILE))) {
            out.writeObject(new ArrayList<>(students));
        } catch (IOException e) {
            logStealthActivity("ERR: Student save failed - " + e.getMessage());
        }
    }

    /** Loads student records from disk */
    public static List<Student> loadStudents() {
        File file = new File(STUDENT_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Student>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ==================== EXPORT FUNCTIONALITY ====================

    /**
     * Exports library data as human-readable text file
     * @return true if successful
     */
    public static boolean exportToText(List<LibraryItem> items, List<Student> students, File destination) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destination))) {
            // Write header
            writer.println("MIVA SLCAS LIBRARY BACKUP - " + java.time.LocalDate.now());
            writer.println("==================================================");

            // Write inventory section
            writer.println("\nI. INVENTORY");
            for (LibraryItem item : items) {
                writer.printf("[%s] %-20s | Type: %-10s | Stock: %d/%d%n",
                        item.getId(), item.getTitle(), item.getType(),
                        item.getAvailableCopies(), item.getTotalCopies());
            }

            // Write student section
            writer.println("\nII. STUDENT RECORDS");
            for (Student s : students) {
                writer.println(s.getName() + " (" + s.getStudentId() +
                        ") - Borrowed Count: " + s.getCurrentLoans().size());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Exports library data as binary backup file
     * Can be re-imported later
     * @return true if successful
     */
    public static boolean exportBackup(List<LibraryItem> items, List<Student> students, File destination) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(destination))) {
            out.writeObject(new ArrayList<>(items));
            out.writeObject(new ArrayList<>(students));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Imports library data from backup file
     * @return Array of [items, students] or null if failed
     */
    public static Object[] importBackup(File source) {
        if (source == null || !source.exists()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(source))) {
            List<LibraryItem> items = (List<LibraryItem>) in.readObject();
            List<Student> students = (List<Student>) in.readObject();
            return new Object[]{items, students};
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== WAITLIST PERSISTENCE ====================

    /** Saves waitlist to disk */
    public static void saveWaitlist(List<String> waitlist) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(WAITLIST_FILE))) {
            out.writeObject(new ArrayList<>(waitlist));
        } catch (IOException e) {
            logStealthActivity("ERR: Waitlist save failed - " + e.getMessage());
        }
    }

    /** Loads waitlist from disk */
    public static List<String> loadWaitlist() {
        File file = new File(WAITLIST_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (List<String>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ==================== THEME PREFERENCE ====================

    /** Saves user's theme preference (dark/light mode) */
    public static void saveThemePreference(boolean isDarkMode) {
        try (PrintWriter out = new PrintWriter(new FileWriter(CONFIG_FILE))) {
            out.print(isDarkMode);
        } catch (IOException e) {
            // Fail silently - not critical
        }
    }

    /** Loads user's theme preference */
    public static boolean loadThemePreference() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return false;  // Default to light mode
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line = in.readLine();
            return line != null && Boolean.parseBoolean(line);
        } catch (Exception e) {
            return false;
        }
    }
}