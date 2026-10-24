package org.nonprofit.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all events.
 *
 * OBSERVER pattern (subject role): registered attendees and other
 * {@link EventObserver}s are notified whenever the event is rescheduled,
 * relocated, cancelled or a reminder goes out.
 *
 * Concrete types are created only through {@link EventFactory}.
 */
public abstract class Event {
    static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String id;
    private final String name;
    private LocalDateTime dateTime;
    private String location;
    private final int capacity;
    private double baseTicketPrice;
    private boolean cancelled;

    private final Map<Attendee, Ticket> registrations = new LinkedHashMap<>();
    private final Deque<WaitlistEntry> waitlist = new ArrayDeque<>();
    private final Set<Attendee> checkedIn = new LinkedHashSet<>();
    private final List<EventObserver> observers = new ArrayList<>();

    protected Event(String id, String name, LocalDateTime dateTime, String location, int capacity,
                    double baseTicketPrice) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.location = location;
        this.capacity = capacity;
        this.baseTicketPrice = baseTicketPrice;
    }

    public abstract EventType getType();

    /** Type-specific information shown in {@link #summary()}. */
    protected abstract String details();

    // ---- Observer: subject operations ----

    public void attach(EventObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void detach(EventObserver observer) {
        observers.remove(observer);
    }

    protected void notifyObservers(EventChange change, String message) {
        // Copy so an observer may detach itself while being notified.
        for (EventObserver observer : List.copyOf(observers)) {
            observer.update(this, change, message);
        }
    }

    // ---- State changes that trigger notifications ----

    public void reschedule(LocalDateTime newDateTime) {
        LocalDateTime old = dateTime;
        dateTime = newDateTime;
        notifyObservers(EventChange.RESCHEDULED,
                "moved from " + old.format(DISPLAY_FORMAT) + " to " + newDateTime.format(DISPLAY_FORMAT));
    }

    public void relocate(String newLocation) {
        String old = location;
        location = newLocation;
        notifyObservers(EventChange.RELOCATED, "moved from " + old + " to " + newLocation);
    }

    public void cancel() {
        cancelled = true;
        notifyObservers(EventChange.CANCELLED, "has been cancelled");
    }

    public void remind() {
        notifyObservers(EventChange.REMINDER,
                "starts " + dateTime.format(DISPLAY_FORMAT) + " at " + location);
    }

    // ---- Registration bookkeeping (driven by EventService) ----

    boolean isFull() {
        return registrations.size() >= capacity;
    }

    boolean isRegistered(Attendee attendee) {
        return registrations.containsKey(attendee);
    }

    boolean isWaitlisted(Attendee attendee) {
        return waitlist.stream().anyMatch(e -> e.attendee().equals(attendee));
    }

    void addRegistration(Attendee attendee, Ticket ticket) {
        registrations.put(attendee, ticket);
        attach(attendee);
    }

    Ticket removeRegistration(Attendee attendee) {
        checkedIn.remove(attendee);
        detach(attendee);
        return registrations.remove(attendee);
    }

    /** @return the 1-based position on the waitlist */
    int addToWaitlist(WaitlistEntry entry) {
        waitlist.addLast(entry);
        return waitlist.size();
    }

    WaitlistEntry pollWaitlist() {
        return waitlist.pollFirst();
    }

    boolean removeFromWaitlist(Attendee attendee) {
        return waitlist.removeIf(e -> e.attendee().equals(attendee));
    }

    void markCheckedIn(Attendee attendee) {
        checkedIn.add(attendee);
    }

    // ---- Getters ----

    public String getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getLocation() { return location; }
    public int getCapacity() { return capacity; }
    public double getBaseTicketPrice() { return baseTicketPrice; }
    public void setBaseTicketPrice(double price) { this.baseTicketPrice = price; }
    public boolean isCancelled() { return cancelled; }

    public List<Attendee> getAttendees() {
        return List.copyOf(registrations.keySet());
    }

    public Ticket getTicket(Attendee attendee) {
        return registrations.get(attendee);
    }

    public Map<Attendee, Ticket> getRegistrations() {
        return Collections.unmodifiableMap(registrations);
    }

    public List<Attendee> getWaitlist() {
        return waitlist.stream().map(WaitlistEntry::attendee).toList();
    }

    public Set<Attendee> getCheckedIn() {
        return Collections.unmodifiableSet(checkedIn);
    }

    public String summary() {
        return String.format("%s [%s] %s | %s @ %s | %d/%d registered%s | %s",
                id, getType(), name, dateTime.format(DISPLAY_FORMAT), location,
                registrations.size(), capacity,
                waitlist.isEmpty() ? "" : ", " + waitlist.size() + " waitlisted",
                cancelled ? "CANCELLED" : details());
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
