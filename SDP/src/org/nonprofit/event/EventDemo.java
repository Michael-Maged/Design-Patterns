package org.nonprofit.event;

import java.time.Duration;
import java.time.LocalDateTime;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEventType;

/**
 * Stand-alone walkthrough of the Events module, showing each pattern.
 * Run: java -cp bin org.nonprofit.event.EventDemo
 */
public class EventDemo {

    public static void main(String[] args) {
        // Until the Communication module is merged, a simple subscriber
        // stands in for it and prints what it would send.
        EventBus bus = EventBus.getInstance();
        for (SystemEventType type : SystemEventType.values()) {
            bus.subscribe(type, e -> System.out.println("    [EventBus] " + e));
        }

        EventService service = new EventService();
        LocalDateTime now = LocalDateTime.of(2026, 11, 1, 9, 0);

        heading("1. FACTORY - creating events by type and from user input");
        Fundraiser gala = (Fundraiser) service.createEvent(EventType.FUNDRAISER,
                "Winter Charity Gala", now.plusDays(10).withHour(19), "Grand Hall", 100);
        gala.setFundraisingGoal(20_000);
        Workshop firstAid = (Workshop) service.createEvent("workshop",
                "First Aid Basics", now.plusHours(8), "Room 4B", 2);
        firstAid.setTopic("CPR & first aid");
        firstAid.setInstructor("Dr. Salma Adel");
        OutreachProgram foodDrive = (OutreachProgram) service.createEvent("outreach program",
                "Neighbourhood Food Drive", now.plusDays(3), "Community Center", 50);
        foodDrive.setTargetCommunity("Families in Ard El Lewa");
        service.listEvents().forEach(e -> System.out.println("  " + e.summary()));

        Attendee mona = new Attendee("Mona Hassan", "mona@example.org", "+20100000001", ContactPreference.EMAIL);
        Attendee omar = new Attendee("Omar Khaled", "omar@example.org", "+20100000002", ContactPreference.SMS);
        Attendee laila = new Attendee("Laila Samir", "laila@example.org", "+20100000003", ContactPreference.EMAIL);
        Attendee youssef = new Attendee("Youssef Ali", "youssef@example.org", "+20100000004", ContactPreference.SMS);

        heading("2. DECORATOR - tickets with stacked add-ons");
        print(service.register(gala.getId(), mona));
        print(service.register(gala.getId(), omar, TicketAddOn.VIP));
        print(service.register(gala.getId(), laila, TicketAddOn.VIP, TicketAddOn.MEAL, TicketAddOn.PARKING));
        Ticket manual = new ParkingTicket(new MealTicket(new BasicTicket(gala, youssef)));
        System.out.println("  Built by hand: " + manual);

        heading("3. Capacity and waitlist (workshop holds 2)");
        print(service.register(firstAid.getId(), mona, TicketAddOn.MEAL));
        print(service.register(firstAid.getId(), omar));
        print(service.register(firstAid.getId(), laila, TicketAddOn.PARKING));
        print(service.register(firstAid.getId(), youssef));
        print(service.register(firstAid.getId(), laila));
        System.out.println("  Omar cancels -> first person waiting is promoted:");
        service.cancelRegistration(firstAid.getId(), omar);
        System.out.println("  " + firstAid.summary());

        heading("4. OBSERVER - attendees and the EventBus hear about changes");
        service.reschedule(gala.getId(), gala.getDateTime().plusDays(1));
        service.relocate(gala.getId(), "Nile View Ballroom");

        heading("5. Reminders for events in the next 24 hours");
        var reminded = service.sendUpcomingReminders(now, Duration.ofHours(24));
        System.out.println("  Reminded: " + reminded);

        heading("6. Attendance tracking");
        service.checkIn(gala.getId(), mona);
        service.checkIn(gala.getId(), laila);
        gala.recordPledge(7_500);
        gala.recordPledge(2_500);
        System.out.println(service.attendanceReport(gala.getId()));
        System.out.println(service.attendanceReport(firstAid.getId()));

        heading("7. Cancelling an event notifies everyone registered");
        service.register(foodDrive.getId(), youssef);
        service.cancelEvent(foodDrive.getId());
        try {
            service.register(foodDrive.getId(), mona);
        } catch (IllegalStateException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        heading("8. SINGLETON - one EventBus for the whole system");
        System.out.println("  Same instance? " + (EventBus.getInstance() == bus));

        heading("Mona's inbox");
        mona.getInbox().forEach(n -> System.out.println("  - " + n));
    }

    private static void print(RegistrationResult result) {
        switch (result.status()) {
            case REGISTERED -> System.out.println("  REGISTERED  " + result.ticket().getHolder().getName()
                    + ": " + result.ticket());
            case WAITLISTED -> System.out.println("  WAITLISTED  position " + result.waitlistPosition());
            default -> System.out.println("  " + result.status());
        }
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
