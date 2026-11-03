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

import org.nonprofit.core.Audience;

/**
 * Base class for all events.
 *
 * OBSERVER pattern - Subject (implements {@link EventSubject}): registered
 * attendees and other {@link EventObserver}s are notified through
 * {@link #notifyObservers} whenever the event is rescheduled, relocated,
 * cancelled or a reminder goes out.
 *
 * Concrete types are created only through {@link EventFactory}.
 */
public abstract class Event implements Audience, EventSubject {
    static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String id;
    private String name;
    private LocalDateTime dateTime;
    private String location;
    private int capacity;
    private double baseTicketPrice;
    private boolean cancelled;

    private final Map<Attendee, Ticket> registrations = new LinkedHashMap<>();
    private final Deque<WaitlistEntry> waitlist = new ArrayDeque<>();
    private final Set<Attendee> checkedIn = new LinkedHashSet<>();
    private final List<EventObserver> observers = new ArrayList<>();

    protected Event(String id, String name, LocalDateTime dateTime, String location, int capacity,
                    double baseTicketPrice) {
        validateDetails(name, dateTime, location, capacity);
        requirePrice(baseTicketPrice);
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.location = location;
        this.capacity = capacity;
        this.baseTicketPrice = baseTicketPrice;
    }

    /**
     * Checks the details every event needs. {@link EventFactory} calls this
     * before taking the next ID number, so a rejected event does not use one up.
     */
    static void validateDetails(String name, LocalDateTime dateTime, String location, int capacity) {
        requireName(name);
        requireDateTime(dateTime);
        requireLocation(location);
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
        return name;
    }

    private static LocalDateTime requireDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("Event date and time are required");
        }
        return dateTime;
    }

    private static String requireLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Event location is required");
        }
        return location;
    }

    private static double requirePrice(double price) {
        if (Double.isNaN(price) || Double.isInfinite(price) || price < 0) {
            throw new IllegalArgumentException("Ticket price must be a non-negative number");
        }
        return price;
    }

    /** Refuses a change to an event that has already been cancelled. */
    void requireNotCancelled(String action) {
        if (cancelled) {
            throw new IllegalStateException(name + " is cancelled - cannot " + action);
        }
    }

    public abstract EventType getType();

    /** Type-specific information shown in {@link #summary()}. */
    protected abstract String details();

    // ---- Observer: subject operations ----

    @Override
    public void registerObserver(EventObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(EventObserver observer) {
        observers.remove(observer);
    }

    protected void notifyObservers(EventChange change, String message) {
        // Copy so an observer may remove itself while being notified.
        for (EventObserver observer : List.copyOf(observers)) {
            observer.update(this, change, message);
        }
    }

    // ---- State changes that trigger notifications ----

    public void reschedule(LocalDateTime newDateTime) {
        requireDateTime(newDateTime);
        requireNotCancelled("reschedule it");
        LocalDateTime old = dateTime;
        dateTime = newDateTime;
        notifyObservers(EventChange.RESCHEDULED,
                "moved from " + old.format(DISPLAY_FORMAT) + " to " + newDateTime.format(DISPLAY_FORMAT));
    }

    public void relocate(String newLocation) {
        requireLocation(newLocation);
        requireNotCancelled("relocate it");
        String old = location;
        location = newLocation;
        notifyObservers(EventChange.RELOCATED, "moved from " + old + " to " + newLocation);
    }

    /**
     * Cancels the event and tells everyone registered (through the observers).
     * People on the waitlist are not observers, so each is told directly, and
     * the waitlist is then cleared - nobody can be promoted into a cancelled event.
     */
    public void cancel() {
        requireNotCancelled("cancel it again");
        cancelled = true;
        notifyObservers(EventChange.CANCELLED, "has been cancelled");
        for (WaitlistEntry entry : List.copyOf(waitlist)) {
            entry.attendee().update(this, EventChange.CANCELLED,
                    "has been cancelled - you have been removed from the waitlist");
        }
        waitlist.clear();
    }

    public void remind() {
        requireNotCancelled("send a reminder");
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
        registerObserver(attendee);
    }

    Ticket removeRegistration(Attendee attendee) {
        checkedIn.remove(attendee);
        removeObserver(attendee);
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
        requireNotCancelled("check anyone in");
        checkedIn.add(attendee);
    }

    boolean hasPeople() {
        return !registrations.isEmpty() || !waitlist.isEmpty();
    }

    /** Used when the event is deleted: nobody keeps listening to it. */
    void removeAllObservers() {
        observers.clear();
    }

    // ---- Editing event details ----

    public void rename(String newName) {
        requireName(newName);
        requireNotCancelled("rename it");
        this.name = newName;
    }

    /**
     * Changes how many seats the event has. It cannot drop below the number
     * already registered. Filling newly added seats from the waitlist is done
     * by {@link EventService#setCapacity}, which is why this is package-private.
     */
    void setCapacity(int newCapacity) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        requireNotCancelled("change its capacity");
        if (newCapacity < registrations.size()) {
            throw new IllegalArgumentException("Capacity cannot be below the " + registrations.size()
                    + " people already registered for " + name);
        }
        this.capacity = newCapacity;
    }

    // ---- Getters ----

    public String getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getLocation() { return location; }
    public int getCapacity() { return capacity; }
    public double getBaseTicketPrice() { return baseTicketPrice; }

    /** Applies to tickets issued from now on; tickets already issued keep their price. */
    public void setBaseTicketPrice(double price) {
        requirePrice(price);
        requireNotCancelled("change its ticket price");
        this.baseTicketPrice = price;
    }

    public boolean isCancelled() { return cancelled; }

    public List<Attendee> getAttendees() {
        return List.copyOf(registrations.keySet());
    }

    /** Lets Communication reach every registered attendee without knowing this class. */
    @Override
    public List<Attendee> getAudience() {
        return getAttendees();
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
