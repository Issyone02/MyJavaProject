package utils;

import model.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all System Persistence.
 */
public class FileHandler {
    private static final String DATA_FILE = "inventory.dat";
    private static final String STUDENT_FILE = "students.dat";
    private static final String WAITLIST_FILE = "waitlist.dat";
    private static final String CONFIG_FILE = "config.txt";
    // Stealth log file name - named to look like a system file
    private static final String STEALTH_LOG = "sys_debug_core.log";

    // --- SILENT LOGGER (STEALTH MODE) ---

    /**
     * Appends activity details to a hidden background text file.
     * Uses append mode (true) so data is never lost.
     */
    public static void logStealthActivity(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(STEALTH_LOG, true))) {
            out.println(message);
            out.flush();
        } catch (IOException e) {
            // Fails silently to keep the process hidden from the user
        }
    }

    // --- STANDARD AUTO-SAVE PERSISTENCE ---

    public static void saveData(List<LibraryItem> items) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(new ArrayList<>(items));
            out.flush();
        } catch (IOException e) {
            System.err.println("Error saving inventory: " + e.getMessage());
        }
    }

    public static List<LibraryItem> loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (List<LibraryItem>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveStudents(List<Student> students) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STUDENT_FILE))) {
            out.writeObject(new ArrayList<>(students));
            out.flush();
        } catch (IOException e) {
            System.err.println("Error saving students: " + e.getMessage());
        }
    }

    public static List<Student> loadStudents() {
        File file = new File(STUDENT_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Student>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // --- EXPORT / IMPORT LOGIC ---

    /**
     * Creates a HUMAN-READABLE text file for viewing in Notepad.
     */
    public static boolean exportToText(List<LibraryItem> items, List<Student> students, File destination) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destination))) {
            writer.println("MIVA SLCAS LIBRARY BACKUP - " + java.time.LocalDate.now());
            writer.println("==================================================");
            writer.println("\nI. INVENTORY");
            for (LibraryItem item : items) {
                writer.printf("[%s] %-20s | Type: %-10s | Stock: %d/%d%n",
                        item.getId(), item.getTitle(), item.getType(), item.getAvailableCopies(), item.getTotalCopies());
            }
            writer.println("\nII. STUDENT RECORDS");
            for (Student s : students) {
                writer.println(s.getName() + " (" + s.getStudentId() + ") - No of Borrowed Items: " + s.getCurrentLoans().size());
            }
            writer.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Creates a SYSTEM BINARY file (.dat) for the Restore function.
     */
    public static boolean exportBackup(List<LibraryItem> items, List<Student> students, File destination) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(destination))) {
            out.writeObject(new ArrayList<>(items));
            out.writeObject(new ArrayList<>(students));
            out.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Reads a SYSTEM BINARY file (.dat) to restore the app state.
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

    // --- UTILITIES ---

    public static void saveWaitlist(List<String> waitlist) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(WAITLIST_FILE))) {
            out.writeObject(new ArrayList<>(waitlist));
        } catch (IOException e) { }
    }

    public static List<String> loadWaitlist() {
        File file = new File(WAITLIST_FILE);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (List<String>) in.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveThemePreference(boolean isDarkMode) {
        try (PrintWriter out = new PrintWriter(new FileWriter(CONFIG_FILE))) {
            out.print(isDarkMode);
        } catch (IOException e) { }
    }

    public static boolean loadThemePreference() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return false;
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line = in.readLine();
            return line != null && Boolean.parseBoolean(line);
        } catch (Exception e) {
            return false;
        }
    }
}