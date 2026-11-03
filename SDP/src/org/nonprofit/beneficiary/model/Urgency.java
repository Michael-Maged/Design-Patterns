package org.nonprofit.beneficiary.model;

import java.util.Locale;

/** How urgent a need is. Declared from least to most urgent, so HIGH compares greatest. */
public enum Urgency {
    LOW,
    MEDIUM,
    HIGH;

    /** Parses user input such as "high" or " Medium ". */
    public static Urgency fromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Unknown urgency: null");
        }
        try {
            return valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown urgency: " + input);
        }
    }
}
