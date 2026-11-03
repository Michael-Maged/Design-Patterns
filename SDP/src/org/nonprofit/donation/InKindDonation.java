package org.nonprofit.donation;

import java.time.LocalDate;

/** A donation of goods or services, recorded at an estimated value. */
public class InKindDonation extends Donation {
    private final String itemDescription;

    InKindDonation(String id, Donor donor, double estimatedValue, LocalDate date, String campaign,
                   String itemDescription) {
        super(id, donor, estimatedValue, date, campaign);
        if (itemDescription == null || itemDescription.isBlank()) {
            throw new IllegalArgumentException("In-kind donations need an item description");
        }
        this.itemDescription = itemDescription;
    }

    @Override
    public DonationType getType() {
        return DonationType.IN_KIND;
    }

    public String getItemDescription() { return itemDescription; }

    @Override
    public String details() {
        return itemDescription + " (estimated value)";
    }
}
