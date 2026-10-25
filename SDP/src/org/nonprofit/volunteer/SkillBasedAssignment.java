package org.nonprofit.volunteer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * STRATEGY pattern (concrete strategy): only volunteers who have the
 * required skill and are available that day qualify; the most experienced
 * one (most hours logged) is chosen.
 */
public class SkillBasedAssignment implements AssignmentStrategy {

    @Override
    public Optional<Volunteer> pick(Task task, List<Volunteer> candidates) {
        return candidates.stream()
                .filter(v -> v.hasSkill(task.getRequiredSkill()))
                .filter(v -> v.isAvailableOn(task.getDay()))
                .max(Comparator.comparingDouble(Volunteer::getTotalHours));
    }

    @Override
    public String name() {
        return "Skill-based (qualified, most experienced)";
    }
}
