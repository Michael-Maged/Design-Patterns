package org.nonprofit.volunteer;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;

/**
 * A volunteer with skills, the days they can work, the kinds of tasks they
 * prefer, the tasks they are assigned to and the hours they have logged.
 * Skills and categories are stored lower-case so "First Aid" matches "first aid".
 */
public class Volunteer extends Person {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final Set<String> skills = new LinkedHashSet<>();
    private final EnumSet<DayOfWeek> availability = EnumSet.noneOf(DayOfWeek.class);
    private final Set<String> preferredCategories = new LinkedHashSet<>();
    private final List<Task> tasks = new ArrayList<>();
    private final List<HourEntry> hourLog = new ArrayList<>();

    public Volunteer(String name, String email, String phone, ContactPreference preferredContact) {
        super("VOL-" + NEXT_ID.getAndIncrement(), name, email, phone, preferredContact);
    }

    /** Volunteers are reached by SMS unless they ask otherwise. */
    public Volunteer(String name, String email, String phone) {
        this(name, email, phone, ContactPreference.SMS);
    }

    // ---- Profile (each returns this so a profile can be built in one line) ----

    public Volunteer addSkill(String skill) {
        skills.add(normalize(skill));
        return this;
    }

    public Volunteer removeSkill(String skill) {
        skills.remove(normalize(skill));
        return this;
    }

    public Volunteer addAvailability(DayOfWeek... days) {
        availability.addAll(List.of(days));
        return this;
    }

    public Volunteer removeAvailability(DayOfWeek day) {
        availability.remove(day);
        return this;
    }

    public Volunteer addPreference(String category) {
        preferredCategories.add(normalize(category));
        return this;
    }

    public Volunteer removePreference(String category) {
        preferredCategories.remove(normalize(category));
        return this;
    }

    // ---- Queries used by the assignment strategies ----

    public boolean hasSkill(String skill) {
        return skills.contains(normalize(skill));
    }

    public boolean isAvailableOn(DayOfWeek day) {
        return availability.contains(day);
    }

    public boolean prefers(String category) {
        return preferredCategories.contains(normalize(category));
    }

    /** True if an unfinished task already takes this volunteer's day. */
    public boolean isBookedOn(DayOfWeek day) {
        return tasks.stream().anyMatch(t -> !t.isCompleted() && t.getDay() == day);
    }

    public int openTaskCount() {
        return (int) tasks.stream().filter(t -> !t.isCompleted()).count();
    }

    public double getTotalHours() {
        return hourLog.stream().mapToDouble(HourEntry::hours).sum();
    }

    // ---- Bookkeeping (driven by VolunteerService) ----

    void assign(Task task) {
        tasks.add(task);
    }

    void unassign(Task task) {
        tasks.remove(task);
    }

    void recordHours(HourEntry entry) {
        hourLog.add(entry);
    }

    // ---- Getters ----

    public Set<String> getSkills() { return Set.copyOf(skills); }
    public Set<DayOfWeek> getAvailability() { return EnumSet.copyOf(availability); }
    public Set<String> getPreferredCategories() { return Set.copyOf(preferredCategories); }
    public List<Task> getTasks() { return List.copyOf(tasks); }
    public List<HourEntry> getHourLog() { return List.copyOf(hourLog); }

    public String summary() {
        return String.format("%s %s | skills: %s | days: %s | prefers: %s | %.1f h, %d open task(s)",
                getId(), getName(), skills, availability, preferredCategories, getTotalHours(), openTaskCount());
    }

    static String normalize(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Value is required");
        }
        return text.trim().toLowerCase();
    }
}
