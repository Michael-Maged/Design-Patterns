package org.nonprofit.donation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;

/**
 * The main class of the Donations module. Other parts of the system
 * (the console screen, and later the Facade) only talk to this class.
 *
 * It keeps the donors, donations and campaigns, uses DonationFactory to
 * build new donations, and makes receipts and tax statements.
 *
 * OBSERVER pattern (subject side): every new donation is published on the
 * shared EventBus as DONATION_RECEIVED. This class has no idea who is
 * listening. The Communication module listens and sends the thank-you
 * message, but we never call it directly.
 */
public class DonationService {
    // LinkedHashMap keeps things in the order they were added, which looks nicer in lists.
    private final Map<String, Donor> donors = new LinkedHashMap<>();
    private final Map<String, Donation> donations = new LinkedHashMap<>();
    private final Map<String, Campaign> campaigns = new LinkedHashMap<>(); // key = name in lower case

    private int donorCount = 0;
    private int donationCount = 0;

    // ======================= Donors =======================

    public Donor addDonor(String name, String email, String phone, ContactPreference preferredContact) {
        if (preferredContact == null) {
            preferredContact = ContactPreference.EMAIL; // we thank donors by e-mail unless they ask otherwise
        }
        Donor donor = new Donor("DNR-" + (donorCount + 1), name, email, phone, preferredContact);
        donorCount++; // only counted after the donor was created successfully
        donors.put(donor.getId(), donor);
        return donor;
    }

    /** @return the donor, or null if the id doesn't exist */
    public Donor findDonor(String donorId) {
        return donors.get(donorId);
    }

    public List<Donor> getDonors() {
        return new ArrayList<>(donors.values());
    }

    /** A donor who already gave can't be removed, because their receipts point to them. */
    public void removeDonor(String donorId) {
        Donor donor = getDonorOrFail(donorId);
        if (!donor.getDonations().isEmpty()) {
            throw new IllegalStateException(donor.getName() + " has donations on record and can't be removed");
        }
        donors.remove(donorId);
    }

    // ======================= Campaigns =======================

    public Campaign addCampaign(String name, double goal) {
        Campaign campaign = new Campaign(name, goal);
        String key = campaign.getName().toLowerCase(Locale.ROOT);
        if (campaigns.containsKey(key)) {
            throw new IllegalArgumentException("There is already a campaign called " + campaign.getName());
        }
        campaigns.put(key, campaign);
        return campaign;
    }

    /** Case doesn't matter: "winter blankets" finds "Winter Blankets". Returns null if not found. */
    public Campaign findCampaign(String name) {
        if (name == null) {
            return null;
        }
        return campaigns.get(name.trim().toLowerCase(Locale.ROOT));
    }

    public List<Campaign> getCampaigns() {
        return new ArrayList<>(campaigns.values());
    }

    // ======================= Donations =======================

    /**
     * Records a new donation. Steps:
     *  1. check the donor (and the campaign, if one was given) exist
     *  2. ask the factory to build the right kind of donation
     *  3. save it, add it to the donor's history and to the campaign
     *  4. publish DONATION_RECEIVED so whoever is listening can react
     */
    public Donation donate(String donorId, DonationType type, LocalDate date, DonationForm form) {
        Donor donor = getDonorOrFail(donorId);

        Campaign campaign = null;
        if (form.getCampaign() != null && !form.getCampaign().isBlank()) {
            campaign = findCampaign(form.getCampaign());
            if (campaign == null) {
                throw new IllegalArgumentException("No campaign called " + form.getCampaign());
            }
        }

        // FACTORY: we don't care which subclass comes back, it's just "a Donation".
        // If the form is wrong the factory throws and the number is not used up,
        // so the receipt numbers never have gaps.
        Donation donation = DonationFactory.create(type, "DON-" + (donationCount + 1), donor, date, form);
        donationCount++;

        donations.put(donation.getId(), donation);
        donor.addDonation(donation);
        if (campaign != null) {
            donation.setCampaign(campaign);
            campaign.addAmount(donation.getAmount());
        }

        // OBSERVER: tell the rest of the system. Communication will thank the donor.
        String message = "Thank you for your " + type.name().toLowerCase(Locale.ROOT).replace('_', '-')
                + " donation of " + Donation.money(donation.getAmount()) + "!";
        EventBus.getInstance().publish(new SystemEvent(SystemEventType.DONATION_RECEIVED, message, donor, donation));

        return donation;
    }

    /** Same as above, but the type comes as text the user typed, e.g. "check" or "in-kind". */
    public Donation donate(String donorId, String typeText, LocalDate date, DonationForm form) {
        return donate(donorId, DonationType.fromText(typeText), date, form);
    }

    /** @return the donation, or null if the id doesn't exist */
    public Donation findDonation(String donationId) {
        return donations.get(donationId);
    }

    public List<Donation> getDonations() {
        return new ArrayList<>(donations.values());
    }

    // ======================= Reports =======================

    public double getTotalRaised() {
        double total = 0;
        for (Donation donation : donations.values()) {
            total += donation.getAmount();
        }
        return total;
    }

    public double getTotalFor(DonationType type) {
        double total = 0;
        for (Donation donation : donations.values()) {
            if (donation.getType() == type) {
                total += donation.getAmount();
            }
        }
        return total;
    }

    /** The donors who gave the most, biggest first. */
    public List<Donor> getTopDonors(int howMany) {
        List<Donor> list = new ArrayList<>(donors.values());
        list.sort(Comparator.comparingDouble(Donor::getTotalGiven).reversed());
        if (list.size() > howMany) {
            return new ArrayList<>(list.subList(0, howMany));
        }
        return list;
    }

    public String summaryReport() {
        StringBuilder text = new StringBuilder("Donation summary\n");
        for (DonationType type : DonationType.values()) {
            text.append(String.format("  %-8s %12s%n", type, Donation.money(getTotalFor(type))));
        }
        text.append(String.format("  %-8s %12s", "TOTAL", Donation.money(getTotalRaised())));
        return text.toString();
    }

    // ======================= Documents (Template Method) =======================

    public String makeReceipt(String donationId, LocalDate issueDate) {
        Donation donation = donations.get(donationId);
        if (donation == null) {
            throw new IllegalArgumentException("No donation with id " + donationId);
        }
        return new DonationReceipt(donation, issueDate).generate();
    }

    public String makeTaxStatement(String donorId, int year, LocalDate issueDate) {
        return new TaxStatement(getDonorOrFail(donorId), year, issueDate).generate();
    }

    // ======================= Helpers =======================

    private Donor getDonorOrFail(String donorId) {
        Donor donor = donors.get(donorId);
        if (donor == null) {
            throw new IllegalArgumentException("No donor with id " + donorId);
        }
        return donor;
    }
}
