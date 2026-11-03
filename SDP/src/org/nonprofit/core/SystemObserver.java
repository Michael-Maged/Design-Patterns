package org.nonprofit.core;

/** OBSERVER pattern (observer role): anything that wants to hear about system events. */
@FunctionalInterface
public interface SystemObserver {
    void onEvent(SystemEvent event);
}
