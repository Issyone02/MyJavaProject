package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemLog implements Serializable {
    private final String userId; // Can be Admin ID or "SYSTEM"
    private final String timestamp;
    private final String action; // BORROW, RETURN, STOCK_CHANGE, etc.
    private final String details;

    public SystemLog(String userId, String action, String details) {
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getUserId() { return userId; }
    public String getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}