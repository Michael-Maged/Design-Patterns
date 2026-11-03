package org.nonprofit.app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.donation.DonationDetails;
import org.nonprofit.donation.Donor;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.volunteer.Task;

/**
 * Fills the system with a few records per module, through the
 * {@link NonProfitFacade}, so the interactive menu has something to show.
 * Dates are relative to today so reminders and tax years make sense.
 */
public final class SampleData {

    private SampleData() {
    }

    public static void load(NonProfitFacade facade) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        // ---- Donations ----
        Donor hany = facade.registerDonor("Hany Mansour", "hany@example.org", "+20120000001", ContactPreference.EMAIL);
        Donor rania = facade.registerDonor("Rania Kamel", "rania@example.org", "+20120000002", ContactPreference.SMS);
        Donor bakery = facade.registerDonor("El Nour Bakery", "orders@elnour.example", "+20120000003",
                ContactPreference.EMAIL);
        facade.createCampaign("Winter Blankets", 15_000);
        facade.recordDonation(hany.getId(), "online", today.minusDays(20),
                DonationDetails.of(500).reference("PAY-88213").recurring());
        facade.recordDonation(rania.getId(), "check", today.minusDays(10),
                DonationDetails.of(2_000).reference("004517").bank("National Bank of Egypt").campaign("Winter Blankets"));
        facade.recordDonation(bakery.getId(), "in-kind", today.minusDays(5),
                DonationDetails.of(1_200).item("300 loaves of bread for the food drive"));

        // ---- Volunteers (before beneficiaries, so urgent needs reach them) ----
        facade.registerVolunteer("Sara Mostafa", "sara@example.org", "+20110000001", ContactPreference.EMAIL,
                List.of("first aid", "teaching"), List.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), List.of("health"));
        facade.registerVolunteer("Karim Fathy", "karim@example.org", "+20110000002", ContactPreference.SMS,
                List.of("first aid", "driving"), List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), List.of("logistics"));
        facade.registerVolunteer("Nour Adel", "nour@example.org", "+20110000003", ContactPreference.SMS,
                List.of("cooking"), List.of(DayOfWeek.SATURDAY), List.of("food"));
        Task delivery = facade.addTask("Delivery run", "driving", DayOfWeek.SUNDAY, "logistics",
                "Neighbourhood Food Drive");
        facade.assignTask(delivery.getId());
        facade.logHours(delivery.getId(), 6, today.minusDays(7));
        facade.addTask("Medical tent", "first aid", DayOfWeek.SATURDAY, "health", "Neighbourhood Food Drive");
        facade.addTask("Kitchen helper", "cooking", DayOfWeek.SATURDAY, "food", "Neighbourhood Food Drive");

        // ---- Events ----
        Event gala = facade.createEvent("fundraiser", "Winter Charity Gala", now.plusDays(10).withHour(19),
                "Grand Hall", 100);
        Event firstAid = facade.createEvent("workshop", "First Aid Basics", now.plusHours(20), "Room 4B", 2);
        facade.createEvent("outreach program", "Neighbourhood Food Drive", now.plusDays(3), "Community Center", 50);
        Attendee mona = facade.addAttendee("Mona Hassan", "mona@example.org", "+20100000001", ContactPreference.EMAIL);
        Attendee omar = facade.addAttendee("Omar Khaled", "omar@example.org", "+20100000002", ContactPreference.SMS);
        Attendee laila = facade.addAttendee("Laila Samir", "laila@example.org", "+20100000003", ContactPreference.EMAIL);
        facade.registerAttendee(gala.getId(), mona.getId(), List.of());
        facade.registerAttendee(gala.getId(), omar.getId(), List.of("vip"));
        facade.registerAttendee(firstAid.getId(), laila.getId(), List.of("meal"));

        // ---- Beneficiaries ----
        String amal = facade.registerBeneficiary("Amal Saeed", null, "+20120000011", ContactPreference.SMS,
                "Ard El Lewa", 5).getId();
        String hassan = facade.registerBeneficiary("Hassan Ali", "hassan@example.org", "+20120000012",
                ContactPreference.EMAIL, "Imbaba", 2).getId();
        String raniaY = facade.registerBeneficiary("Rania Youssef", null, "+20120000013", ContactPreference.SMS,
                "Ard El Lewa", 4).getId();
        facade.addNeed(amal, "food parcel", 4, "medium", today.minusDays(9));
        facade.addNeed(hassan, "food parcel", 3, "low", today.minusDays(12));
        facade.addNeed(raniaY, "food parcel", 5, "high", today.minusDays(2));
        facade.addNeed(amal, "counselling session", 6, "low", today.minusDays(5));
        facade.distribute("Winter Food Program", "food parcel", 6, today.minusDays(1));
    }
}
