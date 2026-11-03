package org.nonprofit.donation;

/**
 * Everything the user fills in when making a donation, like a paper form.
 *
 * Not every field is used by every type:
 *   online  -> reference (the payment id) and monthly
 *   check   -> reference (the check number) and bank
 *   in-kind -> item
 * The campaign is optional for all types.
 *
 * Example:
 *   DonationForm form = new DonationForm(500);
 *   form.setReference("PAY-123");
 *   form.setMonthly(true);
 */
public class DonationForm {
    private final double amount;
    private String reference;
    private String bank;
    private String item;
    private boolean monthly;
    private String campaign;

    /** @param amount the money given, or the estimated value for goods */
    public DonationForm(double amount) {
        this.amount = amount;
    }

    public double getAmount() { return amount; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public boolean isMonthly() { return monthly; }
    public void setMonthly(boolean monthly) { this.monthly = monthly; }

    public String getCampaign() { return campaign; }
    public void setCampaign(String campaign) { this.campaign = campaign; }
}
