package org.nonprofit.donation;

import java.time.LocalDate;

/**
 * A donation of goods instead of money (food, clothes, blankets...).
 * The amount here is the estimated value that the donor gave us.
 */
public class InKindDonation extends Donation {
    private final String item;

    InKindDonation(String id, Donor donor, double estimatedValue, LocalDate date, String item) {
        super(id, donor, estimatedValue, date);
        if (item == null || item.isBlank()) {
            throw new IllegalArgumentException("In-kind donations need a description of the items");
        }
        this.item = item;
    }

    @Override
    public DonationType getType() {
        return DonationType.IN_KIND;
    }

    @Override
    public String getDetails() {
        return item + " (estimated value)";
    }

    public String getItem() { return item; }
}
