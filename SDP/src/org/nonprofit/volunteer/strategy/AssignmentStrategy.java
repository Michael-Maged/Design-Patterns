package org.nonprofit.volunteer.strategy;

import java.util.List;
import org.nonprofit.volunteer.model.Task;
import org.nonprofit.volunteer.model.Volunteer;
import org.nonprofit.volunteer.model.VolunteerService;

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
