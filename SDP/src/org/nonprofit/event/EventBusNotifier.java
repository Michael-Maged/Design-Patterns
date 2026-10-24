package org.nonprofit.event;

import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;

/**
 * OBSERVER pattern (concrete observer) that bridges a single event's
 * notifications onto the system-wide {@link EventBus}, so the Communication
 * module can e-mail / SMS attendees without the Events module knowing how.
 */
class EventBusNotifier implements EventObserver {

    @Override
    public void update(Event event, EventChange change, String message) {
        SystemEventType type = switch (change) {
            case REMINDER -> SystemEventType.EVENT_REMINDER;
            case CANCELLED -> SystemEventType.EVENT_CANCELLED;
            case WAITLIST_PROMOTED -> SystemEventType.WAITLIST_PROMOTED;
            case RESCHEDULED, RELOCATED -> SystemEventType.EVENT_UPDATED;
        };
        // recipient == null: addressed to everyone registered; source carries the event.
        EventBus.getInstance().publish(new SystemEvent(type, event.getName() + " " + message, null, event));
    }
}
