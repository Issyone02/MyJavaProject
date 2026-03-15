package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable audit log entry for tracking system actions.
 * Records who did what and when.
 */
public class SystemLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String timestamp;
    private final String action;
    private final String details;

    // Auto-generates timestamp when log is created
    public SystemLog(String userId, String action, String details) {
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getUserId() { return userId; }
    public String getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}