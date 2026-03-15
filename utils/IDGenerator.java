package utils;

import java.util.UUID;

/**
 * Generates unique 5-character IDs for library items.
 */
public class IDGenerator {
    // Generate random 5-character uppercase ID
    public static String generateID() {
        return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}