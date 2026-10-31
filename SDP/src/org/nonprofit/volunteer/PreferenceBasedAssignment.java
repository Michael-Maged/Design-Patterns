package org.nonprofit.volunteer;

import java.util.List;

/**
 * STRATEGY pattern (concrete strategy): only available volunteers who
 * prefer the task's category qualify. Among them, someone who also has the
 * required skill wins; after that, the one with the fewest open tasks.
 */
public class PreferenceBasedAssignment implements AssignmentStrategy {

    @Override
    public Volunteer pick(Task task, List<Volunteer> candidates) {
        Volunteer best = null;
        for (Volunteer v : candidates) {
            if (!v.isAvailableOn(task.getDay()) || !v.prefers(task.getCategory())) {
                continue;
            }
            if (best == null || isBetter(v, best, task)) {
                best = v;
            }
        }
        return best;
    }

    private boolean isBetter(Volunteer v, Volunteer best, Task task) {
        boolean vSkilled = v.hasSkill(task.getRequiredSkill());
        boolean bestSkilled = best.hasSkill(task.getRequiredSkill());
        if (vSkilled != bestSkilled) {
            return vSkilled;
        }
        return v.openTaskCount() < best.openTaskCount();
    }

    @Override
    public String name() {
        return "Preference-based (likes this kind of task)";
    }
}
