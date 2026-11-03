package org.nonprofit.event;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FACTORY pattern: the only place that decides which concrete {@link Event}
 * subclass to instantiate. Clients ask by {@link EventType} or by the raw
 * text a user typed, and get back an {@code Event} without knowing the
 * concrete class. Adding a new event kind = one subclass + one case here.
 */
public final class EventFactory {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private EventFactory() {
    }

    public static Event create(EventType type, String name, LocalDateTime dateTime, String location, int capacity) {
        String id = "EVT-" + NEXT_ID.getAndIncrement();
        return switch (type) {
            case FUNDRAISER -> new Fundraiser(id, name, dateTime, location, capacity);
            case WORKSHOP -> new Workshop(id, name, dateTime, location, capacity);
            case OUTREACH -> new OutreachProgram(id, name, dateTime, location, capacity);
        };
    }

    /** Creates an event from user input, e.g. {@code create("workshop", ...)}. */
    public static Event create(String typeInput, String name, LocalDateTime dateTime, String location, int capacity) {
        return create(EventType.fromString(typeInput), name, dateTime, location, capacity);
    }
}
