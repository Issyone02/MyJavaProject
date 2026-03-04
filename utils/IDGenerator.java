package utils;

import java.util.UUID;

public class IDGenerator {
    // Generates a short, unique 5-character ID
    public static String generateID() {
        // Use UUID for randomness, take first 5 characters, and convert to uppercase
        return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}