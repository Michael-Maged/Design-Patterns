package org.nonprofit.event;

/**
 * DECORATOR pattern (concrete component): general admission at the event's base price.
 * The price is fixed when the ticket is issued, so a later change to the
 * event's base price does not rewrite tickets already sold.
 */
public class BasicTicket implements Ticket {
    private final Event event;
    private final Attendee holder;
    private final double price;

    public BasicTicket(Event event, Attendee holder) {
        if (event == null || holder == null) {
            throw new IllegalArgumentException("A ticket needs an event and a holder");
        }
        this.event = event;
        this.holder = holder;
        this.price = event.getBaseTicketPrice();
    }

    @Override
    public double price() {
        return price;
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
