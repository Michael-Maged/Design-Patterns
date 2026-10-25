package org.nonprofit.volunteer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEventType;

/**
 * Stand-alone walkthrough of the Volunteers module, showing each pattern.
 * Run: java -cp bin org.nonprofit.volunteer.VolunteerDemo
 */
public class VolunteerDemo {

    public static void main(String[] args) {
        // Until the Communication module is merged, a simple subscriber
        // stands in for it and prints what it would send.
        EventBus.getInstance().subscribe(SystemEventType.VOLUNTEER_ASSIGNED,
                e -> System.out.println("    [EventBus] " + e));

        VolunteerService service = new VolunteerService();
        LocalDate today = LocalDate.of(2026, 11, 1);

        heading("1. Volunteer profiles: skills, availability, preferences");
        Volunteer sara = service.registerVolunteer(
                new Volunteer("Sara Mostafa", "sara@example.org", "+20110000001", ContactPreference.EMAIL)
                        .addSkill("First Aid").addSkill("Teaching")
                        .addAvailability(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)
                        .addPreference("health"));
        Volunteer karim = service.registerVolunteer(
                new Volunteer("Karim Fathy", "karim@example.org", "+20110000002")
                        .addSkill("first aid").addSkill("driving")
                        .addAvailability(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        .addPreference("logistics"));
        Volunteer nour = service.registerVolunteer(
                new Volunteer("Nour Adel", "nour@example.org", "+20110000003")
                        .addSkill("cooking")
                        .addAvailability(DayOfWeek.SATURDAY)
                        .addPreference("food"));

        // Some history: past tasks with hours, and work already booked this week.
        Task delivery = service.addTask(new Task("Delivery run", "driving", DayOfWeek.SUNDAY,
                "logistics", "Neighbourhood Food Drive"));
        Task reading = service.addTask(new Task("Reading class", "teaching", DayOfWeek.MONDAY, "education"));
        service.assign(delivery.getId());
        service.logHours(delivery.getId(), 6, today.minusDays(7));
        service.assign(reading.getId());
        service.logHours(reading.getId(), 3, today.minusDays(6));
        service.assign(service.addTask(new Task("Van driver", "driving", DayOfWeek.SUNDAY, "logistics")).getId());
        service.assign(service.addTask(new Task("Tutor", "teaching", DayOfWeek.MONDAY, "education")).getId());
        service.listVolunteers().forEach(v -> System.out.println("  " + v.summary()));

        heading("2. STRATEGY - the same task, three strategies, three answers");
        Task medicalTent = service.addTask(new Task("Medical tent", "first aid", DayOfWeek.SATURDAY,
                "health", "Neighbourhood Food Drive"));
        System.out.println("  " + medicalTent.summary());
        List<AssignmentStrategy> strategies = List.of(
                new SkillBasedAssignment(), new AvailabilityBasedAssignment(), new PreferenceBasedAssignment());
        for (AssignmentStrategy strategy : strategies) {
            service.setStrategy(strategy);
            System.out.printf("  %-52s -> %s%n", strategy.name(), name(service.suggest(medicalTent.getId())));
        }

        heading("3. Strategy chosen at runtime from user input (\"preference\")");
        service.setStrategy(AssignmentMode.fromString("preference"));
        System.out.println("  Assigned: " + name(service.assign(medicalTent.getId())));

        heading("4. Filling the rest of Saturday with the skill-based strategy");
        service.setStrategy(AssignmentMode.SKILL);
        service.addTask(new Task("Kitchen helper", "cooking", DayOfWeek.SATURDAY, "food", "Neighbourhood Food Drive"));
        service.addTask(new Task("Medic backup", "first aid", DayOfWeek.SATURDAY, "health", "Neighbourhood Food Drive"));
        Task extraMedic = service.addTask(new Task("Second medic", "first aid", DayOfWeek.SATURDAY, "health"));
        System.out.println("  Filled " + service.assignAllOpen() + " task(s); nobody is free for "
                + extraMedic + " (one task per day each)");
        service.listTasks().forEach(t -> System.out.println("  " + t.summary()));

        heading("5. Logging hours and the leaderboard");
        for (Task task : service.listTasks()) {
            if (task.isAssigned() && !task.isCompleted() && task.getDay() == DayOfWeek.SATURDAY) {
                service.logHours(task.getId(), task.getName().startsWith("Medic") ? 5 : 4, today.plusDays(6));
            }
        }
        System.out.println(service.leaderboardReport(3));

        heading("6. TEMPLATE METHOD - two certificates, one layout");
        System.out.println(service.issueHoursCertificate(karim.getId(), today.plusDays(7)));
        System.out.println(service.issueParticipationCertificate(sara.getId(),
                "Neighbourhood Food Drive", today.plusDays(7)));
        Volunteer newcomer = service.registerVolunteer(new Volunteer("Hana Ibrahim", "hana@example.org", "+20110000004"));
        try {
            service.issueHoursCertificate(newcomer.getId(), today.plusDays(7));
        } catch (IllegalStateException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        heading("7. Removing a volunteer frees their open tasks");
        service.removeVolunteer(nour.getId());
        service.removeVolunteer(karim.getId());
        service.openTasks().forEach(t -> System.out.println("  back in the pool: " + t.summary()));
    }

    private static String name(Optional<Volunteer> volunteer) {
        return volunteer.map(Volunteer::getName).orElse("nobody fits");
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
