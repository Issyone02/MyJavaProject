package model;

import java.io.Serializable;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Base class for all users (Student and Staff). Holds identity and borrow history. */
public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accountId, fullName;
    private String passwordHash; // null for students
    private String role = "Student"; // "Student", "Admin", "Librarian"

    private List<BorrowRecord> currentLoans = new ArrayList<>();
    private List<BorrowRecord> history      = new ArrayList<>();

    public UserAccount() {}
    /** Creates a student account with ID and name. */
    public UserAccount(String accountId, String fullName) { this.accountId = accountId; this.fullName = fullName; }
    /** Creates a staff account with ID, name, and password. */
    public UserAccount(String accountId, String fullName, String rawPassword) {
        this.accountId = accountId; this.fullName = fullName; this.passwordHash = hashPassword(rawPassword);
    }
    /** Creates a staff account with ID, name, password, and role. */
    public UserAccount(String accountId, String fullName, String rawPassword, String role) {
        this.accountId = accountId; this.fullName = fullName; this.passwordHash = hashPassword(rawPassword); this.role = role;
    }

    /** Deep copy constructor for undo/redo snapshots. Clones all borrow records. */
    public UserAccount(UserAccount other) {
        if (other == null) return;
        this.accountId = other.accountId; this.fullName = other.fullName; this.passwordHash = other.passwordHash; this.role = other.role;
        for (BorrowRecord r : other.currentLoans) this.currentLoans.add(new BorrowRecord(r));
        for (BorrowRecord r : other.history)      this.history.add(new BorrowRecord(r));
    }

    /** Returns user role (Student, Admin, Librarian). */
    public String  getRole()  { return role; }
    
    /** Checks if user has admin privileges. */
    public boolean isAdmin()  { return "Admin".equals(role); }

    public String getStudentId() { return accountId; }
    public String getName()      { return fullName; }
    public void   setName(String name) { setFullName(name); }

    /** Adds a new borrowed item to current loans with today's date. */
    public void addBorrowedItem(LibraryItem item) { currentLoans.add(new BorrowRecord(item)); }

    /** Returns a borrowed item to history. Returns false if item not found in current loans. */
    public boolean returnItem(LibraryItem item) {
        for (BorrowRecord r : currentLoans) {
            if (r.getItem().getId().equals(item.getId())) {
                r.setReturnDate(LocalDate.now());
                currentLoans.remove(r);
                history.add(r);
                return true;
            }
        }
        return false;
    }

    public List<BorrowRecord> getCurrentLoans() { return currentLoans; }
    public List<BorrowRecord> getHistory()      { return history; }

    /** Validates password against stored hash. Returns false for students (no password). */
    public boolean checkPassword(String raw) {
        return passwordHash != null && raw != null && passwordHash.equals(hashPassword(raw));
    }
    /** Changes user password with validation. Throws exception for empty password. */
    public void changePassword(String newRaw) {
        if (newRaw == null || newRaw.isBlank()) throw new IllegalArgumentException("Password cannot be empty.");
        this.passwordHash = hashPassword(newRaw);
    }
    /** Hashes password using SHA-256 with Base64 encoding. Returns raw string on error. */
    public static String hashPassword(String raw) {
        if (raw == null) return null;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) { return raw; }
    }

    public String getAccountId()          { return accountId; }
    public void   setAccountId(String id) { this.accountId = id; }
    public String getFullName()           { return fullName; }
    /** Sets user full name with validation. Throws exception for empty name. */
    public void   setFullName(String n)   {
        if (n == null || n.isBlank()) throw new IllegalArgumentException("Name cannot be empty.");
        this.fullName = n;
    }
    public String getPasswordHash()           { return passwordHash; }
    public void   setPasswordHash(String h)   { this.passwordHash = h; }

    @Override public String toString() { return fullName + " [" + getRole() + " #" + accountId + "]"; }
}
