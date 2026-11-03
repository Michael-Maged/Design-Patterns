package org.nonprofit.event.observer;

/**
 * OBSERVER pattern - Subject: the interface from the lecture. An {@link Event}
 * keeps a list of {@link EventObserver}s and notifies them when it changes.
 * {@code notifyObservers} stays a protected method of {@link Event}, so only
 * the event itself decides when its observers are told.
 */
public interface EventSubject {
    void registerObserver(EventObserver observer);

    void removeObserver(EventObserver observer);
}
