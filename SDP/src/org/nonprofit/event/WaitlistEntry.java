package org.nonprofit.event;

import java.util.List;

/** Someone waiting for a seat, remembering the add-ons they asked for. */
record WaitlistEntry(Attendee attendee, List<TicketAddOn> addOns) {
}
