import java.io.PrintStream;
import java.util.Scanner;

import org.nonprofit.app.DataStore;
import org.nonprofit.app.MainMenuController;
import org.nonprofit.app.MainMenuView;
import org.nonprofit.app.SampleData;
import org.nonprofit.app.SystemDemo;
import org.nonprofit.donation.DonationController;
import org.nonprofit.donation.DonationView;
import org.nonprofit.event.Attendee;
import org.nonprofit.volunteer.VolunteerController;
import org.nonprofit.volunteer.VolunteerView;

/**
 * Starts the non-profit management system.
 * <ul>
 *   <li>{@code java -cp bin App} loads sample data and opens the console menu
 *       (MVC: MainMenuView and MainMenuController over the module services in the DataStore Singleton).</li>
 *   <li>{@code java -cp bin App --demo} runs the scripted walkthrough of every module and pattern.</li>
 * </ul>
 */
public class App {

    public static void main(String[] args) {
        DataStore store = DataStore.getInstance();
        if (args.length > 0 && args[0].equals("--demo")) {
            SystemDemo.run(store);
            return;
        }

        // The sample data sends its own thank-yous and notices; keep them off the screen
        // by taking and dropping the messages it sent and the attendees' notes.
        PrintStream console = System.out;
        SampleData.load(store);
        store.getCommunicationService().takeOutput();
        for (Attendee attendee : store.listAttendees()) {
            attendee.takeNewNotes();
        }
        console.println(">> Sample data loaded: " + store.getDonationService().getDonors().size() + " donors, "
                + store.getVolunteerService().listVolunteers().size() + " volunteers, "
                + store.getEventService().listEvents().size() + " events, "
                + store.getBeneficiaryService().listBeneficiaries().size() + " beneficiaries.");

        // One Scanner for all screens, so none swallows another's input.
        // The module screens are made after the sample data, so their notifications start now.
        Scanner in = new Scanner(System.in);
        DonationController donationScreen = new DonationController(
                store.getDonationService(), new DonationView(in), true,
                store.getCommunicationService()::takeOutput);
        VolunteerController volunteerScreen = new VolunteerController(
                store.getVolunteerService(), new VolunteerView(in, console), true,
                store.getCommunicationService()::takeOutput);
        new MainMenuController(store, new MainMenuView(in, console), donationScreen, volunteerScreen).run();
    }
}
