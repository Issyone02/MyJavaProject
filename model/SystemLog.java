package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Immutable audit log entry. */
public class SystemLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String userId, timestamp, action, details;

    /** Creates a system log entry with current timestamp. */
    public SystemLog(String userId, String action, String details) {
        this.userId    = userId; this.action = action; this.details = details;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /** Returns the user ID who performed the action. */
    public String getUserId()    { return userId; }
    
    /** Returns the timestamp when the log entry was created. */
    public String getTimestamp() { return timestamp; }
    
    /** Returns the action type (e.g., BORROW, RETURN, ADD, DELETE). */
    public String getAction()    { return action; }
    
    /** Returns detailed description of the action. */
    public String getDetails()   { return details; }
}
