package org.nonprofit.event.simplefactory;

import java.time.LocalDateTime;
import org.nonprofit.event.observer.Event;

/** A free community outreach program aimed at a specific community. */
public class OutreachProgram extends Event {
    static final double DEFAULT_TICKET_PRICE = 0.0;

    private String targetCommunity = "General public";

    OutreachProgram(String id, String name, LocalDateTime dateTime, String location, int capacity) {
        super(id, name, dateTime, location, capacity, DEFAULT_TICKET_PRICE);
    }

    @Override
    public EventType getType() {
        return EventType.OUTREACH;
    }

    public void setTargetCommunity(String targetCommunity) {
        if (targetCommunity == null || targetCommunity.isBlank()) {
            throw new IllegalArgumentException("Target community is required");
        }
        requireNotCancelled("change its target community");
        this.targetCommunity = targetCommunity;
    }
    public String getTargetCommunity() { return targetCommunity; }

    @Override
    protected String details() {
        return "for: " + targetCommunity;
    }
}
