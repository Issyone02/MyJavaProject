package model;

/**
 * Represents a staff member (Admin or Librarian).
 * Role is determined by the isAdmin flag — no separate subclasses needed.
 */
public class Staff extends UserAccount {
    private static final long serialVersionUID = 1L;

    private boolean admin;

    public Staff() {}

    public Staff(int userId, String fullName, String rawPassword, boolean isAdmin) {
        super(String.valueOf(userId), fullName, rawPassword);
        this.admin = isAdmin;
    }

    // Deep copy constructor — used by undo/redo snapshots
    public Staff(Staff other) {
        super(other);
        this.admin = other.admin;
    }

    @Override public String  getRole()  { return admin ? "Admin" : "Librarian"; }
    @Override public boolean isAdmin()  { return admin; }

    public int  getUserId()       { return Integer.parseInt(getAccountId()); }
    public void setUserId(int id) { setAccountId(String.valueOf(id)); }
}
