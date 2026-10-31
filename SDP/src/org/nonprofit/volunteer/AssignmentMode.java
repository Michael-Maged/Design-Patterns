package org.nonprofit.volunteer;

import java.util.Locale;

/** The assignment strategies a user can pick from a menu. */
public enum AssignmentMode {
    SKILL,
    AVAILABILITY,
    PREFERENCE;

    /** Makes a new strategy object for this menu choice. */
    public AssignmentStrategy create() {
        switch (this) {
            case SKILL:
                return new SkillBasedAssignment();
            case AVAILABILITY:
                return new AvailabilityBasedAssignment();
            default:
                return new PreferenceBasedAssignment();
        }
    }

    /** Parses user input such as "skill" or " Preference ". */
    public static AssignmentMode fromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Unknown assignment mode: null");
        }
        try {
            return valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown assignment mode: " + input);
        }
    }
}
