package model;

import java.io.Serializable;

/**
 * Read-only DTO for one student table row.
 * hasOverdue lets the cell renderer apply red/bold without any date arithmetic in the View.
 */
public record StudentSummary(
        String studentId, String name, int loanCount,
        String itemTitles, String dueDates,
        boolean hasOverdue
) implements Serializable {}
