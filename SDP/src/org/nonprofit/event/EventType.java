package org.nonprofit.event;

import java.util.Locale;

/** The kinds of events the {@link EventFactory} (SIMPLE FACTORY) knows how to build; the client passes one to choose the product. */
public enum EventType {
    FUNDRAISER,
    WORKSHOP,
    OUTREACH;

    /** Parses user input such as "workshop", " Fundraiser " or "outreach program". */
    public static EventType fromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        String key = input.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
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
