package model;

/**
 * Represents one reservation queue entry: "Name (ID) -> ItemTitle".
 * Owns both formatting and parsing of that string.
 */
public record WaitlistEntry(String studentName, String studentId, String itemTitle) {

    /** Formats this entry into the canonical queue string. */
    public String format() { return studentName + " (" + studentId + ") -> " + itemTitle; }

    /** Parses a raw queue string. Returns null if the format is unrecognised. */
    public static WaitlistEntry parse(String raw) {
        if (raw == null) return null;
        int arrow = raw.indexOf(" -> ");
        if (arrow < 0) return null;
        String left  = raw.substring(0, arrow);
        String title = raw.substring(arrow + 4).trim();
        int pOpen = left.lastIndexOf('('), pClose = left.lastIndexOf(')');
        if (pOpen < 0 || pClose <= pOpen) return null;
        return new WaitlistEntry(left.substring(0, pOpen).trim(),
                                 left.substring(pOpen + 1, pClose).trim(), title);
    }
}
