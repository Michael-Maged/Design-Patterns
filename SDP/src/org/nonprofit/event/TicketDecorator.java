package org.nonprofit.event;

/**
 * DECORATOR pattern (abstract decorator): wraps another {@link Ticket} and
 * forwards to it. Subclasses add to the price and the description, and
 * decorators can be stacked in any order, e.g. Parking(Meal(VIP(Basic))).
 */
public abstract class TicketDecorator implements Ticket {
    protected final Ticket wrapped;

    protected TicketDecorator(Ticket wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public double price() {
        return wrapped.price();
    }

    @Override
    public String description() {
        return wrapped.description();
    }

    @Override
    public Event getEvent() { return wrapped.getEvent(); }

    @Override
    public Attendee getHolder() { return wrapped.getHolder(); }

    @Override
    public String toString() {
        return String.format("%s for %s - $%.2f", description(), getEvent().getName(), price());
    }
}
