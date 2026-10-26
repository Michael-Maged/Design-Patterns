package org.nonprofit.donation;

import java.time.LocalDate;

/** A donation paid by check. */
public class CheckDonation extends Donation {
    private final String checkNumber;
    private final String bankName;

    CheckDonation(String id, Donor donor, double amount, LocalDate date, String campaign,
                  String checkNumber, String bankName) {
        super(id, donor, amount, date, campaign);
        if (checkNumber == null || checkNumber.isBlank()) {
            throw new IllegalArgumentException("Check donations need a check number");
        }
        this.checkNumber = checkNumber;
        this.bankName = bankName == null || bankName.isBlank() ? "unknown bank" : bankName;
    }

    @Override
    public DonationType getType() {
        return DonationType.CHECK;
    }

    public String getCheckNumber() { return checkNumber; }
    public String getBankName() { return bankName; }

    @Override
    public String details() {
        return "check #" + checkNumber + ", " + bankName;
    }
}
