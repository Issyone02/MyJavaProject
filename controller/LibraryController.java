package controller;

import model.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/** Contract for all library operations. GUI panels hold this type — never the concrete LibraryManager. */
public interface LibraryController {
    // Observer
    void addChangeListener(LibraryChangeListener l);
    void removeChangeListener(LibraryChangeListener l);
    // Item CRUD
    String createItem(String type, String title, String author, int year, int total, String userId);
    void    addItem(LibraryItem item, String userId);
    boolean removeItem(String id, String userId, String reason);
    void    updateItem(String userId, String id, String type, String title, String author, int year, int total, String reason);
    LibraryItem getItemById(String id);
    // Import / export
    boolean importFromBackup(String userId, File source);
    boolean exportToText(String userId, File destination);
    boolean exportBinary(String userId, File destination);
    // Session log
    void logSession(String event, String details);
    // Borrow / return
    boolean borrowItem(String userId, UserAccount student, LibraryItem item);
    void    returnItem(String userId, UserAccount student, LibraryItem item);
    void    removeOrphanedLoan(String userId, UserAccount student, LibraryItem item);
    LibraryItem findLoanItemByTitle(String studentId, String itemTitle);
    // Student CRUD
    boolean createStudent(String id, String name, String userId);
    void    removeStudent(String id, String userId);
    void    updateStudent(String id, String newName, String userId);
    UserAccount findStudentById(String id);
    // Waitlist
    void    addToWaitlist(String userId, UserAccount student, LibraryItem item);
    void    moveWaitlistEntry(String userId, int index, boolean up);
    void    removeWaitlistEntry(String userId, int index);
    boolean fulfillWaitlistEntry(String userId, UserAccount student, LibraryItem item, int idx);
    // Sorting
    void insertionSortBy(String field);
    void mergeSortBy(String field);
    void quickSortBy(String field);
    void restoreOrder(List<String> orderedItemIds);
    // Undo / redo
    void saveState(boolean isUndoOrRedo);
    void undo(String userName);
    void redo(String userName);
    // Audit log
    void addLog(String userId, String action, String details);
    // Staff management
    void addOrUpdateStaff(String operatorId, int staffId, String rawPassword, String fullName, boolean isAdmin);
    void removeStaff(String operatorId, int staffId);
    void resetStaffPassword(String operatorId, int targetId, String newPassword);
    Map<Integer, UserAccount> getAllStaff();
    boolean staffExists(int staffId);
    // Auth helpers
    boolean validatePassword(int userId, String rawPassword);
    boolean isAdminUser(int userId);
    String  getStaffFullName(int userId);
    int     getSuperAdminId();
    // DTO queries
    List<LoanView>        getActiveLoans();
    List<OverdueLoanView> getOverdueLoans();
    long                  getOverdueCount();
    BorrowSummary         getBorrowSummary();
    List<StudentSummary>  getStudentSummaries();
    // Raw accessors
    List<LibraryItem> getInventory();
    Queue<String>     getWaitlist();
    List<SystemLog>   getSystemLogs();
    List<LibraryItem> getMostAccessedItems();
    int               getCacheAccessCount(int cacheIndex);
}
