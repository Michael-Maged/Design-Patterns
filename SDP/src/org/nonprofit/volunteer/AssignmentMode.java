package org.nonprofit.volunteer;

import java.util.Locale;
import java.util.function.Supplier;

/** The assignment strategies a user can pick from a menu; each knows how to build its strategy. */
public enum AssignmentMode {
    SKILL(SkillBasedAssignment::new),
    AVAILABILITY(AvailabilityBasedAssignment::new),
    PREFERENCE(PreferenceBasedAssignment::new);

    private final Supplier<AssignmentStrategy> creator;

    AssignmentMode(Supplier<AssignmentStrategy> creator) {
        this.creator = creator;
    }

    public AssignmentStrategy create() {
        return creator.get();
    }

    /** Parses user input such as "skill" or " Preference ". */
    public static AssignmentMode fromString(String input) {
        try {
            return valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown assignment mode: " + input);
        }
    }
}
