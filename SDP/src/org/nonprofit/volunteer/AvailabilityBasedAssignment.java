package org.nonprofit.volunteer;

import java.util.List;

/**
 * STRATEGY pattern (concrete strategy): anyone available that day
 * qualifies, skill or not; the volunteer with the fewest open tasks is
 * chosen so the workload is spread out. Useful for general helper tasks.
 */
public class AvailabilityBasedAssignment implements AssignmentStrategy {

    @Override
    public Volunteer pick(Task task, List<Volunteer> candidates) {
        Volunteer best = null;
        for (Volunteer v : candidates) {
            if (!v.isAvailableOn(task.getDay())) {
                continue;
            }
            if (best == null || v.openTaskCount() < best.openTaskCount()) {
                best = v;
            }
        }
        return best;
    }

    @Override
    public String name() {
        return "Availability-based (available, lightest workload)";
    }
}
