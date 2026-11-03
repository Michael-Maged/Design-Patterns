package org.nonprofit.event;

import java.time.LocalDateTime;

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

    public void setTopic(String topic) { this.topic = topic; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public String getTopic() { return topic; }
    public String getInstructor() { return instructor; }

    @Override
    protected String details() {
        return "topic: " + topic + ", instructor: " + instructor;
    }
}
