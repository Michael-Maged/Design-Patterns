package org.nonprofit.event;

/** The kinds of events the {@link EventFactory} knows how to build. */
public enum EventType {
    FUNDRAISER,
    WORKSHOP,
    OUTREACH;

    /** Parses user input such as "workshop", " Fundraiser " or "outreach program". */
    public static EventType fromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        String key = input.trim().toUpperCase().replace(' ', '_');
        if (key.equals("OUTREACH_PROGRAM")) {
            return OUTREACH;
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown event type: " + input);
        }
    }
}
