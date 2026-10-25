package org.nonprofit.volunteer;

import java.time.DayOfWeek;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A piece of volunteer work: what skill it needs, which day it happens,
 * what category it belongs to (e.g. "health", "logistics") and, optionally,
 * the event it is part of. A task has at most one volunteer.
 */
public class Task {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final String id;
    private final String name;
    private final String requiredSkill;
    private final DayOfWeek day;
    private final String category;
    private final String eventName;
    private Volunteer assignedVolunteer;
    private boolean completed;

    public Task(String name, String requiredSkill, DayOfWeek day, String category, String eventName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name is required");
        }
        if (day == null) {
            throw new IllegalArgumentException("Task day is required");
        }
        this.id = "TSK-" + NEXT_ID.getAndIncrement();
        this.name = name;
        this.requiredSkill = Volunteer.normalize(requiredSkill);
        this.day = day;
        this.category = Volunteer.normalize(category);
        this.eventName = eventName;
    }

    /** A task that is not part of any event. */
    public Task(String name, String requiredSkill, DayOfWeek day, String category) {
        this(name, requiredSkill, day, category, null);
    }

    // ---- Bookkeeping (driven by VolunteerService) ----

    void setAssignedVolunteer(Volunteer volunteer) {
        this.assignedVolunteer = volunteer;
    }

    void markCompleted() {
        this.completed = true;
    }

    // ---- Getters ----

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRequiredSkill() { return requiredSkill; }
    public DayOfWeek getDay() { return day; }
    public String getCategory() { return category; }
    public String getEventName() { return eventName; }
    public Volunteer getAssignedVolunteer() { return assignedVolunteer; }
    public boolean isAssigned() { return assignedVolunteer != null; }
    public boolean isCompleted() { return completed; }

    public String summary() {
        String who = assignedVolunteer == null ? "unassigned" : "-> " + assignedVolunteer.getName();
        return String.format("%s %s | needs %s | %s | %s%s | %s%s",
                id, name, requiredSkill, day, category,
                eventName == null ? "" : " | event: " + eventName,
                who, completed ? " (done)" : "");
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
