package model;

import java.io.Serializable;

/** Read-only DTO for one overdue loan. Returned by LibraryController.getOverdueLoans(). */
public record OverdueLoanView(
        String studentName, String studentId,
        String itemTitle, String itemType,
        String dueDate, long daysOverdue
) implements Serializable {}
