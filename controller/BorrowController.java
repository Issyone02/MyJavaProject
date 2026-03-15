package controller;

import model.LibraryItem;
import model.UserAccount;
import model.WaitlistEntry;

/** Handles borrow/return workflows and waitlist fulfilment. */
public class BorrowController {

    private final LibraryController controller;

    /** Status codes for borrow operations. */
    public enum BorrowStatus {
        SUCCESS, STUDENT_NOT_FOUND, ITEM_NOT_FOUND, ITEM_UNAVAILABLE, DUPLICATE_LOAN
    }

    /** Result object for waitlist fulfilment operations. */
    public static class WaitlistResult {
        public final boolean     success;
        public final String      entry;
        public final UserAccount student;
        
        /** Creates waitlist result with success status, entry, and student. */
        public WaitlistResult(boolean success, String entry, UserAccount student) {
            this.success = success; this.entry = entry; this.student = student;
        }
    }

    /** Creates borrow controller with library controller for delegation. */
    public BorrowController(LibraryController controller) {
        this.controller = controller;
    }

    /** Processes borrow request with validation and delegation to controller. */
    public BorrowStatus processBorrow(String operatorId, String studentId, String itemId) {
        LibraryItem item = controller.getItemById(itemId);
        if (item == null) return BorrowStatus.ITEM_NOT_FOUND;
        UserAccount student = controller.findStudentById(studentId);
        if (student == null) return BorrowStatus.STUDENT_NOT_FOUND;
        if (item.getAvailableCopies() <= 0) return BorrowStatus.ITEM_UNAVAILABLE;
        return controller.borrowItem(operatorId, student, item)
                ? BorrowStatus.SUCCESS : BorrowStatus.DUPLICATE_LOAN;
    }

    /** Adds student to waitlist for unavailable item. */
    public void addToWaitlist(String operatorId, UserAccount student, LibraryItem item) {
        if (student != null && item != null) controller.addToWaitlist(operatorId, student, item);
    }

    /**
     * Finds the first waitlist entry for the item, borrows it for that student,
     * and removes the entry — called automatically after a successful return.
     * 
     * @param operatorId ID of staff performing the operation
     * @param item Library item to fulfil from waitlist
     * @return WaitlistResult with success status, entry string, and student account
     */
    public WaitlistResult fulfilFirstWaitlistEntry(String operatorId, LibraryItem item) {
        if (item == null) return new WaitlistResult(false, null, null);

        String raw = controller.getWaitlist().stream()
                .filter(e -> { WaitlistEntry we = WaitlistEntry.parse(e); return we != null && we.itemTitle().equalsIgnoreCase(item.getTitle()); })
                .findFirst().orElse(null);
        if (raw == null) return new WaitlistResult(false, null, null);

        WaitlistEntry entry = WaitlistEntry.parse(raw);
        if (entry == null) return new WaitlistResult(false, raw, null);

        UserAccount s = controller.findStudentById(entry.studentId());
        if (s == null) return new WaitlistResult(false, raw, null);

        if (controller.borrowItem(operatorId, s, item)) {
            int idx = 0;
            for (String e : controller.getWaitlist()) {
                if (e.equals(raw)) { controller.removeWaitlistEntry(operatorId, idx); break; }
                idx++;
            }
            return new WaitlistResult(true, raw, s);
        }
        return new WaitlistResult(false, raw, s);
    }
}
