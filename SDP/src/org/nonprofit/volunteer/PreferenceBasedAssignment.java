package org.nonprofit.volunteer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * STRATEGY pattern (concrete strategy): only available volunteers who
 * prefer the task's category qualify. Among them, someone who also has the
 * required skill wins; after that, the one with the fewest open tasks.
 */
public class PreferenceBasedAssignment implements AssignmentStrategy {

    @Override
    public Optional<Volunteer> pick(Task task, List<Volunteer> candidates) {
        Comparator<Volunteer> bySkill = Comparator.comparing(v -> !v.hasSkill(task.getRequiredSkill()));
        return candidates.stream()
                .filter(v -> v.isAvailableOn(task.getDay()))
                .filter(v -> v.prefers(task.getCategory()))
                .min(bySkill.thenComparingInt(Volunteer::openTaskCount));
    }

    @Override
    public String name() {
        return "Preference-based (likes this kind of task)";
    }
}
