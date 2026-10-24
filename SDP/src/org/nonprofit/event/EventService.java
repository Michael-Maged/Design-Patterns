package org.nonprofit.event;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;

/**
 * Entry point of the Events module (the Facade in core calls this).
 * Creates events through the {@link EventFactory}, issues decorated
 * {@link Ticket}s, manages capacity and the waitlist, tracks attendance
 * and sends reminders.
 */
public class EventService {
    private final Map<String, Event> events = new LinkedHashMap<>();
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
        event.attach(busNotifier);
        events.put(event.getId(), event);
        return event;
    }

    public Optional<Event> findEvent(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

    public List<Event> listEvents() {
        return List.copyOf(events.values());
    }

    public List<Event> upcomingEvents(LocalDateTime now) {
        return events.values().stream()
                .filter(e -> !e.isCancelled() && e.getDateTime().isAfter(now))
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .toList();
    }

    // ---- Registration, ticketing and the waitlist ----

    public RegistrationResult register(String eventId, Attendee attendee, TicketAddOn... addOns) {
        Event event = require(eventId);
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
     * Cancels a registration (or removes the person from the waitlist).
     * A freed seat is given to the first person waiting.
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

    public void cancelEvent(String eventId) {
        require(eventId).cancel();
    }

    public void sendReminder(String eventId) {
        require(eventId).remind();
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

    private Event require(String eventId) {
        Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("No event with id " + eventId);
        }
        return event;
    }
}
