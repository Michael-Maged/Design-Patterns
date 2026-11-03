package org.nonprofit.donation;

import java.time.LocalDate;

/** TEMPLATE METHOD (concrete class): a receipt for one single donation. */
public class DonationReceipt extends DonationDocument {
    private final Donation donation;

    public DonationReceipt(Donation donation, LocalDate issueDate) {
        super(issueDate);
        this.donation = donation;
    }

    @Override
    protected String getTitle() {
        return "Donation Receipt " + donation.getId();
    }

    @Override
    protected String body() {
        String text = "Donor:    " + donation.getDonor().getName() + " (" + donation.getDonor().getId() + ")\n"
                + "Date:     " + donation.getDate() + "\n"
                + "Method:   " + donation.getType() + "\n"
                + "Details:  " + donation.getDetails();
        if (donation.getCampaign() != null) {
            text += "\nCampaign: " + donation.getCampaign().getName();
        }
        // Client requirement: monthly online gifts get a special thank-you line.
        if (donation instanceof OnlineDonation && ((OnlineDonation) donation).isMonthly()) {
            text += "\nThis is a monthly gift. Thank you for giving every month!";
        }
        return text;
    }

    @Override
    protected String totals() {
        if (donation.getType() == DonationType.IN_KIND) {
            return "Estimated value:  " + Donation.money(donation.getAmount());
        }
        return "Amount received:  " + Donation.money(donation.getAmount());
    }

    /** We use the hook to add one extra line for goods. */
    @Override
    protected String footer() {
        String text = super.footer();
        if (donation.getType() == DonationType.IN_KIND) {
            text += "\nThe value of donated goods is the donor's own estimate.";
        }
        return text;
    }
}
