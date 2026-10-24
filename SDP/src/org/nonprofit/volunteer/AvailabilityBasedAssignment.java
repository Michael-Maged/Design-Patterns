package org.nonprofit.volunteer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * STRATEGY pattern (concrete strategy): anyone available that day
 * qualifies, skill or not; the volunteer with the fewest open tasks is
 * chosen so the workload is spread out. Useful for general helper tasks.
 */
public class AvailabilityBasedAssignment implements AssignmentStrategy {

    @Override
    public Optional<Volunteer> pick(Task task, List<Volunteer> candidates) {
        return candidates.stream()
                .filter(v -> v.isAvailableOn(task.getDay()))
                .min(Comparator.comparingInt(Volunteer::openTaskCount));
    }

    @Override
    public String name() {
        return "Availability-based (available, lightest workload)";
    }
}
