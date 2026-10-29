package org.nonprofit.volunteer;

import java.util.List;

/**
 * STRATEGY pattern (strategy interface): one way of choosing which volunteer
 * gets a task. {@link VolunteerService} holds one strategy and it can be
 * swapped at runtime, so the same task may go to a different person.
 */
public interface AssignmentStrategy {

    /**
     * @param candidates volunteers who are free on the task's day
     * @return the chosen volunteer, or null if nobody fits
     */
    Volunteer pick(Task task, List<Volunteer> candidates);

    /** Short label shown in menus and demo output. */
    String name();
}
