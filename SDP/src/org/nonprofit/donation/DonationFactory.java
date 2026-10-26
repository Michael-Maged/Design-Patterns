package org.nonprofit.donation;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FACTORY pattern: the only place that decides which concrete
 * {@link Donation} subclass to instantiate. Clients ask by
 * {@link DonationType} or by the raw text a user typed, and get back a
 * {@code Donation} without knowing the concrete class. Adding a new
 * donation method = one subclass + one case here.
 */
public final class DonationFactory {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private DonationFactory() {
    }

    /**
     * The number is only used up once the donation is valid, so receipt
     * numbers have no gaps when a bad donation is rejected.
     */
    public static synchronized Donation create(DonationType type, Donor donor, LocalDate date, DonationDetails details) {
        String id = "DNT-" + NEXT_ID.get();
        Donation donation = switch (type) {
            case ONLINE -> new OnlineDonation(id, donor, details.amount(), date, details.campaign(),
                    details.reference(), details.isRecurring());
            case CHECK -> new CheckDonation(id, donor, details.amount(), date, details.campaign(),
                    details.reference(), details.bank());
            case IN_KIND -> new InKindDonation(id, donor, details.amount(), date, details.campaign(),
                    details.item());
        };
        NEXT_ID.incrementAndGet();
        return donation;
    }

    /** Creates a donation from user input, e.g. {@code create("in-kind", ...)}. */
    public static Donation create(String typeInput, Donor donor, LocalDate date, DonationDetails details) {
        return create(DonationType.fromString(typeInput), donor, date, details);
    }
}
