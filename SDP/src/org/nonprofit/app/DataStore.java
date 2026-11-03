package org.nonprofit.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nonprofit.beneficiary.BeneficiaryService;
import org.nonprofit.communication.CommunicationService;
import org.nonprofit.core.Person;
import org.nonprofit.donation.DonationService;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.EventService;
import org.nonprofit.volunteer.VolunteerService;

/**
 * SINGLETON pattern: the one in-memory data store of the whole system.
 * It owns the single instance of every module service, so the main menu,
 * the Volunteers screen and the demo all work on the same donors,
 * volunteers, events and beneficiaries.
 * Each service keeps its own records; this class does not copy them.
 *
 * It also wires the modules together without them importing each other:
 * the volunteers are passed to the Beneficiaries module as the
 * {@link org.nonprofit.core.Audience} for urgent needs, and the
 * {@link CommunicationService} is created exactly once here, so every
 * EventBus message is sent once.
 *
 * It lives in {@code org.nonprofit.app}, not in {@code core}, because it
 * must import every module and {@code core} must not depend on any of them.
 */
public final class DataStore {
    /** Name used in message signatures (the same one the receipts and reports print). */
    public static final String ORGANISATION = "Community Hands Foundation";

    private static volatile DataStore instance;

    private final DonationService donationService = new DonationService();
    private final VolunteerService volunteerService = new VolunteerService();
    private final EventService eventService = new EventService();
    private final BeneficiaryService beneficiaryService = new BeneficiaryService();
    private final CommunicationService communicationService = new CommunicationService(ORGANISATION);

    /** EventService has no store for people, so the attendees the menu can pick are kept here. */
    private final Map<String, Attendee> attendees = new LinkedHashMap<>();

    private DataStore() {
        // URGENT_NEED goes to whoever volunteers at the moment it is raised.
        beneficiaryService.setUrgentResponders(() -> volunteerService.listVolunteers());
    }

    /** Lazily created, thread-safe (double-checked locking), like {@link org.nonprofit.core.EventBus}. */
    public static DataStore getInstance() {
        if (instance == null) {
            synchronized (DataStore.class) {
                if (instance == null) {
                    instance = new DataStore();
                }
            }
        }
        return instance;
    }

    // ---- Module services (one each) ----

    public DonationService getDonationService() { return donationService; }
    public VolunteerService getVolunteerService() { return volunteerService; }
    public EventService getEventService() { return eventService; }
    public BeneficiaryService getBeneficiaryService() { return beneficiaryService; }
    public CommunicationService getCommunicationService() { return communicationService; }

    // ---- Attendees ----

    public synchronized Attendee addAttendee(Attendee attendee) {
        if (attendee == null) {
            throw new IllegalArgumentException("Attendee is required");
        }
        attendees.put(attendee.getId(), attendee);
        return attendee;
    }

    /** @return the attendee, or null if there is none with that id */
    public synchronized Attendee findAttendee(String attendeeId) {
        return attendees.get(attendeeId);
    }

    /** A copy, so callers cannot change the registry. */
    public synchronized List<Attendee> listAttendees() {
        return new ArrayList<>(attendees.values());
    }

    // ---- Broadcast recipients ----

    /** Everyone currently in a group, for a manual broadcast. */
    public List<? extends Person> membersOf(BroadcastGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("Group is required");
        }
        switch (group) {
            case DONORS:
                return donationService.listDonors();
            case VOLUNTEERS:
                return volunteerService.listVolunteers();
            case BENEFICIARIES:
                return beneficiaryService.listBeneficiaries();
            default:
                return listAttendees();
        }
    }
}
