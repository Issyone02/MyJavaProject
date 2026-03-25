package utils;

import java.util.UUID;

/** Generates unique 5-character uppercase IDs for new catalogue items. */
public class IDGenerator {
    public static String generateID() {
        return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}
