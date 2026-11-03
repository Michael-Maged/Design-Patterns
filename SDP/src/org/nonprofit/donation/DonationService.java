package org.nonprofit.donation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemEventType;

/**
 * Entry point of the Donations module (the Facade in core calls this).
 * Keeps donors, donations and campaigns, hands each new donation to the
 * {@link DonationProcessor} for its method, publishes DONATION_RECEIVED on
 * the EventBus and produces receipts and tax statements.
 *
 * It depends only on the abstract creator and the abstract {@link Donation},
 * never on a concrete donation class (Dependency Inversion Principle).
 */
public class DonationService {
    private final Map<String, Donor> donors = new LinkedHashMap<>();
    private final Map<String, Donation> donations = new LinkedHashMap<>();
    private final Map<String, Campaign> campaigns = new LinkedHashMap<>();
    private final Map<DonationType, DonationProcessor> processors = new LinkedHashMap<>();
    private final EventBus bus = EventBus.getInstance();

    public DonationService() {
        processors.put(DonationType.ONLINE, new OnlineDonationProcessor());
        processors.put(DonationType.CHECK, new CheckDonationProcessor());
        processors.put(DonationType.IN_KIND, new InKindDonationProcessor());
    }

    /** Plugs in the creator for a donation method (replacing any earlier one for that method). */
    public void addProcessor(DonationProcessor processor) {
        processors.put(processor.getType(), processor);
    }

    public DonationProcessor processorFor(DonationType type) {
        DonationProcessor processor = processors.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("No processor for " + type + " donations");
        }
        return processor;
    }

    // ---- Donors ----

    public Donor registerDonor(Donor donor) {
        donors.put(donor.getId(), donor);
        return donor;
    }

    /** @return the donor, or null if there is none with that id */
    public Donor findDonor(String donorId) {
        return donors.get(donorId);
    }

    public List<Donor> listDonors() {
        return new ArrayList<>(donors.values());
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

    /** @return the campaign (names compared without regard to case), or null if there is none */
    public Campaign findCampaign(String name) {
        if (name == null) {
            return null;
        }
        return campaigns.get(key(name));
    }

    public List<Campaign> listCampaigns() {
        return new ArrayList<>(campaigns.values());
    }

    // ---- Recording donations ----

    /**
     * Has the creator for this method build and number the donation, then
     * adds it to its campaign and publishes DONATION_RECEIVED.
     */
    public Donation recordDonation(String donorId, DonationType type, LocalDate date, DonationDetails details) {
        Donor donor = requireDonor(donorId);
        Campaign campaign = null;
        if (details.campaign() != null) {
            campaign = findCampaign(details.campaign());
            if (campaign == null) {
                throw new IllegalArgumentException("No campaign named " + details.campaign());
            }
        }
        Donation donation = processorFor(type).receive(donor, date, details);
        donations.put(donation.getId(), donation);
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

    /** @return the donation, or null if there is none with that id */
    public Donation findDonation(String donationId) {
        return donations.get(donationId);
    }

    public List<Donation> listDonations() {
        return new ArrayList<>(donations.values());
    }

    // ---- Reports ----

    public double totalRaised() {
        double total = 0;
        for (Donation d : donations.values()) {
            total += d.getValue();
        }
        return total;
    }

    /** Total value per donation method, in the order ONLINE, CHECK, IN_KIND. */
    public Map<DonationType, Double> totalsByType() {
        Map<DonationType, Double> totals = new LinkedHashMap<>();
        for (DonationType type : DonationType.values()) {
            totals.put(type, 0.0);
        }
        for (Donation d : donations.values()) {
            totals.put(d.getType(), totals.get(d.getType()) + d.getValue());
        }
        return totals;
    }

    /** Donors ranked by total given, most first (equal totals keep their registration order). */
    public List<Donor> topDonors(int top) {
        List<Donor> ranked = new ArrayList<>();
        for (Donor donor : donors.values()) {
            // Insert before the first donor who gave less.
            int position = ranked.size();
            for (int i = 0; i < ranked.size(); i++) {
                if (ranked.get(i).getTotalGiven() < donor.getTotalGiven()) {
                    position = i;
                    break;
                }
            }
            ranked.add(position, donor);
        }
        return new ArrayList<>(ranked.subList(0, Math.min(top, ranked.size())));
    }

    public String summaryReport() {
        StringBuilder sb = new StringBuilder("Donation summary\n");
        Map<DonationType, Double> totals = totalsByType();
        for (DonationType type : totals.keySet()) {
            sb.append(String.format("  %-8s %12s%n", type, DonationDocument.money(totals.get(type))));
        }
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
