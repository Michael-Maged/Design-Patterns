package org.nonprofit.donation;

import java.time.LocalDate;

/** FACTORY METHOD (concrete creator): builds CheckDonation objects. */
public class CheckDonationCreator extends DonationCreator {

    @Override
    protected Donation createDonation(String id, Donor donor, LocalDate date, DonationForm form) {
        return new CheckDonation(id, donor, form.getAmount(), date, form.getReference(), form.getBank());
    }

    @Override
    public DonationType getType() {
        return DonationType.CHECK;
    }
}
