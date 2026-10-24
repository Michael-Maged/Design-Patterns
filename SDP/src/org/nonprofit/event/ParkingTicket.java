package org.nonprofit.event;

/** DECORATOR pattern (concrete decorator): adds a reserved parking spot. */
public class ParkingTicket extends TicketDecorator {
    static final double SURCHARGE = 10.0;

    public ParkingTicket(Ticket wrapped) {
        super(wrapped);
    }

    @Override
    public double price() {
        return wrapped.price() + SURCHARGE;
    }

    @Override
    public String description() {
        return wrapped.description() + " + Parking";
    }
}
