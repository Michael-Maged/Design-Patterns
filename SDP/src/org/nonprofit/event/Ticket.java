package org.nonprofit.event;

/**
 * DECORATOR pattern (component role): what every ticket - plain or with
 * add-ons stacked on top - looks like to the rest of the system.
 */
public interface Ticket {
    double price();

    String description();

    Event getEvent();

    Attendee getHolder();
}
