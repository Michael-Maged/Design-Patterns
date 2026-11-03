package org.nonprofit.donation.factorymethod;

import java.time.LocalDate;
import org.nonprofit.donation.model.DonationType;
import org.nonprofit.donation.model.Donor;

/** Money paid with a paper check. We keep the check number and the bank for our records. */
public class CheckDonation extends Donation {
    private final String checkNumber;
    private final String bankName;

    CheckDonation(String id, Donor donor, double amount, LocalDate date,
                  String checkNumber, String bankName) {
        super(id, donor, amount, date);
        if (checkNumber == null || checkNumber.isBlank()) {
            throw new IllegalArgumentException("Check donations need a check number");
        }
        this.checkNumber = checkNumber;
        // The bank is nice to have but not required.
        if (bankName == null || bankName.isBlank()) {
            this.bankName = "unknown bank";
        } else {
            this.bankName = bankName;
        }
    }

    @Override
    public DonationType getType() {
        return DonationType.CHECK;
    }

    @Override
    public String getDetails() {
        return "check #" + checkNumber + " from " + bankName;
    }

    public String getCheckNumber() { return checkNumber; }
    public String getBankName() { return bankName; }
}
