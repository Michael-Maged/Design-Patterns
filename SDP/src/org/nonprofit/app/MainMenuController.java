package org.nonprofit.app;

import org.nonprofit.beneficiary.BeneficiaryController;
import org.nonprofit.communication.CommunicationController;
import org.nonprofit.donation.DonationController;
import org.nonprofit.event.EventController;
import org.nonprofit.volunteer.VolunteerController;

/**
 * MVC pattern (controller): runs the main console menu. It takes the
 * user's choice from the {@link MainMenuView} and opens that module's own
 * MVC screen; each screen works on the shared service in the
 * {@link DataStore} Singleton and comes back here on "0. Back".
 * A choice that is not on the menu is shown as a {@code !!} message and the
 * menu comes back; nothing the user types stops the program.
 */
public class MainMenuController {
    private final MainMenuView view;
    private final DonationController donationScreen;
    private final VolunteerController volunteerScreen;
    private final EventController eventScreen;
    private final BeneficiaryController beneficiaryScreen;
    private final CommunicationController communicationScreen;

    public MainMenuController(MainMenuView view, DonationController donationScreen,
            VolunteerController volunteerScreen, EventController eventScreen,
            BeneficiaryController beneficiaryScreen, CommunicationController communicationScreen) {
        if (view == null || donationScreen == null || volunteerScreen == null || eventScreen == null
                || beneficiaryScreen == null || communicationScreen == null) {
            throw new IllegalArgumentException("The view and all five module screens are required");
        }
        this.view = view;
        this.donationScreen = donationScreen;
        this.volunteerScreen = volunteerScreen;
        this.eventScreen = eventScreen;
        this.beneficiaryScreen = beneficiaryScreen;
        this.communicationScreen = communicationScreen;
    }

    /** Shows the main menu again after every screen until the user chooses Exit. */
    public void run() {
        boolean running = true;
        while (running) {
            switch (view.showMainMenu()) {
                case 1: donationScreen.run(); break;
                case 2: volunteerScreen.run(); break;
                case 3: eventScreen.run(); break;
                case 4: beneficiaryScreen.run(); break;
                case 5: communicationScreen.run(); break;
                case 0: running = false; break;
                default: view.showError("Please choose a number from the menu.");
            }
        }
        view.showMessage("Goodbye.");
    }
}
