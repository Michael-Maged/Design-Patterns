package org.nonprofit.volunteer.strategy;

import java.util.List;
import org.nonprofit.volunteer.model.Task;
import org.nonprofit.volunteer.model.Volunteer;

/**
 * STRATEGY pattern (concrete strategy): only volunteers who have the
 * required skill and are available that day qualify; the most experienced
 * one (most hours logged) is chosen.
 */
public class SkillBasedAssignment implements AssignmentStrategy {

    @Override
    public Volunteer pick(Task task, List<Volunteer> candidates) {
        Volunteer best = null;
        for (Volunteer v : candidates) {
            if (!v.hasSkill(task.getRequiredSkill()) || !v.isAvailableOn(task.getDay())) {
                continue;
            }
            if (best == null || v.getTotalHours() > best.getTotalHours()) {
                best = v;
            }
        }
        return best;
    }

    @Override
    public String name() {
        return "Skill-based (qualified, most experienced)";
    }
}
