package org.nonprofit.donation;

import java.time.LocalDate;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;
import org.nonprofit.core.SystemObserver;

/**
 * Stand-alone walkthrough of the Donations module, showing each pattern.
 * Run: java -cp bin org.nonprofit.donation.DonationDemo
 */
public class DonationDemo {

    public static void main(String[] args) {
        // Until the Communication module is merged, a simple subscriber
        // stands in for it and prints the thank-you it would send.
        EventBus bus = EventBus.getInstance();
        bus.subscribe(SystemEventType.DONATION_RECEIVED, new SystemObserver() {
            @Override
            public void onEvent(SystemEvent event) {
                System.out.println("    [EventBus] " + event);
            }
        });

        DonationService service = new DonationService();
        LocalDate today = LocalDate.of(2026, 11, 1);

        Donor hany = service.registerDonor(new Donor("Hany Mansour", "hany@example.org", "+20120000001"));
        Donor rania = service.registerDonor(
                new Donor("Rania Kamel", "rania@example.org", "+20120000002", ContactPreference.SMS));
        Donor bakery = service.registerDonor(new Donor("El Nour Bakery", "orders@elnour.example", "+20120000003"));
        service.createCampaign("Winter Blankets", 15_000);

        heading("1. FACTORY METHOD - each creator decides which donation to build");
        for (DonationType type : DonationType.values()) {
            System.out.println("  " + type + " is handled by " + service.processorFor(type).getClass().getSimpleName());
        }
        Donation online = service.recordDonation(hany.getId(), DonationType.ONLINE, today.minusMonths(9),
                DonationDetails.of(500).reference("PAY-88213").recurring());
        Donation check = service.recordDonation(rania.getId(), "check", today.minusMonths(2),
                DonationDetails.of(2_000).reference("004517").bank("National Bank of Egypt").campaign("Winter Blankets"));
        Donation goods = service.recordDonation(bakery.getId(), "in-kind", today.minusDays(5),
                DonationDetails.of(1_200).item("300 loaves of bread for the food drive"));
        for (Donation d : service.listDonations()) {
            System.out.println("  " + d.getClass().getSimpleName() + ": " + d.summary());
        }

        heading("2. Invalid donations are rejected");
        tryRecord(service, hany.getId(), "check", DonationDetails.of(100));
        tryRecord(service, hany.getId(), "online", DonationDetails.of(-50).reference("PAY-1"));
        tryRecord(service, hany.getId(), "crypto", DonationDetails.of(10));
        tryRecord(service, hany.getId(), "online", DonationDetails.of(10).reference("PAY-2").campaign("Summer Camp"));

        heading("3. Campaign goal and progress");
        service.recordDonation(hany.getId(), DonationType.ONLINE, today.minusDays(1),
                DonationDetails.of(4_500).reference("PAY-90410").campaign("winter blankets"));
        for (Campaign c : service.listCampaigns()) {
            System.out.println("  " + c.summary());
        }

        heading("4. TEMPLATE METHOD - receipts and a tax statement, one layout");
        System.out.println(service.receipt(online.getId(), today));
        System.out.println(service.receipt(goods.getId(), today));
        System.out.println(service.taxStatement(hany.getId(), 2026, today));
        try {
            service.taxStatement(rania.getId(), 2025, today);
        } catch (IllegalStateException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        heading("5. Totals and top donors");
        System.out.println(service.summaryReport());
        for (Donor d : service.topDonors(3)) {
            System.out.println("  " + d.summary());
        }

        heading("6. Donor records are kept while they have donations");
        try {
            service.removeDonor(check.getDonor().getId());
        } catch (IllegalStateException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        heading("7. SINGLETON - one EventBus for the whole system");
        System.out.println("  Same instance? " + (EventBus.getInstance() == bus));
    }

    private static void tryRecord(DonationService service, String donorId, String type, DonationDetails details) {
        try {
            service.recordDonation(donorId, type, LocalDate.of(2026, 11, 1), details);
            System.out.println("  Accepted (unexpected)");
        } catch (IllegalArgumentException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
