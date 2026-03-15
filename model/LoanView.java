package model;

import java.io.Serializable;

/** Read-only DTO for one active loan row. Built by LibraryController.getActiveLoans(). */
public record LoanView(
        String studentName, String studentId, String itemId,
        String itemTitle, String itemType,
        String borrowDate, String dueDate,
        String status
) implements Serializable {}
