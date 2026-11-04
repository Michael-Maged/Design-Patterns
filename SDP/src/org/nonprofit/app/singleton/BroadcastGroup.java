package org.nonprofit.app.singleton;

import java.util.Locale;

/** The groups a manual broadcast can go to (see {@link DataStore#membersOf}). */
public enum BroadcastGroup {
    DONORS,
    VOLUNTEERS,
    BENEFICIARIES,
    ATTENDEES;

    /** Parses user input such as "donors" or " Volunteers ". */
    public static BroadcastGroup fromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Group is required");
        }
        try {
            return valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown group: " + input
                    + " (use donors, volunteers, beneficiaries or attendees)");
        }
    }
}
