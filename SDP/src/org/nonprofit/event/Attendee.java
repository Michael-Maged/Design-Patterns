package org.nonprofit.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;

/**
 * A person who registers for events.
 * OBSERVER pattern (concrete observer): once registered, the attendee is
 * attached to the event and receives every update in their inbox.
 */
public class Attendee extends Person implements EventObserver {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final List<String> inbox = new ArrayList<>();

    public Attendee(String name, String email, String phone, ContactPreference preferredContact) {
        super("ATT-" + NEXT_ID.getAndIncrement(), name, email, phone, preferredContact);
    }

    @Override
    public void update(Event event, EventChange change, String message) {
        String note = change + ": " + event.getName() + " " + message;
        inbox.add(note);
        System.out.println("    [inbox of " + getName() + "] " + note);
    }

    public List<String> getInbox() {
        return List.copyOf(inbox);
    }
}
