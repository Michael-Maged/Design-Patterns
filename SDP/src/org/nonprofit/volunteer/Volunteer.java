package org.nonprofit.volunteer;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;

/**
 * A volunteer with skills, the days they can work, the kinds of tasks they
 * prefer, the tasks they are assigned to and the hours they have logged.
 * Skills and categories are stored lower-case so "First Aid" matches "first aid".
 */
public class Volunteer extends Person {
    private static int nextId = 1;

    private final Set<String> skills = new LinkedHashSet<>();
    private final Set<DayOfWeek> availability = new LinkedHashSet<>();
    private final Set<String> preferredCategories = new LinkedHashSet<>();
    private final List<Task> tasks = new ArrayList<>();
    private final List<HourEntry> hourLog = new ArrayList<>();

    public Volunteer(String name, String email, String phone, ContactPreference preferredContact) {
        super(takeId(name), name, email, phone, preferredContact);
    }

    /** Volunteers are reached by SMS unless they ask otherwise. */
    public Volunteer(String name, String email, String phone) {
        this(name, email, phone, ContactPreference.SMS);
    }

    /** Checks the name before taking a number, so a rejected volunteer does not use one up. */
    private static String takeId(String name) {
        requireName(name);
        return "VOL-" + nextId++;
    }

    // ---- Profile (each returns this so a profile can be built in one line) ----

    public Volunteer addSkill(String skill) {
        skills.add(normalizeSkill(skill));
        return this;
    }

    /**
     * Package-private: other packages go through {@link VolunteerService#removeSkill},
     * which refuses a removal that clashes with an open task.
     */
    Volunteer removeSkill(String skill) {
        skills.remove(normalizeSkill(skill));
        return this;
    }

    public Volunteer addAvailability(DayOfWeek... days) {
        // Checked first, so a null day neither gets stored nor leaves the profile half changed.
        for (DayOfWeek day : days) {
            if (day == null) {
                throw new IllegalArgumentException("Day is required");
            }
        }
        for (DayOfWeek day : days) {
            availability.add(day);
        }
        return this;
    }

    /**
     * Package-private: other packages go through {@link VolunteerService#removeAvailability},
     * which refuses a removal that clashes with an open task.
     */
    Volunteer removeAvailability(DayOfWeek day) {
        availability.remove(day);
        return this;
    }

    public Volunteer addPreference(String category) {
        preferredCategories.add(normalizeCategory(category));
        return this;
    }

    public Volunteer removePreference(String category) {
        preferredCategories.remove(normalizeCategory(category));
        return this;
    }

    // ---- Queries used by the assignment strategies ----

    public boolean hasSkill(String skill) {
        return skills.contains(normalizeSkill(skill));
    }

    public boolean isAvailableOn(DayOfWeek day) {
        return availability.contains(day);
    }

    public boolean prefers(String category) {
        return preferredCategories.contains(normalizeCategory(category));
    }

    /**
     * True if an unfinished task already takes this volunteer's day.
     * Tasks are booked by day of the week, not by date, so a completed task
     * (for example last Saturday's) must not block that weekday forever.
     */
    public boolean isBookedOn(DayOfWeek day) {
        for (Task task : tasks) {
            if (!task.isCompleted() && task.getDay() == day) {
                return true;
            }
        }
        return false;
    }

    public int openTaskCount() {
        int count = 0;
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public double getTotalHours() {
        double total = 0;
        for (HourEntry entry : hourLog) {
            total += entry.getHours();
        }
        return total;
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

    // ---- Getters (copies, so callers cannot change the volunteer's data) ----

    public Set<String> getSkills() { return new LinkedHashSet<>(skills); }
    public Set<DayOfWeek> getAvailability() { return new LinkedHashSet<>(availability); }
    public Set<String> getPreferredCategories() { return new LinkedHashSet<>(preferredCategories); }
    public List<Task> getTasks() { return new ArrayList<>(tasks); }
    public List<HourEntry> getHourLog() { return new ArrayList<>(hourLog); }

    public String summary() {
        return String.format("%s %s | skills: %s | days: %s | prefers: %s | %.1f h, %d open task(s)",
                getId(), getName(), skills, availability, preferredCategories, getTotalHours(), openTaskCount());
    }

    static String normalizeSkill(String skill) {
        return normalize(skill, "Skill");
    }

    static String normalizeCategory(String category) {
        return normalize(category, "Category");
    }

    /** @param field what the text is, for the error message ("Skill", "Category") */
    private static String normalize(String text, String field) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        // Locale.ROOT so matching does not change with the computer's language (e.g. Turkish "I").
        return text.trim().toLowerCase(Locale.ROOT);
    }
}
