package controller;

import model.LibraryItem;
import model.UserAccount;

/**
 * BorrowController - Handles borrow/return workflows and waitlist fulfillment
 *
 * Centralizes the logic for checking out items, processing returns,
 * and managing the waitlist queue when items become available.
 */
public class BorrowController {

    private final LibraryManager manager;

    public enum BorrowStatus {
        SUCCESS, STUDENT_NOT_FOUND, ITEM_NOT_FOUND, ITEM_UNAVAILABLE, DUPLICATE_LOAN
    }

    public static class WaitlistResult {
        public final boolean success;
        public final String entry;
        public final UserAccount student;

        public WaitlistResult(boolean success, String entry, UserAccount student) {
            this.success = success;
            this.entry = entry;
            this.student = student;
        }
    }

    // Constructor - initialize with library manager
    public BorrowController(LibraryManager manager) {
        this.manager = manager;
    }

    // Process borrow request with validation
    public BorrowStatus processBorrow(String operatorUserId, String studentId, String itemId) {
        // Find item by ID
        LibraryItem item = manager.getItemById(itemId);
        if (item == null) return BorrowStatus.ITEM_NOT_FOUND;

        // Find student by ID
        UserAccount student = manager.findStudentById(studentId);
        if (student == null) return BorrowStatus.STUDENT_NOT_FOUND;

        // Check if item is available
        if (item.getAvailableCopies() <= 0) return BorrowStatus.ITEM_UNAVAILABLE;

        // Attempt to borrow item
        boolean success = manager.borrowItem(operatorUserId, student, item);
        return success ? BorrowStatus.SUCCESS : BorrowStatus.DUPLICATE_LOAN;
    }

    // Add student to item waitlist
    public void addToWaitlist(String operatorUserId, UserAccount student, LibraryItem item) {
        // Validate inputs
        if (student == null || item == null) return;
        manager.addToWaitlist(operatorUserId, student, item);
    }

    public String findFirstWaitlistEntryForItem(LibraryItem item) {
        if (item == null) return null;
        String target = "-> " + item.getTitle();
        return manager.getWaitlist().stream()
                .filter(entry -> entry.contains(target))
                .findFirst()
                .orElse(null);
    }

    public String extractStudentIdFromWaitlistEntry(String waitlistEntry) {
        if (waitlistEntry == null) return null;
        int openIdx = waitlistEntry.indexOf('(');
        int closeIdx = waitlistEntry.indexOf(')');
        if (openIdx == -1 || closeIdx == -1 || closeIdx <= openIdx + 1) return null;
        return waitlistEntry.substring(openIdx + 1, closeIdx);
    }

    public WaitlistResult fulfilFirstWaitlistEntry(String operatorUserId, LibraryItem item) {
        String entry = findFirstWaitlistEntryForItem(item);
        if (entry == null) return new WaitlistResult(false, null, null);

        String studentId = extractStudentIdFromWaitlistEntry(entry);
        if (studentId == null) return new WaitlistResult(false, entry, null);

        UserAccount s = manager.findStudentById(studentId);
        if (s == null) return new WaitlistResult(false, entry, null);

        boolean success = manager.borrowItem(operatorUserId, s, item);
        if (success) {
            int idx = 0;
            for (String e : manager.getWaitlist()) {
                if (e.equals(entry)) {
                    manager.removeWaitlistEntry(operatorUserId, idx);
                    break;
                }
                idx++;
            }
            return new WaitlistResult(true, entry, s);
        }
        return new WaitlistResult(false, entry, s);
    }
}

