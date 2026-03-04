package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemLog implements Serializable {
    // Who triggered this — either an admin's ID or "SYSTEM" for automated actions
    private final String userId;
    // The exact date and time this log entry was created
    private final String timestamp;
    // A short label for what happened, e.g. BORROW, RETURN, STOCK_CHANGE
    private final String action;
    // A fuller description giving more context about the action
    private final String details;

    public SystemLog(String userId, String action, String details) {
        this.userId = userId;
        this.action = action;
        this.details = details;

        /**
         * Automatically captures the current time and formats it into
         * something readable like "2026-03-04 14:35:22"
         */
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * All fields are final, so these getters are the only way to read the log data —
     * nothing can be changed after creation
     */
    public String getUserId() { return userId; }
    public String getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}