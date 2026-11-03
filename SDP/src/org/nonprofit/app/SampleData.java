package org.nonprofit.app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.nonprofit.beneficiary.Beneficiary;
import org.nonprofit.beneficiary.BeneficiaryService;
import org.nonprofit.beneficiary.Urgency;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.donation.DonationForm;
import org.nonprofit.donation.DonationService;
import org.nonprofit.donation.Donor;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.event.EventService;
import org.nonprofit.event.TicketAddOn;
import org.nonprofit.volunteer.Task;
import org.nonprofit.volunteer.Volunteer;
import org.nonprofit.volunteer.VolunteerService;

/**
 * Fills the system with a few records per module, through the services in
 * the {@link DataStore}, so the interactive menu has something to show.
 * Dates are relative to today so reminders and tax years make sense.
 */
public final class SampleData {

    private SampleData() {
    }

    public static void load(DataStore store) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        // ---- Donations ----
        DonationService donations = store.getDonationService();
        Donor hany = donations.addDonor("Hany Mansour", "hany@example.org", "+20120000001", ContactPreference.EMAIL);
        Donor rania = donations.addDonor("Rania Kamel", "rania@example.org", "+20120000002", ContactPreference.SMS);
        Donor bakery = donations.addDonor("El Nour Bakery", "orders@elnour.example", "+20120000003",
                ContactPreference.EMAIL);
        donations.addCampaign("Winter Blankets", 15_000);
        DonationForm online = new DonationForm(500);
        online.setReference("PAY-88213");
        online.setMonthly(true);
        donations.donate(hany.getId(), "online", today.minusDays(20), online);
        DonationForm check = new DonationForm(2_000);
        check.setReference("004517");
        check.setBank("National Bank of Egypt");
        check.setCampaign("Winter Blankets");
        donations.donate(rania.getId(), "check", today.minusDays(10), check);
        DonationForm goods = new DonationForm(1_200);
        goods.setItem("300 loaves of bread for the food drive");
        donations.donate(bakery.getId(), "in-kind", today.minusDays(5), goods);

        // ---- Volunteers (before beneficiaries, so urgent needs reach them) ----
        VolunteerService volunteers = store.getVolunteerService();
        volunteers.registerVolunteer(new Volunteer("Sara Mostafa", "sara@example.org", "+20110000001",
                ContactPreference.EMAIL).addSkill("first aid").addSkill("teaching")
                .addAvailability(DayOfWeek.MONDAY, DayOfWeek.SATURDAY).addPreference("health"));
        volunteers.registerVolunteer(new Volunteer("Karim Fathy", "karim@example.org", "+20110000002",
                ContactPreference.SMS).addSkill("first aid").addSkill("driving")
                .addAvailability(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).addPreference("logistics"));
        volunteers.registerVolunteer(new Volunteer("Nour Adel", "nour@example.org", "+20110000003",
                ContactPreference.SMS).addSkill("cooking")
                .addAvailability(DayOfWeek.SATURDAY).addPreference("food"));
        Task delivery = volunteers.addTask(new Task("Delivery run", "driving", DayOfWeek.SUNDAY, "logistics",
                "Neighbourhood Food Drive"));
        volunteers.assign(delivery.getId());
        volunteers.logHours(delivery.getId(), 6, today.minusDays(7));
        volunteers.addTask(new Task("Medical tent", "first aid", DayOfWeek.SATURDAY, "health", "Neighbourhood Food Drive"));
        volunteers.addTask(new Task("Kitchen helper", "cooking", DayOfWeek.SATURDAY, "food", "Neighbourhood Food Drive"));

        // ---- Events ----
        EventService events = store.getEventService();
        Event gala = events.createEvent("fundraiser", "Winter Charity Gala", now.plusDays(10).withHour(19),
                "Grand Hall", 100);
        Event firstAid = events.createEvent("workshop", "First Aid Basics", now.plusHours(20), "Room 4B", 2);
        events.createEvent("outreach program", "Neighbourhood Food Drive", now.plusDays(3), "Community Center", 50);
        Attendee mona = events.addAttendee(new Attendee("Mona Hassan", "mona@example.org", "+20100000001",
                ContactPreference.EMAIL));
        Attendee omar = events.addAttendee(new Attendee("Omar Khaled", "omar@example.org", "+20100000002",
                ContactPreference.SMS));
        Attendee laila = events.addAttendee(new Attendee("Laila Samir", "laila@example.org", "+20100000003",
                ContactPreference.EMAIL));
        events.register(gala.getId(), mona);
        events.register(gala.getId(), omar, TicketAddOn.VIP);
        events.register(firstAid.getId(), laila, TicketAddOn.MEAL);

        // ---- Beneficiaries ----
        BeneficiaryService beneficiaries = store.getBeneficiaryService();
        String amal = beneficiaries.registerBeneficiary(new Beneficiary("Amal Saeed", null, "+20120000011",
                ContactPreference.SMS, "Ard El Lewa", 5)).getId();
        String hassan = beneficiaries.registerBeneficiary(new Beneficiary("Hassan Ali", "hassan@example.org",
                "+20120000012", ContactPreference.EMAIL, "Imbaba", 2)).getId();
        String raniaY = beneficiaries.registerBeneficiary(new Beneficiary("Rania Youssef", null, "+20120000013",
                ContactPreference.SMS, "Ard El Lewa", 4)).getId();
        beneficiaries.addNeed(amal, "food parcel", 4, Urgency.MEDIUM, today.minusDays(9));
        beneficiaries.addNeed(hassan, "food parcel", 3, Urgency.LOW, today.minusDays(12));
        beneficiaries.addNeed(raniaY, "food parcel", 5, Urgency.HIGH, today.minusDays(2));
        beneficiaries.addNeed(amal, "counselling session", 6, Urgency.LOW, today.minusDays(5));
        beneficiaries.distribute("Winter Food Program", "food parcel", 6, today.minusDays(1));
    }
}
