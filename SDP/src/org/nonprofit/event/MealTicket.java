package org.nonprofit.event;

/** DECORATOR pattern (concrete decorator): adds a meal to the ticket. */
public class MealTicket extends TicketDecorator {
    static final double SURCHARGE = 15.0;

    public MealTicket(Ticket wrapped) {
        super(wrapped);
    }

    @Override
    public double price() {
        return wrapped.price() + SURCHARGE;
    }

    @Override
    public String description() {
        return wrapped.description() + " + Meal";
    }
}
