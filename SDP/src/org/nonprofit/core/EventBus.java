package org.nonprofit.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * SINGLETON + OBSERVER pattern: the one system-wide notification hub.
 * Modules publish {@link SystemEvent}s here; other modules (mainly
 * Communication) subscribe, so publishers never depend on subscribers.
 */
public final class EventBus {
    private static volatile EventBus instance;

    private final Map<SystemEventType, List<SystemObserver>> subscribers = new EnumMap<>(SystemEventType.class);

    private EventBus() {
    }

    /** Lazily created, thread-safe (double-checked locking). */
    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }

    public synchronized void subscribe(SystemEventType type, SystemObserver observer) {
        subscribers.computeIfAbsent(type, t -> new ArrayList<>()).add(observer);
    }

    public synchronized void unsubscribe(SystemEventType type, SystemObserver observer) {
        List<SystemObserver> list = subscribers.get(type);
        if (list != null) {
            list.remove(observer);
        }
    }

    public void publish(SystemEvent event) {
        List<SystemObserver> targets;
        synchronized (this) {
            // Copy so observers may (un)subscribe while being notified.
            targets = new ArrayList<>(subscribers.getOrDefault(event.getType(), List.of()));
        }
        for (SystemObserver observer : targets) {
            observer.onEvent(event);
        }
    }
}
