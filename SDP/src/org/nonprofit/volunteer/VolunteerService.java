package org.nonprofit.volunteer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;

/**
 * Entry point of the Volunteers module (the Facade in core calls this).
 * Keeps volunteers and tasks, assigns tasks using the current
 * {@link AssignmentStrategy}, logs hours, ranks volunteers and issues
 * {@link Certificate}s.
 */
public class VolunteerService {
    private final Map<String, Volunteer> volunteers = new LinkedHashMap<>();
    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private final EventBus bus = EventBus.getInstance();
    private AssignmentStrategy strategy = new SkillBasedAssignment();

    // ---- Volunteers ----

    public Volunteer registerVolunteer(Volunteer volunteer) {
        volunteers.put(volunteer.getId(), volunteer);
        return volunteer;
    }

    /** @return the volunteer, or null if there is none with that id */
    public Volunteer findVolunteer(String volunteerId) {
        return volunteers.get(volunteerId);
    }

    public List<Volunteer> listVolunteers() {
        return new ArrayList<>(volunteers.values());
    }

    /** Removes a volunteer; their unfinished tasks go back to the pool. */
    public boolean removeVolunteer(String volunteerId) {
        Volunteer volunteer = volunteers.remove(volunteerId);
        if (volunteer == null) {
            return false;
        }
        for (Task task : volunteer.getTasks()) {
            if (!task.isCompleted()) {
                unassign(task.getId());
            }
        }
        return true;
    }

    // ---- Tasks ----

    public Task addTask(Task task) {
        tasks.put(task.getId(), task);
        return task;
    }

    /** @return the task, or null if there is none with that id */
    public Task findTask(String taskId) {
        return tasks.get(taskId);
    }

    public List<Task> listTasks() {
        return new ArrayList<>(tasks.values());
    }

    public List<Task> openTasks() {
        List<Task> open = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (!task.isAssigned()) {
                open.add(task);
            }
        }
        return open;
    }

    // ---- Strategy ----

    public void setStrategy(AssignmentStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy is required");
        }
        this.strategy = strategy;
    }

    public void setStrategy(AssignmentMode mode) {
        setStrategy(mode.create());
    }

    public AssignmentStrategy getStrategy() {
        return strategy;
    }

    // ---- Assignment ----

    /** Who the current strategy would choose (null if nobody fits), without assigning anyone. */
    public Volunteer suggest(String taskId) {
        Task task = requireTask(taskId);
        return strategy.pick(task, freeOn(task));
    }

    /**
     * Assigns the task to the volunteer the current strategy picks and
     * publishes VOLUNTEER_ASSIGNED on the EventBus.
     *
     * @return the assigned volunteer, or null if nobody fits
     */
    public Volunteer assign(String taskId) {
        Task task = requireTask(taskId);
        if (task.isAssigned()) {
            throw new IllegalStateException(task.getName() + " is already assigned to "
                    + task.getAssignedVolunteer().getName());
        }
        Volunteer chosen = strategy.pick(task, freeOn(task));
        if (chosen != null) {
            task.setAssignedVolunteer(chosen);
            chosen.assign(task);
            bus.publish(new SystemEvent(SystemEventType.VOLUNTEER_ASSIGNED,
                    "You are assigned to " + task.getName() + " on " + task.getDay(), chosen, task));
        }
        return chosen;
    }

    /** Assigns every open task in order; returns how many were filled. */
    public int assignAllOpen() {
        int filled = 0;
        for (Task task : openTasks()) {
            if (assign(task.getId()) != null) {
                filled++;
            }
        }
        return filled;
    }

    public void unassign(String taskId) {
        Task task = requireTask(taskId);
        if (task.isCompleted()) {
            throw new IllegalStateException(task.getName() + " is already completed");
        }
        Volunteer volunteer = task.getAssignedVolunteer();
        if (volunteer != null) {
            volunteer.unassign(task);
            task.setAssignedVolunteer(null);
        }
    }

    /** One task per day: anyone with an unfinished task that day is not a candidate. */
    private List<Volunteer> freeOn(Task task) {
        List<Volunteer> free = new ArrayList<>();
        for (Volunteer v : volunteers.values()) {
            if (!v.isBookedOn(task.getDay())) {
                free.add(v);
            }
        }
        return free;
    }

    // ---- Hours ----

    /** Records the hours the assigned volunteer worked and marks the task completed. */
    public void logHours(String taskId, double hours, LocalDate date) {
        Task task = requireTask(taskId);
        if (!task.isAssigned()) {
            throw new IllegalStateException(task.getName() + " has no volunteer");
        }
        if (task.isCompleted()) {
            throw new IllegalStateException("Hours for " + task.getName() + " were already logged");
        }
        // NaN ("not a number") would slip past "hours <= 0", so it is checked too.
        if (Double.isNaN(hours) || hours <= 0) {
            throw new IllegalArgumentException("Hours must be a positive number");
        }
        task.getAssignedVolunteer().recordHours(new HourEntry(task, hours, date));
        task.markCompleted();
    }

    /** Volunteers ranked by total hours, most first (equal hours keep their registration order). */
    public List<Volunteer> leaderboard(int top) {
        if (top < 0) {
            throw new IllegalArgumentException("Leaderboard size cannot be negative");
        }
        List<Volunteer> ranked = new ArrayList<>();
        for (Volunteer v : volunteers.values()) {
            // Insert before the first volunteer with fewer hours.
            int position = ranked.size();
            for (int i = 0; i < ranked.size(); i++) {
                if (ranked.get(i).getTotalHours() < v.getTotalHours()) {
                    position = i;
                    break;
                }
            }
            ranked.add(position, v);
        }
        return new ArrayList<>(ranked.subList(0, Math.min(top, ranked.size())));
    }

    public String leaderboardReport(int top) {
        StringBuilder sb = new StringBuilder("Volunteer leaderboard\n");
        int rank = 1;
        for (Volunteer v : leaderboard(top)) {
            sb.append(String.format("  %d. %-14s %6.1f h%n", rank++, v.getName(), v.getTotalHours()));
        }
        return sb.toString().stripTrailing();
    }

    // ---- Certificates (Template Method) ----

    public String issueHoursCertificate(String volunteerId, LocalDate date) {
        return new HoursCertificate(requireVolunteer(volunteerId), date).issue();
    }

    public String issueParticipationCertificate(String volunteerId, String eventName, LocalDate date) {
        return new EventParticipationCertificate(requireVolunteer(volunteerId), eventName, date).issue();
    }

    private Volunteer requireVolunteer(String volunteerId) {
        Volunteer volunteer = volunteers.get(volunteerId);
        if (volunteer == null) {
            throw new IllegalArgumentException("No volunteer with id " + volunteerId);
        }
        return volunteer;
    }

    private Task requireTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("No task with id " + taskId);
        }
        return task;
    }
}
