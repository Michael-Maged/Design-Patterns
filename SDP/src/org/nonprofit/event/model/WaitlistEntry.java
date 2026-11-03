package org.nonprofit.event.model;

import java.util.List;
import org.nonprofit.event.observer.Attendee;
import org.nonprofit.event.decorator.TicketAddOn;

/** Someone waiting for a seat, remembering the add-ons they asked for. */
public record WaitlistEntry(Attendee attendee, List<TicketAddOn> addOns) {
}
