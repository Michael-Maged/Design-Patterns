package org.nonprofit.beneficiary.strategy;

import java.util.Locale;

/** The allocation strategies a user can pick from a menu. */
public enum AllocationMode {
    URGENCY,
    FIRST_COME,
    EQUAL;

    /** Makes a new strategy object for this menu choice. */
    public AllocationStrategy create() {
        switch (this) {
            case URGENCY:
                return new UrgencyFirstAllocation();
            case FIRST_COME:
                return new FirstComeFirstServedAllocation();
            default:
                return new EqualShareAllocation();
        }
    }

    /** Parses user input such as "urgency", "first come" or " Equal ". */
    public static AllocationMode fromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Unknown allocation mode: null");
        }
        try {
            return valueOf(input.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown allocation mode: " + input);
        }
    }
}
