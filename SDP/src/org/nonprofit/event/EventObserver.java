package org.nonprofit.event;

/**
 * OBSERVER pattern - Observer: the lecture's {@code update()}. Called by the
 * Subject ({@link EventSubject}, i.e. an {@link Event}) it is registered with when that event changes.
 */
public interface EventObserver {
    void update(Event event, EventChange change, String message);
}
