package org.nonprofit.donation;

import java.time.LocalDate;

/**
 * FACTORY METHOD pattern (GoF) - the CREATOR.
 *
 * receive() is the job every donation goes through, whatever its type:
 *   1. create the donation        <- this step is different for each type
 *   2. add it to the donor's history
 *
 * Step 1 is the "factory method" createDonation(). This class doesn't know
 * which donation to build, so it leaves that decision to its subclasses:
 *   OnlineDonationCreator  -> builds an OnlineDonation
 *   CheckDonationCreator   -> builds a CheckDonation
 *   InKindDonationCreator  -> builds an InKindDonation
 *
 * To support a new way of donating, we write one new Donation subclass and
 * one new creator subclass. This class never changes.
 */
public abstract class DonationCreator {

    /** The operation that uses the factory method. final, so every type follows the same steps. */
    public final Donation receive(String id, Donor donor, LocalDate date, DonationForm form) {
        Donation donation = createDonation(id, donor, date, form); // the subclass decides what this is
        donor.addDonation(donation);
        return donation;
    }

    /** The FACTORY METHOD: each subclass returns its own kind of Donation. */
    protected abstract Donation createDonation(String id, Donor donor, LocalDate date, DonationForm form);

    /** Which donation type this creator is for. */
    public abstract DonationType getType();
}
