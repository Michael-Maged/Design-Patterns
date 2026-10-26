package org.nonprofit.donation;

import java.util.Locale;

/** The donation methods the {@link DonationFactory} knows how to build. */
public enum DonationType {
    ONLINE,
    CHECK,
    IN_KIND;

    /** Parses user input such as "online", " Check " or "in-kind". */
    public static DonationType fromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Donation type is required");
        }
        String key = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (key.equals("INKIND")) {
            return IN_KIND;
        }
        if (key.equals("CHEQUE")) {
            return CHECK;
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown donation type: " + input);
        }
    }
}
