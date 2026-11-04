package org.nonprofit.app;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.nonprofit.beneficiary.strategy.AllocationMode;
import org.nonprofit.beneficiary.model.Beneficiary;
import org.nonprofit.beneficiary.mvc.BeneficiaryController;
import org.nonprofit.beneficiary.model.BeneficiaryService;
import org.nonprofit.beneficiary.mvc.BeneficiaryView;
import org.nonprofit.beneficiary.model.Need;
import org.nonprofit.beneficiary.model.Urgency;
import org.nonprofit.communication.mvc.CommunicationController;
import org.nonprofit.communication.model.CommunicationService;
import org.nonprofit.communication.mvc.CommunicationView;
import org.nonprofit.communication.model.SentMessage;
import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.core.observer.EventBus;
import org.nonprofit.donation.model.Campaign;
import org.nonprofit.donation.factorymethod.Donation;
import org.nonprofit.donation.mvc.DonationController;
import org.nonprofit.donation.model.DonationForm;
import org.nonprofit.donation.model.DonationService;
import org.nonprofit.donation.model.DonationType;
import org.nonprofit.donation.mvc.DonationView;
import org.nonprofit.donation.model.Donor;
import org.nonprofit.event.observer.Attendee;
import org.nonprofit.event.observer.Event;
import org.nonprofit.event.mvc.EventController;
import org.nonprofit.event.model.EventService;
import org.nonprofit.event.mvc.EventView;
import org.nonprofit.event.model.RegistrationResult;
import org.nonprofit.event.decorator.TicketAddOn;
import org.nonprofit.volunteer.strategy.AssignmentMode;
import org.nonprofit.volunteer.model.Task;
import org.nonprofit.volunteer.model.Volunteer;
import org.nonprofit.volunteer.mvc.VolunteerController;
import org.nonprofit.volunteer.model.VolunteerService;
import org.nonprofit.volunteer.mvc.VolunteerView;
import org.nonprofit.app.singleton.BroadcastGroup;
import org.nonprofit.app.singleton.DataStore;
import org.nonprofit.app.mvc.MainMenuController;
import org.nonprofit.app.mvc.MainMenuView;

/**
 * The scripted, non-interactive walkthrough run by {@code App --demo}.
 * Every step calls the module services from the {@link DataStore} and prints
 * evidence of each pattern: Singleton, Factory / Factory Method, Strategy,
 * Decorator, Observer, Template Method and, last, MVC (the main menu driven
 * by scripted input). Dates are fixed, so the output is the same every run.
 * The model classes print nothing: after each step the demo prints what
 * Communication sent and the attendees' new inbox notes ({@link #show()}).
 */
public final class SystemDemo {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 9, 0);

    private final DataStore store;
    private final DonationService donations;
    private final VolunteerService volunteers;
    private final EventService events;
    private final BeneficiaryService beneficiaries;
    private final CommunicationService communication;
    private int section;

    private SystemDemo(DataStore store) {
        this.store = store;
        this.donations = store.getDonationService();
        this.volunteers = store.getVolunteerService();
        this.events = store.getEventService();
        this.beneficiaries = store.getBeneficiaryService();
        this.communication = store.getCommunicationService();
    }

    public static void run(DataStore store) {
        new SystemDemo(store).walkthrough();
    }

    private void walkthrough() {
        singleton();
        donations();
        volunteers();
        events();
        beneficiaries();
        communication();
        mvc();
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
        heading("FACTORY METHOD - each creator decides which donation to build; OBSERVER sends the thank-you");
        Donor hany = donations.addDonor("Hany Mansour", "hany@example.org", "+20120000001", ContactPreference.EMAIL);
        Donor rania = donations.addDonor("Rania Kamel", "rania@example.org", "+20120000002", ContactPreference.SMS);
        Donor bakery = donations.addDonor("El Nour Bakery", "orders@elnour.example", "+20120000003",
                ContactPreference.EMAIL);
        donations.addCampaign("Winter Blankets", 15_000);
        for (DonationType type : DonationType.values()) {
            System.out.println("  " + type + " is handled by " + donations.getCreator(type).getClass().getSimpleName());
        }

        int before = logSize();
        DonationForm onlineForm = new DonationForm(500);
        onlineForm.setReference("PAY-88213");
        onlineForm.setMonthly(true);
        Donation online = donations.donate(hany.getId(), "online", TODAY.minusMonths(9), onlineForm);
        DonationForm checkForm = new DonationForm(2_000);
        checkForm.setReference("004517");
        checkForm.setBank("National Bank of Egypt");
        checkForm.setCampaign("Winter Blankets");
        Donation check = donations.donate(rania.getId(), "check", TODAY.minusMonths(2), checkForm);
        DonationForm goodsForm = new DonationForm(1_200);
        goodsForm.setItem("300 loaves of bread for the food drive");
        Donation goods = donations.donate(bakery.getId(), "in-kind", TODAY.minusDays(5), goodsForm);
        DonationForm bigGift = new DonationForm(4_500);
        bigGift.setReference("PAY-90410");
        bigGift.setCampaign("winter blankets"); // case doesn't matter
        donations.donate(hany.getId(), "online", TODAY.minusDays(1), bigGift);
        sent(before, 4);
        for (Donation d : List.of(online, check, goods)) {
            System.out.println("  text -> " + d.getClass().getSimpleName() + ": " + d);
        }
        tryDonate(hany.getId(), "crypto", TODAY, new DonationForm(10));                       // unknown type
        DonationForm infinite = new DonationForm(Double.POSITIVE_INFINITY);
        infinite.setReference("PAY-1");
        tryDonate(hany.getId(), "online", TODAY, infinite);                                   // not a real amount
        DonationForm future = new DonationForm(50);
        future.setReference("PAY-2");
        tryDonate(hany.getId(), "online", TODAY.plusDays(1), future);                         // dated after today
        Donor updated = donations.updateDonor(rania.getId(), "Rania Kamel", "rania.kamel@example.org",
                "+20120000002", ContactPreference.EMAIL);
        System.out.println("  Updated contact: " + updated + " now " + updated.getEmail() + " by "
                + updated.getPreferredContact());

        heading("TEMPLATE METHOD - a receipt and a tax statement share one layout");
        System.out.println(donations.makeReceipt(online.getId(), TODAY));
        System.out.println(donations.makeTaxStatement(hany.getId(), 2026, TODAY));
        System.out.println(donations.summaryReport());
        for (Campaign campaign : donations.getCampaigns()) {
            System.out.println("  Campaign " + campaign);
        }
        System.out.println("  Top donors:");
        for (Donor donor : donations.getTopDonors(3)) {
            System.out.println("    " + donor.summary());
        }
    }

    /** Donations dated with the demo's fixed "today", so a later date is refused. */
    private void tryDonate(String donorId, String type, LocalDate date, DonationForm form) {
        try {
            donations.donate(donorId, DonationType.fromText(type), date, form, TODAY);
            System.out.println("  Accepted (this should not happen!)");
        } catch (IllegalArgumentException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
    }

    // ---- Volunteers ----

    private void volunteers() {
        heading("STRATEGY - swapping the assignment rule changes who gets the task");
        Volunteer sara = volunteers.registerVolunteer(new Volunteer("Sara Mostafa", "sara@example.org",
                "+20110000001", ContactPreference.EMAIL).addSkill("first aid").addSkill("teaching")
                .addAvailability(DayOfWeek.MONDAY, DayOfWeek.SATURDAY).addPreference("health"));
        Volunteer karim = volunteers.registerVolunteer(new Volunteer("Karim Fathy", "karim@example.org",
                "+20110000002", ContactPreference.SMS).addSkill("first aid").addSkill("driving")
                .addAvailability(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).addPreference("logistics"));
        volunteers.registerVolunteer(new Volunteer("Nour Adel", "nour@example.org", "+20110000003",
                ContactPreference.SMS).addSkill("cooking").addAvailability(DayOfWeek.SATURDAY).addPreference("food"));

        System.out.println("  Some history first (each assignment is sent to the volunteer):");
        Task delivery = addTask("Delivery run", "driving", DayOfWeek.SUNDAY, "logistics", "Neighbourhood Food Drive");
        Task reading = addTask("Reading class", "teaching", DayOfWeek.MONDAY, "education", null);
        volunteers.assign(delivery.getId());
        volunteers.logHours(delivery.getId(), 6, TODAY.minusDays(7));
        volunteers.assign(reading.getId());
        volunteers.logHours(reading.getId(), 3, TODAY.minusDays(6));
        volunteers.assign(addTask("Van driver", "driving", DayOfWeek.SUNDAY, "logistics", null).getId());
        volunteers.assign(addTask("Tutor", "teaching", DayOfWeek.MONDAY, "education", null).getId());
        show();

        Task tent = addTask("Medical tent", "first aid", DayOfWeek.SATURDAY, "health", "Neighbourhood Food Drive");
        System.out.println("  " + tent.summary());
        for (String rule : List.of("skill", "availability", "preference")) {
            volunteers.setStrategy(AssignmentMode.fromString(rule));
            System.out.printf("  %-52s -> %s%n", volunteers.getStrategy().name(),
                    nameOf(volunteers.suggest(tent.getId())));
        }
        Volunteer chosen = volunteers.assign(tent.getId());
        show();
        System.out.println("  Assigned with the preference rule: " + nameOf(chosen));
        volunteers.logHours(tent.getId(), 5, TODAY.plusDays(6));

        heading("TEMPLATE METHOD - leaderboard and certificates");
        System.out.println(volunteers.leaderboardReport(3));
        System.out.println(volunteers.issueHoursCertificate(karim.getId(), TODAY.plusDays(7)));
        System.out.println(volunteers.issueParticipationCertificate(sara.getId(), "Neighbourhood Food Drive",
                TODAY.plusDays(7)));
    }

    // ---- Events ----

    private void events() {
        heading("FACTORY - events created from text");
        Event gala = events.createEvent("fundraiser", "Winter Charity Gala", NOW.plusDays(10).withHour(19),
                "Grand Hall", 100);
        Event workshop = events.createEvent("workshop", "First Aid Basics", NOW.plusHours(8), "Room 4B", 2);
        Event drive = events.createEvent("outreach program", "Neighbourhood Food Drive", NOW.plusDays(3),
                "Community Center", 50);
        for (Event e : events.listEvents()) {
            System.out.println("  text -> " + e.getClass().getSimpleName() + ": " + e.summary());
        }

        heading("DECORATOR - ticket add-ons stack on the price; a full event uses the waitlist");
        Attendee mona = addAttendee("Mona Hassan", "mona@example.org", "+20100000001", ContactPreference.EMAIL);
        Attendee omar = addAttendee("Omar Khaled", "omar@example.org", "+20100000002", ContactPreference.SMS);
        Attendee laila = addAttendee("Laila Samir", "laila@example.org", "+20100000003", ContactPreference.EMAIL);
        Attendee youssef = addAttendee("Youssef Ali", "youssef@example.org", "+20100000004", ContactPreference.SMS);
        register(gala, mona);
        register(gala, omar, TicketAddOn.VIP);
        register(gala, laila, TicketAddOn.VIP, TicketAddOn.MEAL, TicketAddOn.PARKING);
        System.out.println("  The workshop holds 2:");
        register(workshop, mona, TicketAddOn.MEAL);
        register(workshop, omar);
        register(workshop, youssef);
        System.out.println("  Omar cancels the workshop, so Youssef is promoted from the waitlist:");
        events.cancelRegistration(workshop.getId(), omar);
        show();

        heading("OBSERVER - changes and reminders reach every attendee");
        int before = logSize();
        events.reschedule(gala.getId(), gala.getDateTime().plusDays(1));
        sent(before, 3);
        List<Event> reminded = events.sendUpcomingReminders(NOW, Duration.ofHours(24));
        show();
        System.out.println("  Reminded: " + reminded);
        events.checkIn(gala.getId(), mona);
        events.checkIn(gala.getId(), laila);
        System.out.println(events.attendanceReport(gala.getId()));
        events.register(drive.getId(), youssef, NOW);
        events.cancelEvent(drive.getId());
        show();
    }

    // ---- Beneficiaries ----

    private void beneficiaries() {
        heading("OBSERVER - a HIGH need reaches every volunteer (Audience wired by DataStore)");
        String amal = beneficiaries.registerBeneficiary(new Beneficiary("Amal Saeed", null, "+20120000011",
                ContactPreference.SMS, "Ard El Lewa", 5)).getId();
        String hassan = beneficiaries.registerBeneficiary(new Beneficiary("Hassan Ali", "hassan@example.org",
                "+20120000012", ContactPreference.EMAIL, "Imbaba", 2)).getId();
        String rania = beneficiaries.registerBeneficiary(new Beneficiary("Rania Youssef", null, "+20120000013",
                ContactPreference.SMS, "Ard El Lewa", 4)).getId();
        beneficiaries.addNeed(amal, "food parcel", 4, Urgency.MEDIUM, TODAY.minusDays(9));
        beneficiaries.addNeed(hassan, "food parcel", 3, Urgency.LOW, TODAY.minusDays(12));
        int before = logSize();
        beneficiaries.addNeed(rania, "food parcel", 5, Urgency.HIGH, TODAY.minusDays(2));
        sent(before, volunteers.listVolunteers().size());

        heading("STRATEGY - the same 6 food parcels under three allocation rules");
        for (String rule : List.of("urgency", "first come", "equal")) {
            beneficiaries.setStrategy(AllocationMode.fromString(rule));
            System.out.printf("  %-26s -> %s%n", beneficiaries.getStrategy().name(),
                    describe(beneficiaries.preview("food parcel", 6)));
        }
        System.out.println("  Distributing with the equal-share rule:");
        beneficiaries.distribute("Winter Food Program", "food parcel", 6, TODAY);
        show();

        heading("TEMPLATE METHOD - two impact reports, one layout");
        System.out.println(beneficiaries.programImpactReport("Winter Food Program", TODAY.plusDays(7)));
        System.out.println(beneficiaries.progressReport(amal, TODAY.plusDays(7)));
    }

    // ---- Communication ----

    private void communication() {
        heading("DECORATOR + STRATEGY - an urgent broadcast to all donors, then all volunteers by SMS");
        int reached = communication.broadcast(store.membersOf(BroadcastGroup.fromString("donors")),
                "Winter appeal", "We need blankets by Friday.", true, null);
        show();
        System.out.println("  Reached " + reached + " donor(s), each on their own channel"
                + " (urgent tag + signature + e-mail footer stacked on the text).");
        reached = communication.broadcast(store.membersOf(BroadcastGroup.VOLUNTEERS), "Road closed",
                "Meet at the side gate on Saturday.", false, ContactPreference.SMS);
        show();
        System.out.println("  Reached " + reached + " volunteer(s), all forced onto SMS.");

        heading("Message log (every message sent once)");
        for (SentMessage m : communication.getLog()) {
            System.out.printf("  %-9s -> %-16s %s%n", m.getChannel(), m.getRecipient(), m.getSubject());
        }
    }

    // ---- MVC ----

    /**
     * Drives the real main menu with scripted input: the controller reads
     * each choice from the view, asks the services (the model) and has the
     * view print the answer. Only screens that do not depend on today's
     * date are used, so the output stays the same on every run.
     */
    private void mvc() {
        heading("MVC - the main menu (MainMenuController + MainMenuView) over the module services");
        String typed = String.join("\n",
                "1", "1", "0",        // Donations > List donors > Back
                "abc",                // a bad choice is refused and the menu comes back
                "0") + "\n";          // Exit
        System.out.println("  Scripted input: " + typed.replace("\n", " ").trim());
        Scanner in = new Scanner(typed);
        DonationController donationScreen = new DonationController(donations, new DonationView(in), true,
                communication::takeOutput);
        VolunteerController volunteerScreen = new VolunteerController(volunteers,
                new VolunteerView(in, System.out), true, communication::takeOutput);
        EventController eventScreen = new EventController(events, new EventView(in, System.out), true,
                communication::takeOutput);
        BeneficiaryController beneficiaryScreen = new BeneficiaryController(beneficiaries,
                new BeneficiaryView(in, System.out), true, communication::takeOutput);
        CommunicationController communicationScreen = new CommunicationController(communication,
                new CommunicationView(in, System.out), true, store::membersOf, store::findPerson,
                store::eventAudience);
        new MainMenuController(new MainMenuView(in, System.out), donationScreen, volunteerScreen,
                eventScreen, beneficiaryScreen, communicationScreen).run();

        System.out.println();
        System.out.println("  " + donations.getDonors().size() + " donors, " + donations.getDonations().size()
                + " donations, " + volunteers.listVolunteers().size() + " volunteers, "
                + volunteers.listTasks().size() + " tasks, " + events.listEvents().size() + " events, "
                + events.listAttendees().size() + " attendees, " + beneficiaries.listBeneficiaries().size()
                + " beneficiaries, " + communication.getLog().size() + " messages sent.");
    }

    // ---- Helpers ----

    private Task addTask(String name, String skill, DayOfWeek day, String category, String eventName) {
        return volunteers.addTask(new Task(name, skill, day, category, eventName));
    }

    private Attendee addAttendee(String name, String email, String phone, ContactPreference contact) {
        return events.addAttendee(new Attendee(name, email, phone, contact));
    }

    private int logSize() {
        return communication.getLog().size();
    }

    /** Shows how many messages Communication sent since {@code before}, against how many were expected. */
    private void sent(int before, int expected) {
        show();
        int count = logSize() - before;
        System.out.println("  CommunicationService sent " + count + " message(s); expected " + expected
                + (count == expected ? " - no duplicates." : " - MISMATCH!"));
    }

    /** Registers with the demo's fixed "now", so an event that has started would be refused. */
    private void register(Event event, Attendee attendee, TicketAddOn... addOns) {
        RegistrationResult result = events.register(event.getId(), attendee, NOW, addOns);
        show();
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

    /** The demo is the view: prints the messages sent and the inbox notes received since the last call. */
    private void show() {
        communication.takeOutput().forEach(System.out::println);
        for (Attendee attendee : events.listAttendees()) {
            for (String note : attendee.takeNewNotes()) {
                System.out.println("    [inbox of " + attendee.getName() + "] " + note);
            }
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
