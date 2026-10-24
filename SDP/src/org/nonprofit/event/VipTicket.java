package org.nonprofit.event;

/** DECORATOR pattern (concrete decorator): front-row seating and backstage access. */
public class VipTicket extends TicketDecorator {
    static final double SURCHARGE = 50.0;

    public VipTicket(Ticket wrapped) {
        super(wrapped);
    }

    @Override
    public double price() {
        return wrapped.price() + SURCHARGE;
    }

    @Override
    public String description() {
        return wrapped.description() + " + VIP seating";
    }
}
