package org.nonprofit.donation;

/**
 * Starts the interactive Donations screen (MVC).
 * Run from the SDP folder:  java -cp bin org.nonprofit.donation.DonationApp
 */
public class DonationApp {

    public static void main(String[] args) {
        DonationService model = new DonationService();
        DonationView view = new DonationView();
        DonationController controller = new DonationController(model, view);
        controller.run();
    }
}
