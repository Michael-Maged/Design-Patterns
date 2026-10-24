package org.nonprofit.event;

import java.util.function.UnaryOperator;

/** The add-ons a user can pick; each knows which decorator to wrap a ticket in. */
public enum TicketAddOn {
    VIP(VipTicket::new),
    MEAL(MealTicket::new),
    PARKING(ParkingTicket::new);

    private final UnaryOperator<Ticket> decorator;

    TicketAddOn(UnaryOperator<Ticket> decorator) {
        this.decorator = decorator;
    }

    public Ticket applyTo(Ticket ticket) {
        return decorator.apply(ticket);
    }

    public static TicketAddOn fromString(String input) {
        try {
            return valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown ticket add-on: " + input);
        }
    }
}
