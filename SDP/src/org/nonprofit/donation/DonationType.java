package org.nonprofit.donation;

import java.util.Locale;

/** The three ways someone can donate to the organization. */
public enum DonationType {
    ONLINE,
    CHECK,
    IN_KIND;

    /**
     * Turns what the user typed into a donation type.
     * Accepts things like "online", "Check", "cheque", "in-kind" or "in kind".
     */
    public static DonationType fromText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Donation type is required");
        }
        String clean = text.trim().toLowerCase(Locale.ROOT);
        switch (clean) {
            case "online":
                return ONLINE;
            case "check":
            case "cheque":
                return CHECK;
            case "in-kind":
            case "in kind":
            case "inkind":
            case "goods":
                return IN_KIND;
            default:
                throw new IllegalArgumentException("Unknown donation type: " + text + " (use online, check or in-kind)");
        }
    }
}
