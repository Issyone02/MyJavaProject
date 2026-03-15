package model;

import java.io.Serializable;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Abstract base class for ALL users in the system (Student, Admin, Librarian).
 *
 * Holds identity fields shared by every user type:
 *   - accountId   — unique identifier (e.g. "STU001" for students, "30114413" for staff)
 *   - fullName    — display name
 *
 * Holds borrowing history (satisfies requirement: "UserAccount class with borrowing history"):
 *   - currentLoans — items currently checked out
 *   - history      — completed (returned) loans
 *
 * Password support is optional — students don't log in so their passwordHash is null.
 * Admin and Librarian set a password via the constructor.
 */
public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accountId;
    private String fullName;
    private String passwordHash;   // null for students (they don't log in)

    // Borrowing history — shared by all user types
    private List<BorrowRecord> currentLoans = new ArrayList<>();
    private List<BorrowRecord> history      = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────────────────

    public UserAccount() {}

    // For students (no password)
    public UserAccount(String accountId, String fullName) {
        this.accountId = accountId;
        this.fullName  = fullName;
    }

    // For staff (with password)
    public UserAccount(String accountId, String fullName, String rawPassword) {
        this.accountId    = accountId;
        this.fullName     = fullName;
        this.passwordHash = hashPassword(rawPassword);
    }

    // Deep copy constructor — used by undo/redo snapshots
    public UserAccount(UserAccount other) {
        if (other == null) return;
        this.accountId    = other.accountId;
        this.fullName     = other.fullName;
        this.passwordHash = other.passwordHash;
        for (BorrowRecord r : other.currentLoans) this.currentLoans.add(new BorrowRecord(r));
        for (BorrowRecord r : other.history)      this.history.add(new BorrowRecord(r));
    }

    // ── Role ─────────────────────────────────────────────────────────────────

    public String getRole()  { return "Student"; } 
    public boolean isAdmin() { return false; }       

    // ── Student convenience aliases ───────────────────────────────────────────

    public String getStudentId()            { return getAccountId(); }
    public void   setStudentId(String id)   { setAccountId(id); }
    public String getName()                 { return getFullName(); }
    public void   setName(String name)      { setFullName(name); }

    // ── Borrowing operations ─────────────────────────────────────────────────

    public void addBorrowedItem(LibraryItem item) {
        currentLoans.add(new BorrowRecord(item));
    }

    // Returns the item and moves the record to history. Returns false if not found.
    public boolean returnItem(LibraryItem item) {
        for (BorrowRecord record : currentLoans) {
            if (record.getItem().getId().equals(item.getId())) {
                record.setReturnDate(LocalDate.now());
                currentLoans.remove(record);
                history.add(record);
                return true;
            }
        }
        return false;
    }

public List<BorrowRecord> getCurrentLoans() { return currentLoans; }
    public List<BorrowRecord> getHistory()      { return history; }

    // ── Password (optional — null for students) ──────────────────────────────

    public boolean checkPassword(String rawPassword) {
        if (passwordHash == null || rawPassword == null) return false;
        return passwordHash.equals(hashPassword(rawPassword));
    }

    public void changePassword(String newRawPassword) {
        if (newRawPassword == null || newRawPassword.isBlank())
            throw new IllegalArgumentException("Password cannot be empty.");
        this.passwordHash = hashPassword(newRawPassword);
    }

    public static String hashPassword(String raw) {
        if (raw == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return raw;
        }
    }

    // ── Identity getters/setters ─────────────────────────────────────────────

    public String getAccountId()              { return accountId; }
    public void   setAccountId(String id)     { this.accountId = id; }

    public String getFullName()               { return fullName; }
    public void   setFullName(String n)       {
        if (n == null || n.isBlank()) throw new IllegalArgumentException("Name cannot be empty.");
        this.fullName = n;
    }

    public String getPasswordHash()           { return passwordHash; }
    public void   setPasswordHash(String h)   { this.passwordHash = h; }

    @Override
    public String toString() {
        return fullName + " [" + getRole() + " #" + accountId + "]";
    }
}
