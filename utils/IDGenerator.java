package utils;

import java.util.UUID;

/**
 * IDGenerator - Generates unique IDs for library items
 *
 * Uses UUID (Universally Unique Identifier) to ensure no duplicates.
 * Produces short, readable 5-character IDs like "A3F9E" or "B2C4D".
 */
public class IDGenerator {
    /**
     * Generates a unique 5-character ID
     *
     * @return Uppercase 5-character unique string (e.g., "F3A9B")
     */
    public static String generateID() {
        // Use UUID for guaranteed uniqueness, take first 5 chars, uppercase
        return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}