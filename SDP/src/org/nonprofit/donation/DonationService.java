package org.nonprofit.donation;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;

/**
 * Entry point of the Donations module (the Facade in core calls this).
 * Keeps donors, donations and campaigns, creates donations through the
 * {@link DonationFactory}, publishes DONATION_RECEIVED on the EventBus and
 * produces receipts and tax statements.
 */
public class DonationService {
    private final Map<String, Donor> donors = new LinkedHashMap<>();
    private final Map<String, Donation> donations = new LinkedHashMap<>();
    private final Map<String, Campaign> campaigns = new LinkedHashMap<>();
    private final EventBus bus = EventBus.getInstance();

    // ---- Donors ----

    public Donor registerDonor(Donor donor) {
        donors.put(donor.getId(), donor);
        return donor;
    }

    public Optional<Donor> findDonor(String donorId) {
        return Optional.ofNullable(donors.get(donorId));
    }

    public List<Donor> listDonors() {
        return List.copyOf(donors.values());
    }

    /** Donors with donations on record cannot be removed: receipts and tax statements depend on them. */
    public boolean removeDonor(String donorId) {
        Donor donor = requireDonor(donorId);
        if (!donor.getDonations().isEmpty()) {
            throw new IllegalStateException(donor.getName() + " has donations on record and cannot be removed");
        }
        return donors.remove(donorId) != null;
    }

    // ---- Campaigns ----

    public Campaign createCampaign(String name, double goal) {
        String key = key(name);
        if (campaigns.containsKey(key)) {
            throw new IllegalArgumentException("Campaign already exists: " + name);
        }
        Campaign campaign = new Campaign(name, goal);
        campaigns.put(key, campaign);
        return campaign;
    }

    public Optional<Campaign> findCampaign(String name) {
        return Optional.ofNullable(name == null ? null : campaigns.get(key(name)));
    }

    public List<Campaign> listCampaigns() {
        return List.copyOf(campaigns.values());
    }

    // ---- Recording donations ----

    /**
     * Creates the donation through the factory, adds it to the donor's
     * history and its campaign, and publishes DONATION_RECEIVED.
     */
    public Donation recordDonation(String donorId, DonationType type, LocalDate date, DonationDetails details) {
        Donor donor = requireDonor(donorId);
        Campaign campaign = null;
        if (details.campaign() != null) {
            campaign = findCampaign(details.campaign())
                    .orElseThrow(() -> new IllegalArgumentException("No campaign named " + details.campaign()));
        }
        Donation donation = DonationFactory.create(type, donor, date, details);
        donations.put(donation.getId(), donation);
        donor.addDonation(donation);
        if (campaign != null) {
            campaign.addDonation(donation);
        }
        bus.publish(new SystemEvent(SystemEventType.DONATION_RECEIVED,
                String.format("Thank you for your %s donation of %s", type, DonationDocument.money(donation.getValue())),
                donor, donation));
        return donation;
    }

    /** Records a donation from user input, e.g. {@code recordDonation(id, "check", ...)}. */
    public Donation recordDonation(String donorId, String typeInput, LocalDate date, DonationDetails details) {
        return recordDonation(donorId, DonationType.fromString(typeInput), date, details);
    }

    public Optional<Donation> findDonation(String donationId) {
        return Optional.ofNullable(donations.get(donationId));
    }

    public List<Donation> listDonations() {
        return List.copyOf(donations.values());
    }

    // ---- Reports ----

    public double totalRaised() {
        return donations.values().stream().mapToDouble(Donation::getValue).sum();
    }

    public Map<DonationType, Double> totalsByType() {
        Map<DonationType, Double> totals = new EnumMap<>(DonationType.class);
        for (DonationType type : DonationType.values()) {
            totals.put(type, 0.0);
        }
        donations.values().forEach(d -> totals.merge(d.getType(), d.getValue(), Double::sum));
        return totals;
    }

    /** Donors ranked by total given, most first. */
    public List<Donor> topDonors(int top) {
        return donors.values().stream()
                .sorted(Comparator.comparingDouble(Donor::getTotalGiven).reversed())
                .limit(top)
                .toList();
    }

    public String summaryReport() {
        StringBuilder sb = new StringBuilder("Donation summary\n");
        totalsByType().forEach((type, total) ->
                sb.append(String.format("  %-8s %12s%n", type, DonationDocument.money(total))));
        sb.append(String.format("  %-8s %12s", "TOTAL", DonationDocument.money(totalRaised())));
        return sb.toString();
    }

    // ---- Documents (Template Method) ----

    public String receipt(String donationId, LocalDate issueDate) {
        return new DonationReceipt(requireDonation(donationId), issueDate).generate();
    }

    public String taxStatement(String donorId, int year, LocalDate issueDate) {
        return new TaxDeductionStatement(requireDonor(donorId), year, issueDate).generate();
    }

    private Donor requireDonor(String donorId) {
        Donor donor = donors.get(donorId);
        if (donor == null) {
            throw new IllegalArgumentException("No donor with id " + donorId);
        }
        return donor;
    }

    private Donation requireDonation(String donationId) {
        Donation donation = donations.get(donationId);
        if (donation == null) {
            throw new IllegalArgumentException("No donation with id " + donationId);
        }
        return donation;
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
