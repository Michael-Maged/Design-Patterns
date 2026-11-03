package org.nonprofit.app;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nonprofit.beneficiary.AllocationMode;
import org.nonprofit.beneficiary.Beneficiary;
import org.nonprofit.beneficiary.BeneficiaryService;
import org.nonprofit.beneficiary.Distribution;
import org.nonprofit.beneficiary.Need;
import org.nonprofit.beneficiary.Urgency;
import org.nonprofit.communication.CommunicationService;
import org.nonprofit.communication.SentMessage;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;
import org.nonprofit.donation.Campaign;
import org.nonprofit.donation.Donation;
import org.nonprofit.donation.DonationDetails;
import org.nonprofit.donation.DonationService;
import org.nonprofit.donation.Donor;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.event.EventService;
import org.nonprofit.event.RegistrationResult;
import org.nonprofit.event.TicketAddOn;
import org.nonprofit.volunteer.AssignmentMode;
import org.nonprofit.volunteer.Task;
import org.nonprofit.volunteer.Volunteer;
import org.nonprofit.volunteer.VolunteerService;

/**
 * FACADE pattern: the single entry point the console menu and the demo call.
 * One simple method per main operation of every module. It hides the five
 * module services and how they fit together: where the one instance of each
 * lives ({@link DataStore}), looking people up by id, turning text such as
 * "check", "workshop", "preference" or "vip" into the right types, building
 * tickets with add-ons, and choosing who a group broadcast goes to.
 *
 * It only delegates; the rules stay in the modules.
 */
public class NonProfitFacade {
    private final DataStore store;
    private final DonationService donations;
    private final VolunteerService volunteers;
    private final EventService events;
    private final BeneficiaryService beneficiaries;
    private final CommunicationService communication;

    /** Works on the system-wide {@link DataStore}. */
    public NonProfitFacade() {
        this(DataStore.getInstance());
    }

    NonProfitFacade(DataStore store) {
        this.store = store;
        this.donations = store.getDonationService();
        this.volunteers = store.getVolunteerService();
        this.events = store.getEventService();
        this.beneficiaries = store.getBeneficiaryService();
        this.communication = store.getCommunicationService();
    }

    // ---- Donations ----

    public Donor registerDonor(String name, String email, String phone, ContactPreference contact) {
        return donations.registerDonor(new Donor(name, email, phone, requireContact(contact)));
    }

    public List<Donor> listDonors() {
        return donations.listDonors();
    }

    public Campaign createCampaign(String name, double goal) {
        return donations.createCampaign(name, goal);
    }

    /** @param type user input such as "online", "check" or "in-kind" (FACTORY METHOD picks the class) */
    public Donation recordDonation(String donorId, String type, LocalDate date, DonationDetails details) {
        if (details == null) {
            throw new IllegalArgumentException("Donation details are required");
        }
        return donations.recordDonation(donorId, type, date, details);
    }

    public List<Donation> listDonations() {
        return donations.listDonations();
    }

    public String receipt(String donationId, LocalDate issueDate) {
        return donations.receipt(donationId, issueDate);
    }

    public String taxStatement(String donorId, int year, LocalDate issueDate) {
        return donations.taxStatement(donorId, year, issueDate);
    }

    /** Totals by method, campaign progress and the top three donors. */
    public String donationSummary() {
        StringBuilder sb = new StringBuilder(donations.summaryReport());
        for (Campaign campaign : donations.listCampaigns()) {
            sb.append("\n  Campaign ").append(campaign.summary());
        }
        List<Donor> top = donations.topDonors(3);
        if (!top.isEmpty()) {
            sb.append("\n  Top donors:");
            for (Donor donor : top) {
                sb.append("\n    ").append(donor.summary());
            }
        }
        return sb.toString();
    }

    // ---- Volunteers ----

    public Volunteer registerVolunteer(String name, String email, String phone, ContactPreference contact,
            List<String> skills, List<DayOfWeek> days, List<String> categories) {
        // Build the whole profile first, so a bad value registers nobody.
        Volunteer volunteer = new Volunteer(name, email, phone, requireContact(contact));
        for (String skill : copyOf(skills)) {
            volunteer.addSkill(skill);
        }
        for (DayOfWeek day : copyOf(days)) {
            volunteer.addAvailability(day);
        }
        for (String category : copyOf(categories)) {
            volunteer.addPreference(category);
        }
        return volunteers.registerVolunteer(volunteer);
    }

    public List<Volunteer> listVolunteers() {
        return volunteers.listVolunteers();
    }

    /** @param eventName the event the task belongs to, or null/blank for none */
    public Task addTask(String name, String skill, DayOfWeek day, String category, String eventName) {
        String event = eventName == null || eventName.isBlank() ? null : eventName.trim();
        return volunteers.addTask(new Task(name, skill, day, category, event));
    }

    public List<Task> listTasks() {
        return volunteers.listTasks();
    }

    /** @param mode user input such as "skill", "availability" or "preference" @return the new rule's name */
    public String setAssignmentRule(String mode) {
        volunteers.setStrategy(AssignmentMode.fromString(mode));
        return volunteers.getStrategy().name();
    }

    /** Who the current rule would choose, without assigning. @return null if nobody fits */
    public Volunteer suggestVolunteer(String taskId) {
        return volunteers.suggest(taskId);
    }

    /** @return the volunteer the current rule chose, or null if nobody fits */
    public Volunteer assignTask(String taskId) {
        return volunteers.assign(taskId);
    }

    public void logHours(String taskId, double hours, LocalDate date) {
        volunteers.logHours(taskId, hours, date);
    }

    public String leaderboard(int top) {
        return volunteers.leaderboardReport(top);
    }

    public String hoursCertificate(String volunteerId, LocalDate date) {
        return volunteers.issueHoursCertificate(volunteerId, date);
    }

    public String participationCertificate(String volunteerId, String eventName, LocalDate date) {
        return volunteers.issueParticipationCertificate(volunteerId, eventName, date);
    }

    // ---- Events ----

    /** @param type user input such as "fundraiser", "workshop" or "outreach program" (FACTORY picks the class) */
    public Event createEvent(String type, String name, LocalDateTime dateTime, String location, int capacity) {
        if (dateTime == null) {
            throw new IllegalArgumentException("Event date and time are required");
        }
        requireText(location, "Location");
        return events.createEvent(type, name, dateTime, location.trim(), capacity);
    }

    public List<Event> listEvents() {
        return events.listEvents();
    }

    /** Adds a person who can then be registered for events. */
    public Attendee addAttendee(String name, String email, String phone, ContactPreference contact) {
        return store.addAttendee(new Attendee(name, email, phone, requireContact(contact)));
    }

    public List<Attendee> listAttendees() {
        return store.listAttendees();
    }

    /**
     * Registers a known attendee, with add-ons such as "vip", "meal" or
     * "parking" stacked on the ticket (DECORATOR). A full event puts them on
     * the waitlist instead.
     */
    public RegistrationResult registerAttendee(String eventId, String attendeeId, List<String> addOns) {
        Attendee attendee = requireAttendee(attendeeId);
        return events.register(eventId, attendee, parseAddOns(addOns));
    }

    /**
     * Adds a new person and registers them in one step. The event and the
     * add-ons are checked first, so a failed registration leaves no stray
     * attendee on file.
     */
    public RegistrationResult registerNewAttendee(String eventId, String name, String email, String phone,
            ContactPreference contact, List<String> addOns) {
        TicketAddOn[] chosen = parseAddOns(addOns);
        if (events.findEvent(eventId).isEmpty()) {
            throw new IllegalArgumentException("No event with id " + eventId);
        }
        Attendee attendee = addAttendee(name, email, phone, contact);
        return events.register(eventId, attendee, chosen);
    }

    private static TicketAddOn[] parseAddOns(List<String> addOns) {
        List<TicketAddOn> chosen = new ArrayList<>();
        for (String addOn : copyOf(addOns)) {
            if (!addOn.isBlank()) {
                chosen.add(TicketAddOn.fromString(addOn));
            }
        }
        return chosen.toArray(new TicketAddOn[0]);
    }

    /** @return false if the attendee was neither registered nor waitlisted */
    public boolean cancelRegistration(String eventId, String attendeeId) {
        return events.cancelRegistration(eventId, requireAttendee(attendeeId));
    }

    public void checkIn(String eventId, String attendeeId) {
        events.checkIn(eventId, requireAttendee(attendeeId));
    }

    public void rescheduleEvent(String eventId, LocalDateTime newDateTime) {
        if (newDateTime == null) {
            throw new IllegalArgumentException("New date and time are required");
        }
        events.reschedule(eventId, newDateTime);
    }

    public void cancelEvent(String eventId) {
        events.cancelEvent(eventId);
    }

    /** Reminds everyone registered for an event starting within {@code hours} of {@code now}. */
    public List<Event> sendUpcomingReminders(LocalDateTime now, int hours) {
        if (now == null) {
            throw new IllegalArgumentException("Current time is required");
        }
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be at least 1");
        }
        return events.sendUpcomingReminders(now, Duration.ofHours(hours));
    }

    public String attendanceReport(String eventId) {
        return events.attendanceReport(eventId);
    }

    // ---- Beneficiaries ----

    public Beneficiary registerBeneficiary(String name, String email, String phone, ContactPreference contact,
            String area, int householdSize) {
        return beneficiaries.registerBeneficiary(
                new Beneficiary(name, email, phone, requireContact(contact), area, householdSize));
    }

    public List<Beneficiary> listBeneficiaries() {
        return beneficiaries.listBeneficiaries();
    }

    /** @param urgency "low", "medium" or "high"; a HIGH need alerts every volunteer */
    public Need addNeed(String beneficiaryId, String category, int quantity, String urgency, LocalDate requestedOn) {
        return beneficiaries.addNeed(beneficiaryId, category, quantity, Urgency.fromString(urgency), requestedOn);
    }

    public List<Need> openNeeds() {
        return beneficiaries.openNeeds();
    }

    /** @param mode user input such as "urgency", "first come" or "equal" @return the new rule's name */
    public String setAllocationRule(String mode) {
        beneficiaries.setStrategy(AllocationMode.fromString(mode));
        return beneficiaries.getStrategy().name();
    }

    public String allocationRule() {
        return beneficiaries.getStrategy().name();
    }

    /** How the current rule would share the stock, without handing anything out. */
    public Map<Need, Integer> previewDistribution(String category, int available) {
        return beneficiaries.preview(category, available);
    }

    public List<Distribution> distribute(String program, String category, int available, LocalDate date) {
        return beneficiaries.distribute(program, category, available, date);
    }

    public List<String> listPrograms() {
        return beneficiaries.listPrograms();
    }

    public String programImpactReport(String program, LocalDate date) {
        return beneficiaries.programImpactReport(program, date);
    }

    public String progressReport(String beneficiaryId, LocalDate date) {
        return beneficiaries.progressReport(beneficiaryId, date);
    }

    // ---- Communication ----

    /**
     * Sends one message to everyone in a group.
     *
     * @param forced one channel for everybody, or null for each person's preference
     * @return how many people were reached
     */
    public int broadcast(BroadcastGroup group, String subject, String text, boolean urgent, ContactPreference forced) {
        if (group == null) {
            throw new IllegalArgumentException("Group is required");
        }
        requireText(subject, "Subject");
        requireText(text, "Message text");
        return communication.broadcast(membersOf(group), subject.trim(), text.trim(), urgent, forced);
    }

    /** @param group user input such as "donors" or "volunteers" */
    public int broadcast(String group, String subject, String text, boolean urgent, ContactPreference forced) {
        return broadcast(BroadcastGroup.fromString(group), subject, text, urgent, forced);
    }

    public List<SentMessage> messageLog() {
        return communication.getLog();
    }

    // ---- Helpers ----

    private List<? extends Person> membersOf(BroadcastGroup group) {
        switch (group) {
            case DONORS:
                return donations.listDonors();
            case VOLUNTEERS:
                return volunteers.listVolunteers();
            case BENEFICIARIES:
                return beneficiaries.listBeneficiaries();
            default:
                return store.listAttendees();
        }
    }

    private Attendee requireAttendee(String attendeeId) {
        Attendee attendee = store.findAttendee(attendeeId);
        if (attendee == null) {
            throw new IllegalArgumentException("No attendee with id " + attendeeId);
        }
        return attendee;
    }

    private static ContactPreference requireContact(ContactPreference contact) {
        if (contact == null) {
            throw new IllegalArgumentException("Contact preference is required");
        }
        return contact;
    }

    private static void requireText(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
    }

    /** A null list counts as empty; the copy protects against later changes by the caller. */
    private static <T> List<T> copyOf(List<T> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }
}
