package org.nonprofit.event.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.nonprofit.core.observer.EventBus;
import org.nonprofit.core.observer.SystemEvent;
import org.nonprofit.core.observer.SystemEventType;
import org.nonprofit.event.observer.Attendee;
import org.nonprofit.event.decorator.BasicTicket;
import org.nonprofit.event.observer.Event;
import org.nonprofit.event.observer.EventBusNotifier;
import org.nonprofit.event.observer.EventChange;
import org.nonprofit.event.mvc.EventController;
import org.nonprofit.event.simplefactory.EventFactory;
import org.nonprofit.event.observer.EventObserver;
import org.nonprofit.event.simplefactory.EventType;
import org.nonprofit.event.simplefactory.Fundraiser;
import org.nonprofit.event.simplefactory.OutreachProgram;
import org.nonprofit.event.decorator.Ticket;
import org.nonprofit.event.decorator.TicketAddOn;
import org.nonprofit.event.simplefactory.Workshop;

/**
 * Entry point of the Events module (the Events screen, {@link EventController}, calls this).
 * Creates events through the {@link EventFactory}, issues decorated
 * {@link Ticket}s, manages capacity and the waitlist, tracks attendance
 * and sends reminders. It also keeps the attendees on file, so the Events
 * screen can pick an existing person by ID.
 */
public class EventService {
    private final Map<String, Event> events = new LinkedHashMap<>();
    /** The people on file, by ID (registering for an event does not add anyone here). */
    private final Map<String, Attendee> attendees = new LinkedHashMap<>();
    private final EventObserver busNotifier = new EventBusNotifier();
    private final EventBus bus = EventBus.getInstance();

    // ---- Creating and finding events ----

    public Event createEvent(EventType type, String name, LocalDateTime dateTime, String location, int capacity) {
        return store(EventFactory.create(type, name, dateTime, location, capacity));
    }

    public Event createEvent(String typeInput, String name, LocalDateTime dateTime, String location, int capacity) {
        return store(EventFactory.create(typeInput, name, dateTime, location, capacity));
    }

    private Event store(Event event) {
        // Every event forwards its changes to the system bus.
        event.registerObserver(busNotifier);
        events.put(event.getId(), event);
        return event;
    }

    public Optional<Event> findEvent(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

    public List<Event> listEvents() {
        return List.copyOf(events.values());
    }

    /**
     * Deletes an event that nobody has signed up for. An event with people
     * registered or waitlisted must be cancelled instead, so they are told.
     */
    public void deleteEvent(String eventId) {
        Event event = require(eventId);
        if (event.hasPeople()) {
            throw new IllegalStateException(event.getName() + " has people registered or waitlisted"
                    + " - cancel the event instead so they are notified");
        }
        event.removeAllObservers();
        events.remove(eventId);
    }

    public List<Event> upcomingEvents(LocalDateTime now) {
        return events.values().stream()
                .filter(e -> !e.isCancelled() && e.getDateTime().isAfter(now))
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .toList();
    }

    // ---- Attendees on file ----

    public synchronized Attendee addAttendee(Attendee attendee) {
        if (attendee == null) {
            throw new IllegalArgumentException("Attendee is required");
        }
        attendees.put(attendee.getId(), attendee);
        return attendee;
    }

    /** @return the attendee, or null if there is none with that id */
    public synchronized Attendee findAttendee(String attendeeId) {
        return attendees.get(attendeeId);
    }

    /** A copy, so callers cannot change the registry. */
    public synchronized List<Attendee> listAttendees() {
        return new ArrayList<>(attendees.values());
    }

    // ---- Registration, ticketing and the waitlist ----

    public RegistrationResult register(String eventId, Attendee attendee, TicketAddOn... addOns) {
        Event event = require(eventId);
        if (attendee == null) {
            throw new IllegalArgumentException("Attendee is required");
        }
        if (addOns == null || Arrays.asList(addOns).contains(null)) {
            throw new IllegalArgumentException("Ticket add-ons cannot be null");
        }
        if (event.isCancelled()) {
            throw new IllegalStateException(event.getName() + " is cancelled");
        }
        if (event.isRegistered(attendee)) {
            return RegistrationResult.of(RegistrationResult.Status.ALREADY_REGISTERED);
        }
        if (event.isWaitlisted(attendee)) {
            return RegistrationResult.of(RegistrationResult.Status.ALREADY_WAITLISTED);
        }
        if (event.isFull()) {
            int position = event.addToWaitlist(new WaitlistEntry(attendee, List.of(addOns)));
            return RegistrationResult.waitlisted(position);
        }
        Ticket ticket = issueTicket(event, attendee, List.of(addOns));
        event.addRegistration(attendee, ticket);
        bus.publish(new SystemEvent(SystemEventType.EVENT_REGISTERED,
                "You are registered for " + event.getName() + ": " + ticket, attendee, event));
        return RegistrationResult.registered(ticket);
    }

    /**
     * Same as {@link #register(String, Attendee, TicketAddOn...)}, but refuses
     * an event that has already started at {@code now}.
     */
    public RegistrationResult register(String eventId, Attendee attendee, LocalDateTime now, TicketAddOn... addOns) {
        Event event = require(eventId);
        requireNow(now);
        if (!event.getDateTime().isAfter(now)) {
            throw new IllegalStateException(event.getName() + " has already started - registration is closed");
        }
        return register(eventId, attendee, addOns);
    }

    /**
     * Cancels a registration (or removes the person from the waitlist).
     * A freed seat is given to the first person waiting, unless the event is cancelled.
     *
     * @return false if the attendee was neither registered nor waitlisted
     */
    public boolean cancelRegistration(String eventId, Attendee attendee) {
        Event event = require(eventId);
        if (event.removeFromWaitlist(attendee)) {
            return true;
        }
        if (event.removeRegistration(attendee) == null) {
            return false;
        }
        promoteFromWaitlist(event);
        return true;
    }

    private void promoteFromWaitlist(Event event) {
        if (event.isCancelled()) {
            return;
        }
        while (!event.isFull()) {
            WaitlistEntry next = event.pollWaitlist();
            if (next == null) {
                return;
            }
            Attendee attendee = next.attendee();
            Ticket ticket = issueTicket(event, attendee, next.addOns());
            event.addRegistration(attendee, ticket);
            String message = "a seat opened up - you are now registered (" + ticket.description() + ")";
            // Only the promoted person is told, not every attendee.
            attendee.update(event, EventChange.WAITLIST_PROMOTED, message);
            bus.publish(new SystemEvent(SystemEventType.WAITLIST_PROMOTED,
                    event.getName() + ": " + message, attendee, event));
        }
    }

    /** Builds a basic ticket and wraps it in one decorator per add-on. */
    private Ticket issueTicket(Event event, Attendee attendee, List<TicketAddOn> addOns) {
        Ticket ticket = new BasicTicket(event, attendee);
        for (TicketAddOn addOn : addOns) {
            ticket = addOn.applyTo(ticket);
        }
        return ticket;
    }

    // ---- Attendance ----

    public void checkIn(String eventId, Attendee attendee) {
        Event event = require(eventId);
        event.requireNotCancelled("check anyone in");
        if (!event.isRegistered(attendee)) {
            throw new IllegalArgumentException(attendee.getName() + " is not registered for " + event.getName());
        }
        event.markCheckedIn(attendee);
    }

    public double attendanceRate(String eventId) {
        Event event = require(eventId);
        int registered = event.getAttendees().size();
        return registered == 0 ? 0 : event.getCheckedIn().size() * 100.0 / registered;
    }

    public double ticketRevenue(String eventId) {
        return require(eventId).getRegistrations().values().stream().mapToDouble(Ticket::price).sum();
    }

    public String attendanceReport(String eventId) {
        Event event = require(eventId);
        StringBuilder sb = new StringBuilder();
        sb.append("Attendance report - ").append(event.summary()).append('\n');
        for (Map.Entry<Attendee, Ticket> entry : event.getRegistrations().entrySet()) {
            boolean present = event.getCheckedIn().contains(entry.getKey());
            sb.append(String.format("  %-6s %-16s %-50s $%7.2f%n",
                    present ? "[x]" : "[ ]", entry.getKey().getName(),
                    entry.getValue().description(), entry.getValue().price()));
        }
        for (Attendee waiting : event.getWaitlist()) {
            sb.append(String.format("  %-6s %s%n", "(wait)", waiting.getName()));
        }
        sb.append(String.format("  Checked in: %d/%d (%.0f%%)   Ticket revenue: $%.2f",
                event.getCheckedIn().size(), event.getAttendees().size(),
                attendanceRate(eventId), ticketRevenue(eventId)));
        return sb.toString();
    }

    // ---- Changes and reminders (each triggers the Observer chain) ----

    public void reschedule(String eventId, LocalDateTime newDateTime) {
        require(eventId).reschedule(newDateTime);
    }

    public void relocate(String eventId, String newLocation) {
        require(eventId).relocate(newLocation);
    }

    /**
     * Cancels the event. Registered people hear through the event's
     * observers; people on the waitlist are not observers, so each of them
     * gets their own message on the bus as well.
     */
    public void cancelEvent(String eventId) {
        Event event = require(eventId);
        List<Attendee> waiting = event.getWaitlist();
        event.cancel();
        for (Attendee attendee : waiting) {
            bus.publish(new SystemEvent(SystemEventType.EVENT_CANCELLED,
                    event.getName() + " has been cancelled - you have been removed from the waitlist",
                    attendee, event));
        }
    }

    /** Refuses a cancelled event (see {@link Event#remind()}). */
    public void sendReminder(String eventId) {
        require(eventId).remind();
    }

    /** Sends a reminder, refusing an event that is cancelled or has already started at {@code now}. */
    public void sendReminder(String eventId, LocalDateTime now) {
        Event event = require(eventId);
        requireNow(now);
        event.requireNotCancelled("send a reminder");
        if (!event.getDateTime().isAfter(now)) {
            throw new IllegalStateException(event.getName() + " has already started - no reminder sent");
        }
        event.remind();
    }

    /** Sends reminders for every event starting within {@code window} of {@code now}. */
    public List<Event> sendUpcomingReminders(LocalDateTime now, Duration window) {
        List<Event> reminded = new ArrayList<>();
        LocalDateTime cutoff = now.plus(window);
        for (Event event : upcomingEvents(now)) {
            if (!event.getDateTime().isAfter(cutoff)) {
                event.remind();
                reminded.add(event);
            }
        }
        return reminded;
    }

    // ---- Editing events (Update in CRUD) ----

    public void renameEvent(String eventId, String newName) {
        require(eventId).rename(newName);
    }

    /** Applies to tickets issued from now on; tickets already issued keep their price. */
    public void setBaseTicketPrice(String eventId, double price) {
        require(eventId).setBaseTicketPrice(price);
    }

    /**
     * Changes the number of seats. It cannot go below the number already
     * registered; if seats are added, people waiting are promoted into them.
     */
    public void setCapacity(String eventId, int capacity) {
        Event event = require(eventId);
        event.setCapacity(capacity);
        promoteFromWaitlist(event);
    }

    public void setFundraiserGoal(String eventId, double goal) {
        requireType(eventId, Fundraiser.class, "a fundraiser").setFundraisingGoal(goal);
    }

    public void recordPledge(String eventId, double amount) {
        requireType(eventId, Fundraiser.class, "a fundraiser").recordPledge(amount);
    }

    public void setWorkshopDetails(String eventId, String topic, String instructor) {
        requireType(eventId, Workshop.class, "a workshop").setDetails(topic, instructor);
    }

    public void setTargetCommunity(String eventId, String community) {
        requireType(eventId, OutreachProgram.class, "an outreach program").setTargetCommunity(community);
    }

    private <T extends Event> T requireType(String eventId, Class<T> type, String what) {
        Event event = require(eventId);
        if (!type.isInstance(event)) {
            throw new IllegalArgumentException(event.getName() + " (" + eventId + ") is a "
                    + event.getType() + ", not " + what);
        }
        return type.cast(event);
    }

    private static void requireNow(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("The current time is required");
        }
    }

    private Event require(String eventId) {
        Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("No event with id " + eventId);
        }
        return event;
    }
}
