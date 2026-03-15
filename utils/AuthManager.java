package utils;

import model.Staff;
import model.UserAccount;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Staff authentication and account management.
 * Stores a map of integer staff IDs → UserAccount objects (Admin or Librarian).
 * Passwords are always hashed — plaintext is never stored or compared directly.
 * Students are NOT system users; they're data records managed by LibraryManager.
 */
public class AuthManager {

    // The super admin account that cannot be deleted or demoted
    public static final int SUPER_ADMIN_ID = 30114413;

    private static final String USER_FILE = "system_users.dat";
    private static Map<Integer, UserAccount> users = new HashMap<>();

    // ── Load / Save ──────────────────────────────────────────────────────────

    // Call once at startup. Creates a default admin if no user file exists.
    public static void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            createDefaultAdmin();
            saveToDisk();
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object raw = ois.readObject();
            if (raw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Integer, UserAccount> loaded = (Map<Integer, UserAccount>) raw;
                users = loaded;
            }
        } catch (Exception e) {
            users = new HashMap<>();
            createDefaultAdmin();
            saveToDisk();
        }
    }

    private static synchronized void saveToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("Critical Error: Could not save user database.");
        }
    }

    private static void createDefaultAdmin() {
        users.put(SUPER_ADMIN_ID, new Staff(SUPER_ADMIN_ID, "System Owner", "Ol@l3r3", true));
    }

    // ── Login / Validation ───────────────────────────────────────────────────

    // Validates a plaintext password against the stored hash
    public static boolean validate(int id, String rawPassword) {
        UserAccount u = users.get(id);
        return u != null && u.checkPassword(rawPassword);
    }

    public static boolean isUserExists(int id) {
        return users.containsKey(id);
    }

    // ── Role checks ──────────────────────────────────────────────────────────

    public static boolean isAdmin(int id) {
        UserAccount u = users.get(id);
        return u != null && u.isAdmin();
    }

    // ── User info ────────────────────────────────────────────────────────────

    public static String getFullName(int id) {
        UserAccount u = users.get(id);
        return u != null ? u.getFullName() : "Unknown User";
    }
    public static String getRole(int id) {
        UserAccount u = users.get(id);
        return u != null ? u.getRole() : "Unknown";
    }

    // ── Account management ───────────────────────────────────────────────────

    // Creates or updates a user. Pass null for rawPass to keep the existing password.
    public static void addUser(int id, String rawPass, String fullName, boolean isAdminRole) {
        if (id == SUPER_ADMIN_ID) {
            // Super admin: only name and password can change, role stays Admin
            UserAccount existing = users.get(id);
            if (existing != null) {
                existing.setFullName(fullName);
                if (rawPass != null && !rawPass.isBlank()) {
                    existing.changePassword(rawPass);
                }
                saveToDisk();
                return;
            }
        }
        UserAccount account = new Staff(id, fullName, rawPass, isAdminRole);
        users.put(id, account);
        saveToDisk();
    }

    public static void changePassword(int id, String newRawPassword) {
        UserAccount u = users.get(id);
        if (u != null) {
            u.changePassword(newRawPassword);
            saveToDisk();
        }
    }

    // Admin resets another user's password (same logic, separate method for clarity)
    public static void resetUserPassword(int targetId, String newRawPassword) {
        changePassword(targetId, newRawPassword);
    }

    // The super admin can never be removed
    public static void removeUser(int id) {
        if (id == SUPER_ADMIN_ID) return;
        users.remove(id);
        saveToDisk();
    }

    public static Map<Integer, UserAccount> getAllUsers() {
        return users;
    }
}
