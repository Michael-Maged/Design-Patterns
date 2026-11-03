package org.nonprofit.app;

import java.util.List;

import org.nonprofit.beneficiary.BeneficiaryService;
import org.nonprofit.communication.CommunicationService;
import org.nonprofit.core.Audience;
import org.nonprofit.core.Person;
import org.nonprofit.donation.DonationService;
import org.nonprofit.event.EventService;
import org.nonprofit.volunteer.VolunteerService;

/**
 * SINGLETON pattern: the one in-memory data store of the whole system.
 * It owns the single instance of every module service, so the five module
 * screens and the demo all work on the same donors, volunteers, events,
 * attendees and beneficiaries.
 * Each service keeps its own records; this class does not copy them.
 *
 * It also wires the modules together without them importing each other:
 * the volunteers are passed to the Beneficiaries module as the
 * {@link org.nonprofit.core.Audience} for urgent needs, and the
 * {@link CommunicationService} is created exactly once here, so every
 * EventBus message is sent once. The Communication screen reaches people in
 * the other modules through {@link #membersOf}, {@link #findPerson} and
 * {@link #eventAudience}, which only hand out core types ({@link Person},
 * {@link Audience}).
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

    // ---- Broadcast recipients ----

    /** Everyone currently in a group, for a manual broadcast. */
    public List<? extends Person> membersOf(BroadcastGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("Group is required");
        }
        switch (group) {
            case DONORS:
                return donationService.getDonors();
            case VOLUNTEERS:
                return volunteerService.listVolunteers();
            case BENEFICIARIES:
                return beneficiaryService.listBeneficiaries();
            default:
                return eventService.listAttendees();
        }
    }

    /** The members of a group typed by the user, e.g. "donors" (see {@link BroadcastGroup#fromString}). */
    public List<? extends Person> membersOf(String groupName) {
        return membersOf(BroadcastGroup.fromString(groupName));
    }

    // ---- Looking people and events up for the Communication screen ----

    /**
     * Any person on file by ID: a donor (DNR-), volunteer (VOL-), attendee
     * (ATT-) or beneficiary (BEN-).
     *
     * @return the person, or null if nobody has that id
     */
    public Person findPerson(String personId) {
        Person person = donationService.findDonor(personId);
        if (person == null) {
            person = volunteerService.findVolunteer(personId);
        }
        if (person == null) {
            person = eventService.findAttendee(personId);
        }
        if (person == null) {
            person = beneficiaryService.findBeneficiary(personId);
        }
        return person;
    }

    /** @return the event with that id as an {@link Audience} (its registered attendees), or null if there is none */
    public Audience eventAudience(String eventId) {
        return eventService.findEvent(eventId).orElse(null);
    }
}
