package org.nonprofit.volunteer;

import java.time.DayOfWeek;

/**
 * A piece of volunteer work: what skill it needs, which day it happens,
 * what category it belongs to (e.g. "health", "logistics") and, optionally,
 * the event it is part of. A task has at most one volunteer.
 */
public class Task {
    private static int nextId = 1;

    private final String id;
    private String name;
    private String requiredSkill;
    private DayOfWeek day;
    private String category;
    private String eventName;
    private Volunteer assignedVolunteer;
    private boolean completed;

    public Task(String name, String requiredSkill, DayOfWeek day, String category, String eventName) {
        // Everything is checked before the number is taken, so a rejected task does not use one up.
        check(name, requiredSkill, day, category);
        this.id = "TSK-" + nextId++;
        this.name = name;
        this.requiredSkill = Volunteer.normalizeSkill(requiredSkill);
        this.day = day;
        this.category = Volunteer.normalizeCategory(category);
        this.eventName = eventName;
    }

    /** A task that is not part of any event. */
    public Task(String name, String requiredSkill, DayOfWeek day, String category) {
        this(name, requiredSkill, day, category, null);
    }

    /** Throws if any of the task's details is missing. */
    static void check(String name, String requiredSkill, DayOfWeek day, String category) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name is required");
        }
        if (day == null) {
            throw new IllegalArgumentException("Task day is required");
        }
        Volunteer.normalizeSkill(requiredSkill);
        Volunteer.normalizeCategory(category);
    }

    // ---- Bookkeeping (driven by VolunteerService) ----

    /**
     * Changes the task's details. {@link VolunteerService#updateTask} checks
     * the assignment rules first; the values are checked here before any
     * field changes, so a rejected update leaves the task as it was.
     */
    void update(String name, String requiredSkill, DayOfWeek day, String category, String eventName) {
        check(name, requiredSkill, day, category);
        this.name = name;
        this.requiredSkill = Volunteer.normalizeSkill(requiredSkill);
        this.day = day;
        this.category = Volunteer.normalizeCategory(category);
        this.eventName = eventName;
    }

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
