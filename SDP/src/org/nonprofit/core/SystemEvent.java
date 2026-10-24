package org.nonprofit.core;

/**
 * A notification travelling over the {@link EventBus}.
 * {@code recipient} is null when the notification is meant for a whole group
 * (the subscriber can inspect {@code source} to find who that group is).
 */
public final class SystemEvent {
    private final SystemEventType type;
    private final String message;
    private final Person recipient;
    private final Object source;

    public SystemEvent(SystemEventType type, String message, Person recipient, Object source) {
        this.type = type;
        this.message = message;
        this.recipient = recipient;
        this.source = source;
    }

    public SystemEventType getType() { return type; }
    public String getMessage() { return message; }
    public Person getRecipient() { return recipient; }
    public Object getSource() { return source; }

    @Override
    public String toString() {
        String to = recipient == null ? "all" : recipient.getName();
        return type + " -> " + to + ": " + message;
    }
}
