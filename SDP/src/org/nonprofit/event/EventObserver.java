package org.nonprofit.event;

/** OBSERVER pattern (observer role): notified when an {@link Event} it follows changes. */
public interface EventObserver {
    void update(Event event, EventChange change, String message);
}
