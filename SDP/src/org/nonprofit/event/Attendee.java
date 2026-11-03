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
 * Nothing is printed here (model class); a view shows the new notes.
 */
public class Attendee extends Person implements EventObserver {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final List<String> inbox = new ArrayList<>();
    /** How many inbox notes have already been handed to a view by {@link #takeNewNotes()}. */
    private int shown;

    public Attendee(String name, String email, String phone, ContactPreference preferredContact) {
        super(nextId(name), name, email, phone, preferredContact);
    }

    /** Checks the name first, so a rejected attendee does not use up an ID number. */
    private static String nextId(String name) {
        requireName(name);
        return "ATT-" + NEXT_ID.getAndIncrement();
    }

    @Override
    public void update(Event event, EventChange change, String message) {
        String note = change + ": " + event.getName() + " " + message;
        inbox.add(note);
    }

    public List<String> getInbox() {
        return List.copyOf(inbox);
    }

    /** The notes received since the last call; the whole inbox is still kept in {@link #getInbox()}. */
    public List<String> takeNewNotes() {
        List<String> notes = List.copyOf(inbox.subList(shown, inbox.size()));
        shown = inbox.size();
        return notes;
    }
}
