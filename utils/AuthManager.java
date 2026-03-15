package utils;

import controller.AuthController;
import model.UserAccount;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/** Staff authentication and account management. Singleton. */
public class AuthManager implements AuthController {

    public static final int SUPER_ADMIN_ID = 30114413;

    private static final String USER_FILE = "system_users.dat";
    private static Map<Integer, UserAccount> users = new HashMap<>();
    private static final AuthManager INSTANCE = new AuthManager();

    private AuthManager() {}

    public static AuthManager getInstance() { return INSTANCE; }

    // ── AuthController ────────────────────────────────────────────────────────

    @Override
    public boolean validate(int userId, String rawPassword) {
        UserAccount u = users.get(userId);
        return u != null && u.checkPassword(rawPassword);
    }

    @Override
    public String getFullName(int userId) {
        UserAccount u = users.get(userId);
        return u != null ? u.getFullName() : "Unknown User";
    }

    @Override
    public void logFailedAttempt(String attemptedId) {
        FileHandler.logStealthActivity("FAILED LOGIN ATTEMPT for ID: " + attemptedId);
    }

    // ── Startup ───────────────────────────────────────────────────────────────

    /** Call once at startup. Creates a default admin if no user file exists. */
    public static void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) { createDefaultAdmin(); saveToDisk(); return; }
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

    public static synchronized void saveToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("Critical Error: Could not save user database.");
        }
    }

    private static void createDefaultAdmin() {
        users.put(SUPER_ADMIN_ID, new UserAccount(String.valueOf(SUPER_ADMIN_ID), "System Owner", "Ol@l3r3", "Admin"));
    }

    // ── Staff CRUD (called only by LibraryManager) ────────────────────────────

    public static boolean validateStatic(int id, String rawPassword) {
        UserAccount u = users.get(id);
        return u != null && u.checkPassword(rawPassword);
    }

    public static boolean isUserExists(int id)  { return users.containsKey(id); }
    public static boolean isAdmin(int id)        { UserAccount u = users.get(id); return u != null && u.isAdmin(); }

    /** Creates or updates a staff account. Pass null for rawPass to keep the existing password. */
    public static void addUser(int id, String rawPass, String fullName, boolean isAdminRole) {
        if (id == SUPER_ADMIN_ID) {
            UserAccount existing = users.get(id);
            if (existing != null) {
                existing.setFullName(fullName);
                if (rawPass != null && !rawPass.isBlank()) existing.changePassword(rawPass);
                saveToDisk();
                return;
            }
        }
        String role = isAdminRole ? "Admin" : "Librarian";
        users.put(id, new UserAccount(String.valueOf(id), fullName, rawPass, role));
        saveToDisk();
    }

    public static void resetUserPassword(int targetId, String newRawPassword) {
        UserAccount u = users.get(targetId);
        if (u != null) { u.changePassword(newRawPassword); saveToDisk(); }
    }

    public static Map<Integer, UserAccount> getAllUsers() { return users; }

    public static void removeUser(int id) {
        if (id == SUPER_ADMIN_ID) return;
        users.remove(id);
        saveToDisk();
    }
}
