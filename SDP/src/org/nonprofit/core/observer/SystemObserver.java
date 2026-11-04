package org.nonprofit.core.observer;

/**
 * OBSERVER pattern - Observer: anything that wants to hear about system events.
 * {@link #onEvent} is the lecture's update(); the Subject is {@link EventBus}.
 */
@FunctionalInterface
public interface SystemObserver {
    void onEvent(SystemEvent event);
}
