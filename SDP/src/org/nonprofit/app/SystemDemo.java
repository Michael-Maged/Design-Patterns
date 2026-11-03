package org.nonprofit.app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.nonprofit.beneficiary.Need;
import org.nonprofit.communication.SentMessage;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.donation.Donation;
import org.nonprofit.donation.DonationDetails;
import org.nonprofit.donation.Donor;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.event.RegistrationResult;
import org.nonprofit.volunteer.Task;
import org.nonprofit.volunteer.Volunteer;

/**
 * The scripted, non-interactive walkthrough run by {@code App --demo}.
 * Every step goes through the {@link NonProfitFacade} and prints evidence of
 * each pattern: Singleton, Factory, Strategy, Decorator, Observer, Template
 * Method and Facade. Dates are fixed, so the output is the same every run.
 */
public final class SystemDemo {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 9, 0);

    private final NonProfitFacade facade;
    private int section;

    private SystemDemo(NonProfitFacade facade) {
        this.facade = facade;
    }

    public static void run(NonProfitFacade facade) {
        new SystemDemo(facade).walkthrough();
    }

    private void walkthrough() {
        singleton();
        donations();
        volunteers();
        events();
        beneficiaries();
        communication();
        summary();
    }

    // ---- Singleton ----

    private void singleton() {
        heading("SINGLETON - one DataStore and one EventBus for the whole system");
        DataStore first = DataStore.getInstance();
        DataStore second = DataStore.getInstance();
        System.out.println("  DataStore.getInstance() twice, same object? " + (first == second));
        System.out.println("  Same DonationService both times? "
                + (first.getDonationService() == second.getDonationService()));
        System.out.println("  EventBus.getInstance() twice, same object? "
                + (EventBus.getInstance() == EventBus.getInstance()));
        System.out.println("  CommunicationService is created once in DataStore, so each bus message is sent once.");
    }

    // ---- Donations ----

    private void donations() {
        heading("FACTORY METHOD - donations created from text; OBSERVER sends the thank-you");
        Donor hany = facade.registerDonor("Hany Mansour", "hany@example.org", "+20120000001", ContactPreference.EMAIL);
        Donor rania = facade.registerDonor("Rania Kamel", "rania@example.org", "+20120000002", ContactPreference.SMS);
        Donor bakery = facade.registerDonor("El Nour Bakery", "orders@elnour.example", "+20120000003",
                ContactPreference.EMAIL);
        facade.createCampaign("Winter Blankets", 15_000);

        int before = logSize();
        Donation online = facade.recordDonation(hany.getId(), "online", TODAY.minusMonths(9),
                DonationDetails.of(500).reference("PAY-88213").recurring());
        Donation check = facade.recordDonation(rania.getId(), "check", TODAY.minusMonths(2),
                DonationDetails.of(2_000).reference("004517").bank("National Bank of Egypt").campaign("Winter Blankets"));
        Donation goods = facade.recordDonation(bakery.getId(), "in-kind", TODAY.minusDays(5),
                DonationDetails.of(1_200).item("300 loaves of bread for the food drive"));
        facade.recordDonation(hany.getId(), "online", TODAY.minusDays(1),
                DonationDetails.of(4_500).reference("PAY-90410").campaign("winter blankets"));
        sent(before, 4);
        for (Donation d : List.of(online, check, goods)) {
            System.out.println("  text -> " + d.getClass().getSimpleName() + ": " + d.summary());
        }
        try {
            facade.recordDonation(hany.getId(), "crypto", TODAY, DonationDetails.of(10));
        } catch (IllegalArgumentException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        heading("TEMPLATE METHOD - a receipt and a tax statement share one layout");
        System.out.println(facade.receipt(online.getId(), TODAY));
        System.out.println(facade.taxStatement(hany.getId(), 2026, TODAY));
        System.out.println(facade.donationSummary());
    }

    // ---- Volunteers ----

    private void volunteers() {
        heading("STRATEGY - swapping the assignment rule changes who gets the task");
        Volunteer sara = facade.registerVolunteer("Sara Mostafa", "sara@example.org", "+20110000001",
                ContactPreference.EMAIL, List.of("first aid", "teaching"),
                List.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), List.of("health"));
        Volunteer karim = facade.registerVolunteer("Karim Fathy", "karim@example.org", "+20110000002",
                ContactPreference.SMS, List.of("first aid", "driving"),
                List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), List.of("logistics"));
        facade.registerVolunteer("Nour Adel", "nour@example.org", "+20110000003",
                ContactPreference.SMS, List.of("cooking"), List.of(DayOfWeek.SATURDAY), List.of("food"));

        System.out.println("  Some history first (each assignment is sent to the volunteer):");
        Task delivery = facade.addTask("Delivery run", "driving", DayOfWeek.SUNDAY, "logistics", "Neighbourhood Food Drive");
        Task reading = facade.addTask("Reading class", "teaching", DayOfWeek.MONDAY, "education", null);
        facade.assignTask(delivery.getId());
        facade.logHours(delivery.getId(), 6, TODAY.minusDays(7));
        facade.assignTask(reading.getId());
        facade.logHours(reading.getId(), 3, TODAY.minusDays(6));
        facade.assignTask(facade.addTask("Van driver", "driving", DayOfWeek.SUNDAY, "logistics", null).getId());
        facade.assignTask(facade.addTask("Tutor", "teaching", DayOfWeek.MONDAY, "education", null).getId());

        Task tent = facade.addTask("Medical tent", "first aid", DayOfWeek.SATURDAY, "health", "Neighbourhood Food Drive");
        System.out.println("  " + tent.summary());
        for (String rule : List.of("skill", "availability", "preference")) {
            String name = facade.setAssignmentRule(rule);
            System.out.printf("  %-52s -> %s%n", name, nameOf(facade.suggestVolunteer(tent.getId())));
        }
        System.out.println("  Assigned with the preference rule: " + nameOf(facade.assignTask(tent.getId())));
        facade.logHours(tent.getId(), 5, TODAY.plusDays(6));

        heading("TEMPLATE METHOD - leaderboard and certificates");
        System.out.println(facade.leaderboard(3));
        System.out.println(facade.hoursCertificate(karim.getId(), TODAY.plusDays(7)));
        System.out.println(facade.participationCertificate(sara.getId(), "Neighbourhood Food Drive", TODAY.plusDays(7)));
    }

    // ---- Events ----

    private void events() {
        heading("FACTORY - events created from text");
        Event gala = facade.createEvent("fundraiser", "Winter Charity Gala", NOW.plusDays(10).withHour(19),
                "Grand Hall", 100);
        Event workshop = facade.createEvent("workshop", "First Aid Basics", NOW.plusHours(8), "Room 4B", 2);
        Event drive = facade.createEvent("outreach program", "Neighbourhood Food Drive", NOW.plusDays(3),
                "Community Center", 50);
        for (Event e : facade.listEvents()) {
            System.out.println("  text -> " + e.getClass().getSimpleName() + ": " + e.summary());
        }

        heading("DECORATOR - ticket add-ons stack on the price; a full event uses the waitlist");
        Attendee mona = facade.addAttendee("Mona Hassan", "mona@example.org", "+20100000001", ContactPreference.EMAIL);
        Attendee omar = facade.addAttendee("Omar Khaled", "omar@example.org", "+20100000002", ContactPreference.SMS);
        Attendee laila = facade.addAttendee("Laila Samir", "laila@example.org", "+20100000003", ContactPreference.EMAIL);
        Attendee youssef = facade.addAttendee("Youssef Ali", "youssef@example.org", "+20100000004", ContactPreference.SMS);
        register(gala, mona, List.of());
        register(gala, omar, List.of("vip"));
        register(gala, laila, List.of("vip", "meal", "parking"));
        System.out.println("  The workshop holds 2:");
        register(workshop, mona, List.of("meal"));
        register(workshop, omar, List.of());
        register(workshop, youssef, List.of());
        System.out.println("  Omar cancels the workshop, so Youssef is promoted from the waitlist:");
        facade.cancelRegistration(workshop.getId(), omar.getId());

        heading("OBSERVER - changes and reminders reach every attendee");
        int before = logSize();
        facade.rescheduleEvent(gala.getId(), gala.getDateTime().plusDays(1));
        sent(before, 3);
        List<Event> reminded = facade.sendUpcomingReminders(NOW, 24);
        System.out.println("  Reminded: " + reminded);
        facade.checkIn(gala.getId(), mona.getId());
        facade.checkIn(gala.getId(), laila.getId());
        System.out.println(facade.attendanceReport(gala.getId()));
        facade.registerAttendee(drive.getId(), youssef.getId(), List.of());
        facade.cancelEvent(drive.getId());
    }

    // ---- Beneficiaries ----

    private void beneficiaries() {
        heading("OBSERVER - a HIGH need reaches every volunteer (Audience wired by DataStore)");
        String amal = facade.registerBeneficiary("Amal Saeed", null, "+20120000011", ContactPreference.SMS,
                "Ard El Lewa", 5).getId();
        String hassan = facade.registerBeneficiary("Hassan Ali", "hassan@example.org", "+20120000012",
                ContactPreference.EMAIL, "Imbaba", 2).getId();
        String rania = facade.registerBeneficiary("Rania Youssef", null, "+20120000013", ContactPreference.SMS,
                "Ard El Lewa", 4).getId();
        facade.addNeed(amal, "food parcel", 4, "medium", TODAY.minusDays(9));
        facade.addNeed(hassan, "food parcel", 3, "low", TODAY.minusDays(12));
        int before = logSize();
        facade.addNeed(rania, "food parcel", 5, "high", TODAY.minusDays(2));
        sent(before, facade.listVolunteers().size());

        heading("STRATEGY - the same 6 food parcels under three allocation rules");
        for (String rule : List.of("urgency", "first come", "equal")) {
            String name = facade.setAllocationRule(rule);
            System.out.printf("  %-26s -> %s%n", name, describe(facade.previewDistribution("food parcel", 6)));
        }
        System.out.println("  Distributing with the equal-share rule:");
        facade.distribute("Winter Food Program", "food parcel", 6, TODAY);

        heading("TEMPLATE METHOD - two impact reports, one layout");
        System.out.println(facade.programImpactReport("Winter Food Program", TODAY.plusDays(7)));
        System.out.println(facade.progressReport(amal, TODAY.plusDays(7)));
    }

    // ---- Communication ----

    private void communication() {
        heading("DECORATOR + STRATEGY - an urgent broadcast to all donors, then all volunteers by SMS");
        int reached = facade.broadcast("donors", "Winter appeal", "We need blankets by Friday.", true, null);
        System.out.println("  Reached " + reached + " donor(s), each on their own channel"
                + " (urgent tag + signature + e-mail footer stacked on the text).");
        reached = facade.broadcast(BroadcastGroup.VOLUNTEERS, "Road closed", "Meet at the side gate on Saturday.",
                false, ContactPreference.SMS);
        System.out.println("  Reached " + reached + " volunteer(s), all forced onto SMS.");

        heading("Message log (every message sent once)");
        for (SentMessage m : facade.messageLog()) {
            System.out.printf("  %-9s -> %-16s %s%n", m.getChannel(), m.getRecipient(), m.getSubject());
        }
    }

    // ---- Facade ----

    private void summary() {
        heading("FACADE - everything above went through NonProfitFacade");
        System.out.println("  " + facade.listDonors().size() + " donors, " + facade.listDonations().size()
                + " donations, " + facade.listVolunteers().size() + " volunteers, "
                + facade.listTasks().size() + " tasks, " + facade.listEvents().size() + " events, "
                + facade.listAttendees().size() + " attendees, " + facade.listBeneficiaries().size()
                + " beneficiaries, " + facade.messageLog().size() + " messages sent.");
    }

    // ---- Helpers ----

    private int logSize() {
        return facade.messageLog().size();
    }

    /** Shows how many messages Communication sent since {@code before}, against how many were expected. */
    private void sent(int before, int expected) {
        int count = logSize() - before;
        System.out.println("  CommunicationService sent " + count + " message(s); expected " + expected
                + (count == expected ? " - no duplicates." : " - MISMATCH!"));
    }

    private void register(Event event, Attendee attendee, List<String> addOns) {
        RegistrationResult result = facade.registerAttendee(event.getId(), attendee.getId(), addOns);
        switch (result.status()) {
            case REGISTERED:
                System.out.println("  REGISTERED  " + attendee.getName() + ": " + result.ticket());
                break;
            case WAITLISTED:
                System.out.println("  WAITLISTED  " + attendee.getName() + " at position " + result.waitlistPosition());
                break;
            default:
                System.out.println("  " + result.status() + "  " + attendee.getName());
        }
    }

    private static String describe(Map<Need, Integer> shares) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Need, Integer> share : shares.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(share.getKey().getBeneficiary().getName()).append(' ').append(share.getValue());
        }
        return sb.toString();
    }

    private static String nameOf(Volunteer volunteer) {
        return volunteer == null ? "nobody fits" : volunteer.getName();
    }

    private void heading(String title) {
        System.out.println();
        System.out.println("=== " + (++section) + ". " + title + " ===");
    }
}
