package org.nonprofit.donation;

import java.time.LocalDate;

/** FACTORY METHOD (concrete creator): builds InKindDonation objects (goods, not money). */
public class InKindDonationCreator extends DonationCreator {

    @Override
    protected Donation createDonation(String id, Donor donor, LocalDate date, DonationForm form) {
        return new InKindDonation(id, donor, form.getAmount(), date, form.getItem());
    }

    @Override
    public DonationType getType() {
        return DonationType.IN_KIND;
    }
}
