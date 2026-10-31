package org.nonprofit.donation;

import java.time.LocalDate;
import java.util.List;

/**
 * TEMPLATE METHOD pattern (concrete class): a yearly statement listing every
 * donation a donor made in one tax year, for their tax return.
 */
public class TaxDeductionStatement extends DonationDocument {
    private final Donor donor;
    private final int year;

    public TaxDeductionStatement(Donor donor, int year, LocalDate issueDate) {
        super(issueDate);
        this.donor = donor;
        this.year = year;
    }

    @Override
    protected String title() {
        return "Tax Deduction Statement " + year;
    }

    @Override
    protected String body() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Donor: %s (%s)%n%n", donor.getName(), donor.getId()));
        sb.append(String.format("%-10s  %-8s  %-8s  %12s%n", "Date", "ID", "Method", "Value"));
        for (Donation d : donations()) {
            sb.append(String.format("%-10s  %-8s  %-8s  %12s%n", d.getDate(), d.getId(), d.getType(), money(d.getValue())));
        }
        return sb.toString().stripTrailing();
    }

    @Override
    protected String totals() {
        double money = 0;
        double goods = 0;
        for (Donation d : donations()) {
            if (d.getType() == DonationType.IN_KIND) {
                goods += d.getValue();
            } else {
                money += d.getValue();
            }
        }
        return String.format("%-28s %12s%n%-28s %12s%n%-28s %12s",
                "Money donations:", money(money),
                "Goods (estimated value):", money(goods),
                "Total for " + year + ":", money(money + goods));
    }

    /** No statement for a year in which the donor gave nothing. */
    @Override
    protected boolean hasContent() {
        return !donations().isEmpty();
    }

    private List<Donation> donations() {
        return donor.donationsIn(year);
    }
}
