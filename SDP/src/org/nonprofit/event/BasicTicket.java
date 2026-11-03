package org.nonprofit.event;

/** DECORATOR pattern (concrete component): general admission at the event's base price. */
public class BasicTicket implements Ticket {
    private final Event event;
    private final Attendee holder;

    public BasicTicket(Event event, Attendee holder) {
        this.event = event;
        this.holder = holder;
    }

    @Override
    public double price() {
        return event.getBaseTicketPrice();
    }

    @Override
    public String description() {
        return "General admission";
    }

    @Override
    public Event getEvent() { return event; }

    @Override
    public Attendee getHolder() { return holder; }

    @Override
    public String toString() {
        return String.format("%s for %s - $%.2f", description(), event.getName(), price());
    }
}
