package org.nonprofit.donation;

import java.time.LocalDate;

/**
 * FACTORY METHOD pattern (creator): {@link #receive} is the workflow every
 * donation goes through, the same way {@code PizzaStore.orderPizza()} is in
 * the textbook. The one step that differs - which concrete {@link Donation}
 * to build - is the factory method {@link #createDonation}, which each
 * subclass implements. Adding a new donation method = one new product class
 * + one new creator subclass; this class does not change.
 */
public abstract class DonationProcessor {
    /** Shared by all creators so donation numbers form one sequence. */
    private static int nextId = 1;

    /**
     * Builds the donation, gives it the next number and adds it to the
     * donor's history. Final so every method follows the same steps.
     */
    public final Donation receive(Donor donor, LocalDate date, DonationDetails details) {
        Donation donation = createDonation("DNT-" + nextId, donor, date, details);
        // The number is only used up once the donation is valid,
        // so receipt numbers have no gaps when a bad donation is rejected.
        nextId++;
        donor.addDonation(donation);
        return donation;
    }

    /** The factory method: subclasses decide which donation class to instantiate. */
    protected abstract Donation createDonation(String id, Donor donor, LocalDate date, DonationDetails details);

    /** The donation method this creator handles. */
    public abstract DonationType getType();
}
