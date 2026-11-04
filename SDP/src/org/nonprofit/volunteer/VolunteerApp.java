package org.nonprofit.volunteer;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.volunteer.model.Task;
import org.nonprofit.volunteer.model.Volunteer;
import org.nonprofit.volunteer.mvc.VolunteerController;
import org.nonprofit.volunteer.model.VolunteerService;
import org.nonprofit.volunteer.mvc.VolunteerView;

/**
 * Starts the Volunteers screen: builds the model, the view and the
 * controller and connects them (MVC). A few sample volunteers and tasks
 * are added first so the menus have something to show.
 * Run: java -cp bin org.nonprofit.volunteer.VolunteerApp
 */
public class VolunteerApp {

    public static void main(String[] args) {
        VolunteerService model = new VolunteerService();
        addSampleData(model);
        VolunteerView view = new VolunteerView();
        VolunteerController controller = new VolunteerController(model, view);
        controller.run();
    }

    static void addSampleData(VolunteerService model) {
        model.registerVolunteer(
                new Volunteer("Sara Mostafa", "sara@example.org", "+20110000001", ContactPreference.EMAIL)
                        .addSkill("first aid").addSkill("teaching")
                        .addAvailability(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)
                        .addPreference("health"));
        model.registerVolunteer(
                new Volunteer("Karim Fathy", "karim@example.org", "+20110000002")
                        .addSkill("first aid").addSkill("driving")
                        .addAvailability(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        .addPreference("logistics"));
        model.registerVolunteer(
                new Volunteer("Nour Adel", "nour@example.org", "+20110000003")
                        .addSkill("cooking")
                        .addAvailability(DayOfWeek.SATURDAY)
                        .addPreference("food"));

        // Karim already has some experience.
        Task delivery = model.addTask(new Task("Delivery run", "driving", DayOfWeek.SUNDAY,
                "logistics", "Neighbourhood Food Drive"));
        model.assign(delivery.getId());
        model.logHours(delivery.getId(), 6, LocalDate.of(2026, 10, 25));

        model.addTask(new Task("Medical tent", "first aid", DayOfWeek.SATURDAY, "health", "Neighbourhood Food Drive"));
        model.addTask(new Task("Kitchen helper", "cooking", DayOfWeek.SATURDAY, "food", "Neighbourhood Food Drive"));
    }
}
