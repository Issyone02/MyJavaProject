package utils;

import java.util.HashMap;
import java.util.Map;
import java.io.*;

public class AuthManager {

    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;
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
    public static final int SUPER_ADMIN_ID = 30114413;
    private static final String USER_FILE = "system_users.dat";

    public static void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            users.put(SUPER_ADMIN_ID, new User("Ol@l3r3", "System Owner", true));
            saveToDisk();
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object raw = ois.readObject();
            if (raw instanceof Map) {
                users = (Map<Integer, User>) raw;
            }
        } catch (Exception e) {
            users.put(SUPER_ADMIN_ID, new User("Ol@l3r3", "System Owner", true));
        }
    }

    private static synchronized void saveToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("Critical Error: Could not save user database.");
        }
    }

    // --- NEW METHOD TO UPDATE ROLE ---
    public static void updateUserRole(int id, boolean isSuper) {
        if (id == SUPER_ADMIN_ID) return; // Protect system owner
        if (users.containsKey(id)) {
            users.get(id).isSuper = isSuper;
            saveToDisk();
        }
    }

    public static boolean validate(int id, String password) {
        return users.containsKey(id) && users.get(id).password.equals(password);
    }

    public static boolean isUserExists(int id) {
        return users.containsKey(id);
    }

    public static String getNickname(int id) {
        return users.containsKey(id) ? users.get(id).nickname : "Unknown User";
    }

    public static String getUserFullDetails(int id) {
        if (!users.containsKey(id)) return "Unknown";
        User u = users.get(id);
        return u.nickname + " (" + (u.isSuper ? "Admin" : "Librarian") + ")";
    }

    public static boolean isSuperAdmin(int id) {
        return users.containsKey(id) && users.get(id).isSuper;
    }

    public static Map<Integer, User> getAllUsers() {
        return new HashMap<>(users);
    }

    public static void addUser(int id, String password, String nickname, boolean isSuper) {
        users.put(id, new User(password, nickname, isSuper));
        saveToDisk();
    }

    public static void removeUser(int id) {
        if (id != SUPER_ADMIN_ID) {
            users.remove(id);
            saveToDisk();
        }
    }
}