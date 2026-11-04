package org.nonprofit.donation;

import java.time.LocalDate;

import org.nonprofit.communication.model.CommunicationService;
import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.donation.model.Campaign;
import org.nonprofit.donation.factorymethod.Donation;
import org.nonprofit.donation.templatemethod.DonationDocument;
import org.nonprofit.donation.model.DonationForm;
import org.nonprofit.donation.model.DonationService;
import org.nonprofit.donation.model.DonationType;
import org.nonprofit.donation.model.Donor;

/**
 * A fixed walkthrough of the Donations module that shows each pattern.
 * Run from the SDP folder:  java -cp bin org.nonprofit.donation.DonationDemo
 */
public class DonationDemo {

    // Model classes never print, so the demo prints what Communication sent.
    private static CommunicationService comms;

    public static void main(String[] args) {
        // The Communication module listens on the EventBus. We create it here
        // only so we can SEE the thank-you messages it sends (Observer pattern).
        comms = new CommunicationService(DonationDocument.ORGANIZATION);

        DonationService service = new DonationService();
        LocalDate today = LocalDate.of(2026, 11, 1);

        Donor hany = service.addDonor("Hany Mansour", "hany@example.org", "+20120000001", ContactPreference.EMAIL);
        Donor rania = service.addDonor("Rania Kamel", "rania@example.org", "+20120000002", ContactPreference.SMS);
        Donor bakery = service.addDonor("El Nour Bakery", "orders@elnour.example", "+20120000003", null);
        service.addCampaign("Winter Blankets", 15_000);

        title("1. FACTORY METHOD - each creator decides which donation to build");
        for (DonationType type : DonationType.values()) {
            System.out.println("  " + type + " is handled by " + service.getCreator(type).getClass().getSimpleName());
        }
        System.out.println();

        DonationForm onlineForm = new DonationForm(500);
        onlineForm.setReference("PAY-88213");
        onlineForm.setMonthly(true);
        Donation online = service.donate(hany.getId(), DonationType.ONLINE, today.minusMonths(9), onlineForm);
        showMessages();

        DonationForm checkForm = new DonationForm(2_000);
        checkForm.setReference("004517");
        checkForm.setBank("National Bank of Egypt");
        checkForm.setCampaign("Winter Blankets");
        service.donate(rania.getId(), "check", today.minusMonths(2), checkForm);
        showMessages();

        DonationForm goodsForm = new DonationForm(1_200);
        goodsForm.setItem("300 loaves of bread for the food drive");
        Donation goods = service.donate(bakery.getId(), "in-kind", today.minusDays(5), goodsForm);
        showMessages();

        System.out.println();
        for (Donation d : service.getDonations()) {
            System.out.println("  " + d.getClass().getSimpleName() + " -> " + d);
        }

        title("2. Bad input is rejected");
        tryDonate(service, hany.getId(), "check", new DonationForm(100));        // no check number
        tryDonate(service, hany.getId(), "online", new DonationForm(-50));       // negative amount
        tryDonate(service, hany.getId(), "crypto", new DonationForm(10));        // unknown type
        DonationForm wrongCampaign = new DonationForm(10);
        wrongCampaign.setReference("PAY-2");
        wrongCampaign.setCampaign("Summer Camp");
        tryDonate(service, hany.getId(), "online", wrongCampaign);               // campaign doesn't exist

        title("3. Campaign goal and progress (client requirement)");
        DonationForm bigGift = new DonationForm(4_500);
        bigGift.setReference("PAY-90410");
        bigGift.setCampaign("winter blankets"); // case doesn't matter
        service.donate(hany.getId(), DonationType.ONLINE, today.minusDays(1), bigGift);
        showMessages();
        for (Campaign c : service.getCampaigns()) {
            System.out.println("  " + c);
        }

        title("4. TEMPLATE METHOD - same layout, different content");
        System.out.println(service.makeReceipt(online.getId(), today));
        System.out.println(service.makeReceipt(goods.getId(), today));
        System.out.println(service.makeTaxStatement(hany.getId(), 2026, today));
        try {
            service.makeTaxStatement(rania.getId(), 2025, today);
        } catch (IllegalStateException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        title("5. Totals and top donors");
        System.out.println(service.summaryReport());
        for (Donor d : service.getTopDonors(3)) {
            System.out.println("  " + d.summary());
        }

        title("6. A donor who already gave can't be deleted");
        try {
            service.removeDonor(rania.getId());
        } catch (IllegalStateException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
    }

    private static void tryDonate(DonationService service, String donorId, String type, DonationForm form) {
        try {
            service.donate(donorId, type, LocalDate.of(2026, 11, 1), form);
            System.out.println("  Accepted (this should not happen!)");
        } catch (IllegalArgumentException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
    }

    /** Prints the messages Communication sent since the last call. */
    private static void showMessages() {
        for (String line : comms.takeOutput()) {
            System.out.println(line);
        }
    }

    private static void title(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
