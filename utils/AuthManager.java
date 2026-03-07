package utils;

import java.util.HashMap;
import java.util.Map;
import java.io.*;

/**
 * AuthManager - Handles user authentication and authorization
 *
 * Manages:
 * - User accounts (login credentials)
 * - User roles (Admin vs Staff)
 * - Password validation
 * - User database persistence
 *
 * Default admin account: ID=30114413, Password="Ol@l3r3"
 */
public class AuthManager {

    /**
     * User - Represents a system user account
     */
    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        public String password;    // User password
        public String nickname;    // Display name
        public boolean isSuper;    // Admin privileges flag

        public User(String password, String nickname, boolean isSuper) {
            this.password = password;
            this.nickname = nickname;
            this.isSuper = isSuper;
        }
    }

    private static Map<Integer, User> users = new HashMap<>();  // User database
    public static final int SUPER_ADMIN_ID = 30114413;          // System owner ID
    private static final String USER_FILE = "system_users.dat"; // Storage file

    /**
     * Loads user accounts from disk
     * Creates default admin if no users exist
     */
    public static void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            // Create default admin account
            users.put(SUPER_ADMIN_ID, new User("Ol@l3r3", "System Owner", true));
            saveToDisk();
            return;
        }

        // Load existing users
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object raw = ois.readObject();
            if (raw instanceof Map) {
                users = (Map<Integer, User>) raw;
            }
        } catch (Exception e) {
            // Fallback to default admin
            users.put(SUPER_ADMIN_ID, new User("Ol@l3r3", "System Owner", true));
        }
    }

    /**
     * Saves user database to disk
     */
    private static synchronized void saveToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("Critical Error: Could not save user database.");
        }
    }

    /**
     * Updates a user's role (Admin/Staff)
     * System owner role cannot be changed
     */
    public static void updateUserRole(int id, boolean isSuper) {
        if (id == SUPER_ADMIN_ID) return;  // Protect system owner
        if (users.containsKey(id)) {
            users.get(id).isSuper = isSuper;
            saveToDisk();
        }
    }

    /**
     * Validates login credentials
     * @return true if ID and password match
     */
    public static boolean validate(int id, String password) {
        return users.containsKey(id) && users.get(id).password.equals(password);
    }

    /**
     * Checks if user ID exists
     */
    public static boolean isUserExists(int id) {
        return users.containsKey(id);
    }

    /**
     * Gets user's display name
     */
    public static String getNickname(int id) {
        return users.containsKey(id) ? users.get(id).nickname : "Unknown User";
    }

    /**
     * Gets full user details with role
     */
    public static String getUserFullDetails(int id) {
        if (!users.containsKey(id)) return "Unknown";
        User u = users.get(id);
        return u.nickname + " (" + (u.isSuper ? "Admin" : "Librarian") + ")";
    }

    /**
     * Checks if user has admin privileges
     */
    public static boolean isSuperAdmin(int id) {
        return users.containsKey(id) && users.get(id).isSuper;
    }

    /**
     * Adds a new user account
     */
    public static void addUser(int id, String password, String nickname, boolean isSuper) {
        users.put(id, new User(password, nickname, isSuper));
        saveToDisk();
    }

    /**
     * Changes a user's password
     */
    public static void changePassword(int id, String newPassword) {
        if (users.containsKey(id)) {
            users.get(id).password = newPassword;
            saveToDisk();
        }
    }

    /**
     * Resets a user's password to a specific value
     * Used by System Owner for recovery
     */
    public static void resetUserPassword(int targetId, String newPassword) {
        if (users.containsKey(targetId)) {
            users.get(targetId).password = newPassword;
            saveToDisk();
        }
    }

    /**
     * Deletes a user account
     * System owner cannot be deleted
     */
    public static void removeUser(int id) {
        if (id == SUPER_ADMIN_ID) return;  // Protect system owner
        users.remove(id);
        saveToDisk();
    }

    /**
     * Gets all users (for user management interface)
     */
    public static Map<Integer, User> getAllUsers() {
        return users;
    }
}