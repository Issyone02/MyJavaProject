package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SystemLog - Records audit trail entry
 *
 * Captures who did what and when for accountability.
 * Used for system audit log and security tracking.
 */
public class SystemLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userId;      // Who performed the action
    private final String timestamp;   // When it happened
    private final String action;      // What action (BORROW, RETURN, DELETE, etc.)
    private final String details;     // Additional details

    /**
     * Creates a new log entry with current timestamp
     *
     * @param userId  User performing the action
     * @param action  Action type
     * @param details Additional information
     */
    public SystemLog(String userId, String action, String details) {
        this.userId = userId;
        this.action = action;
        this.details = details;
        // Capture exact moment with formatted timestamp
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Getters (no setters - immutable log entry)
    public String getUserId() { return userId; }
    public String getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}