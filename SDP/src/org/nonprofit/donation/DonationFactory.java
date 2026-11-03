package org.nonprofit.donation;

import java.time.LocalDate;

/**
 * FACTORY pattern.
 *
 * This is the only class that knows which Donation subclass to build for
 * each type. Everyone else just says "give me a CHECK donation with this form"
 * and gets back a Donation, without writing "new CheckDonation(...)" themselves.
 *
 * Why this helps: if the organization later accepts a new method (say, mobile
 * wallet), we add one new subclass and one new case here. DonationService,
 * the receipts and the screens do not change at all.
 */
public final class DonationFactory {

    // Nobody needs an object of this class, they just call DonationFactory.create(...).
    private DonationFactory() {
    }

    public static Donation create(DonationType type, String id, Donor donor, LocalDate date, DonationForm form) {
        if (type == null) {
            throw new IllegalArgumentException("Donation type is required");
        }
        switch (type) {
            case ONLINE:
                return new OnlineDonation(id, donor, form.getAmount(), date, form.getReference(), form.isMonthly());
            case CHECK:
                return new CheckDonation(id, donor, form.getAmount(), date, form.getReference(), form.getBank());
            case IN_KIND:
                return new InKindDonation(id, donor, form.getAmount(), date, form.getItem());
            default:
                throw new IllegalArgumentException("We don't support " + type + " donations yet");
        }
    }
}
