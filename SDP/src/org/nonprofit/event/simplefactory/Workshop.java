package org.nonprofit.event.simplefactory;

import java.time.LocalDateTime;
import org.nonprofit.event.observer.Event;

/** A training/learning session led by an instructor. */
public class Workshop extends Event {
    static final double DEFAULT_TICKET_PRICE = 25.0;

    private String topic = "General";
    private String instructor = "TBA";

    Workshop(String id, String name, LocalDateTime dateTime, String location, int capacity) {
        super(id, name, dateTime, location, capacity, DEFAULT_TICKET_PRICE);
    }

    @Override
    public EventType getType() {
        return EventType.WORKSHOP;
    }

    public void setTopic(String topic) {
        requireText(topic, "Workshop topic");
        requireNotCancelled("change its topic");
        this.topic = topic;
    }

    public void setInstructor(String instructor) {
        requireText(instructor, "Instructor");
        requireNotCancelled("change its instructor");
        this.instructor = instructor;
    }

    /** Sets both together; both are checked before either changes. */
    public void setDetails(String topic, String instructor) {
        requireText(topic, "Workshop topic");
        requireText(instructor, "Instructor");
        requireNotCancelled("change its details");
        this.topic = topic;
        this.instructor = instructor;
    }

    private static String requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
        return value;
    }
    public String getTopic() { return topic; }
    public String getInstructor() { return instructor; }

    @Override
    protected String details() {
        return "topic: " + topic + ", instructor: " + instructor;
    }
}
