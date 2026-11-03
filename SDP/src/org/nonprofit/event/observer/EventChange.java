package org.nonprofit.event.observer;

/** What happened to an event; passed to every {@link EventObserver}. */
public enum EventChange {
    RESCHEDULED,
    RELOCATED,
    CANCELLED,
    REMINDER,
    WAITLIST_PROMOTED
}
