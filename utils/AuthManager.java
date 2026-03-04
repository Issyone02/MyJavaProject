package utils;

import java.util.HashMap;
import java.util.Map;
import java.io.*;

/**
 * Handles security, user profiles, and persistent storage of credentials.
 */
public class AuthManager {

    /**
     * Inner class to encapsulate user profile data.
     * Marked Serializable so it can be written to "system_users.dat".
     */
    public static class User implements Serializable {
        public String password;
        public String nickname;
        public boolean isSuper;

        public User(String password, String nickname, boolean isSuper) {
            this.password = password;
            this.nickname = nickname;
            this.isSuper = isSuper;
        }
    }

    private static Map<Integer, User> users = new HashMap<>();
    // The "Root" account that can never be deleted
    public static final int SUPER_ADMIN_ID = 30114413;
    private static final String USER_FILE = "system_users.dat";

    /**
     * Initializes the user database.
     * Includes a migration layer to handle legacy data formats.
     */
    public static void loadUsers() {
        File file = new File(USER_FILE);

        // Bootstrapping: Create the first Super Admin if no file exists
        if (!file.exists()) {
            users.put(SUPER_ADMIN_ID, new User("Ol@l3r3", "System Owner", true));
            saveToDisk();
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object raw = ois.readObject();

            // MIGRATION LOGIC:
            // Checks if the file contains the old Map<Integer, String> format.
            // If so, it converts them into the new 'User' objects.
            if (raw instanceof Map && !((Map<?, ?>) raw).isEmpty() && ((Map<?, ?>) raw).values().iterator().next() instanceof String) {
                Map<Integer, String> oldData = (Map<Integer, String>) raw;
                for (Map.Entry<Integer, String> entry : oldData.entrySet()) {
                    boolean isS = entry.getKey() == SUPER_ADMIN_ID;
                    users.put(entry.getKey(), new User(entry.getValue(), isS ? "System Owner" : "Staff", isS));
                }
                saveToDisk(); // Save the new format immediately
            } else {
                users = (Map<Integer, User>) raw;
            }
        } catch (Exception e) {
            // Safety fallback: if the file is corrupted, restore the Super Admin
            users.put(SUPER_ADMIN_ID, new User("Ol@l3r3", "System Owner", true));
        }
    }

    /**
     * Commits the current user map to the local disk.
     */
    private static void saveToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks credentials during Login.
     */
    public static boolean validate(int id, String password) {
        return users.containsKey(id) && users.get(id).password.equals(password);
    }

    public static String getNickname(int id) {
        return users.containsKey(id) ? users.get(id).nickname : "Unknown User";
    }

    public static boolean isSuperAdmin(int id) {
        return id == SUPER_ADMIN_ID;
    }

    /**
     * Used by UserManagementDialog to populate the staff table.
     */
    public static Map<Integer, User> getAllUsers() {
        return new HashMap<>(users); // Return a copy to protect the original map
    }

    public static void addUser(int id, String password, String nickname) {
        users.put(id, new User(password, nickname, id == SUPER_ADMIN_ID));
        saveToDisk();
    }

    public static void removeUser(int id) {
        // Prevent accidental lockout by protecting the Super Admin
        if (id != SUPER_ADMIN_ID) {
            users.remove(id);
            saveToDisk();
        }
    }
}