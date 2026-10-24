package org.nonprofit.core;

/** Topics that modules publish on the {@link EventBus}. */
public enum SystemEventType {
    DONATION_RECEIVED,
    VOLUNTEER_ASSIGNED,
    RESOURCE_DISTRIBUTED,
    URGENT_NEED,
    EVENT_REGISTERED,
    EVENT_UPDATED,
    EVENT_REMINDER,
    EVENT_CANCELLED,
    WAITLIST_PROMOTED
}
