package org.nonprofit.donation;

import java.time.LocalDate;

/** TEMPLATE METHOD pattern (concrete class): a receipt for a single donation. */
public class DonationReceipt extends DonationDocument {
    private final Donation donation;

    public DonationReceipt(Donation donation, LocalDate issueDate) {
        super(issueDate);
        this.donation = donation;
    }

    @Override
    protected String title() {
        return "Donation Receipt " + donation.getId();
    }

    @Override
    protected String body() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Donor:     %s (%s)%n", donation.getDonor().getName(), donation.getDonor().getId()));
        sb.append(String.format("Date:      %s%n", donation.getDate()));
        sb.append(String.format("Method:    %s%n", donation.getType()));
        sb.append(String.format("Details:   %s", donation.details()));
        if (donation.getCampaign() != null) {
            sb.append(String.format("%nCampaign:  %s", donation.getCampaign()));
        }
        if (donation instanceof OnlineDonation online && online.isRecurringMonthly()) {
            sb.append(String.format("%nThis is a recurring monthly gift. Thank you for giving every month."));
        }
        return sb.toString();
    }

    @Override
    protected String totals() {
        String label = donation.getType() == DonationType.IN_KIND ? "Estimated value:" : "Amount received:";
        return String.format("%-20s %s", label, money(donation.getValue()));
    }

    /** In-kind gifts carry an extra note about how the value was set. */
    @Override
    protected String footer() {
        String note = donation.getType() == DonationType.IN_KIND
                ? "\nThe value of donated goods is the donor's own estimate." : "";
        return super.footer() + note;
    }
}
